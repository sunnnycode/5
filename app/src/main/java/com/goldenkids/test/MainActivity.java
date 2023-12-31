package com.goldenkids.test;

import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.Manifest;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener,View.OnClickListener {
    private GoogleMap mMap;
    private ApiService apiService;
    //    private Object BitmapDescriptorFactory;
    private String cctvUrl;
    private static final String CHANNEL_ID = "test"; // 원하는 채널 ID로 변경

    //setMarker에서 홍수난 쪽에 list.add(new Double[]{lang,lat}); 으로 추가
    private ArrayList<Double[]> list = new ArrayList<Double[]>();
    //String 배열 하나 더만들어서 location 그러니까 위치 이름 값 넣기 그걸 listview안에 삽입

    // CCTV api 호출
    private class FetchCctvDataTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapsInitializer.initialize(this);
        //bitmapDescriptorFactory = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(this);



        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://openapi.its.go.kr:9443/")
                .addConverterFactory(ScalarsConverterFactory.create())  // ScalarsConverterFactory를 사용하여 문자열로 변환
                .build();

        // ApiService 인스턴스 생성
        ApiService apiService = retrofit.create(ApiService.class);

        // API 호출
        Call<String> call = apiService.getCctvInfo("1b3009aee895424bbaea43c7d7f6a13d", "all", "2", "126.8845475", "127.8845475", "35.8504119", "36.8504119", "xml");

        // 비동기적으로 실행
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    String responseBody = response.body();
                    //System.out.println(responseBody);
                    ArrayList<Jung> jungList = Jung.parseJungData(responseBody);
                    for (Jung jung : jungList) {
                        //LatLng currentLatLng = new LatLng(jung.getCoordY(),jung.getCoordX());

                        setmaker(jung.getCoordY(), jung.getCoordX(), jung.getCctvName(), jung.getCctvUrl());

                    }
                    // 여기에서 문자열을 원하는 방식으로 처리할 수 있습니다.
                    // 예: 필요한 경우 문자열을 XML 파서를 사용하여 파싱
                } else {
                    // 에러 처리
                    System.out.println("에러: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // 실패 처리
                t.printStackTrace();
            }
        });
        showNotification();
    }
    @Override
    public void onClick(View v) {

    }
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        //위치 권한 요청

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//마지막 위치 받아오기
        Location loc_Current = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        // 마커 클릭 리스너 설정
        mMap.setOnMarkerClickListener(this);

        // 마커 추가
//        MarkerOptions markerOptions = new MarkerOptions();

        // API 호출 결과로 받은 좌표에 파란색 마커 추가
        LatLng apiLatLng = new LatLng(loc_Current.getLatitude(), loc_Current.getLongitude());

        setmaker(loc_Current.getLatitude(), loc_Current.getLongitude(), "내 위치", "");
    }

    public void setmaker(double lat, double lang, String location, String cctvUrl) {
        LatLng currentLatLng = new LatLng(lat, lang);


        MarkerOptions markerOptions = new MarkerOptions()
                .position(currentLatLng)
                .snippet(cctvUrl)
                .title(location);
        if (location.equals("[중부선] 오창") || location.equals("[경부선] 강서") || location.equals("[서천공주선] 장평2교") || location.equals("[호남지선] 서대전분기점2") || location.equals("[경부선] 남청주육교") || location.equals("[청주영덕선] 문의교")) {
            // MarkerOptions를 사용하여 파란색 마커 추가
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));

            mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));

        } else if (location.equals("내 위치")) {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));

            mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
        }
        mMap.addMarker(markerOptions);
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {

        //이미지를 보여주는 다이얼로그 메서드 호출
//        showImageDialog(brandTag);
        //임시,

        if (marker.getTitle().equals("[중부선] 오창")) {
            showImageDialog();
            //showNotification();

        } else {

            showVideoViewDialog(marker.getSnippet());
        }
        return true;
    }

    private void showNotification() {
        // 알림 빌더
        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("주의")
                .setContentText("침수")
                .setSmallIcon(R.drawable.a_2);

        // 알림 매니저
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // SDK 버전이 26 이상인지 확인
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 알림 채널 생성
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Test", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription("알림");
            notificationManager.createNotificationChannel(notificationChannel);
        }
        // 알림 ID
        int NOTIFICATION_ID = 0;
        // 알림 표시
        notificationManager.notify(NOTIFICATION_ID, notifyBuilder.build());
    }

    private void showImageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.image_dialog, null); // image_dialog_layout.xml 파일을 만들어야 합니다.

        // 예시: 브랜드 태그에 따라 이미지를 설정
        ImageView imageView = dialogView.findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.a_1);

        builder.setView(dialogView)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 확인 버튼을 눌렀을 때의 동작
                        dialog.dismiss(); // 다이얼로그 닫기
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showVideoViewDialog(String cctvurl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.media_dialog, null);
        VideoView video = dialogView.findViewById(R.id.videoView);
        video.setVideoURI(Uri.parse(cctvurl));

        builder.setView(dialogView)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 확인 버튼을 눌렀을 때의 동작
                        dialog.dismiss(); // 다이얼로그 닫기

                    }
                });
//                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        // 확인 버튼을 눌렀을 때의 동작
//                    }
//                });
        video.start();

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public class SampleData {
        private String area;
        private int distance;


        public SampleData(String area, int distance) {

            this.area = area;
            this.distance = distance;
        }

//
//        public String getMovieName()
//        {
//            return this.movieName;
//        }
//
//        public String getGrade()
//        {
//            return this.grade;
//        }
//    }




    }
}