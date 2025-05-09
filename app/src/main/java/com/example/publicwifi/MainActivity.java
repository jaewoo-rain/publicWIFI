package com.example.publicwifi;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.Projection;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {
    private Button btn1, btn2, btn3;
    private NaverMap naverMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final int MARKER_MIN_ZOOM = 11;
    private static final int MARKER_SIZE = 180;
    private LatLng lastLongPress; // 마커 추가하기위한 좌표

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();

        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);

        // 화면움직임 + 여러가지 화면 세팅 가능
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (naverMap == null) return;  // 아직 준비 안 됐으면 무시
                CameraPosition cameraPosition = new CameraPosition(
                        new LatLng(35.80883, 127.14799), // 대상 지점
                        16, // 줌 레벨
                        20, // 기울임 각도
                        180 // 베어링 각도
                );
                // 카메라만 부드럽게 이동
                naverMap.moveCamera(CameraUpdate.toCameraPosition(cameraPosition));
            }
        });

        // 다른 세팅은 그대로, 화면만 움직임 + 애니메이션 가능 (duration은 없어도됨)
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(35.8068378,127.1195137)).animate(CameraAnimation.Easing, 1000)
                        .finishCallback(() -> {
                            Toast.makeText(MainActivity.this, "카메라 이동 완료", Toast.LENGTH_SHORT).show();

                        })
                        .cancelCallback(()-> {
                            Toast.makeText(MainActivity.this, "카메라 이동 취소", Toast.LENGTH_SHORT).show();
                        });
                naverMap.moveCamera(cameraUpdate);
            }
        });


        // 1) FusedLocationSource 인스턴스 생성 (requestCode는 아무 값이나 정해두면 됩니다)
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);


        // 최초 한 번만 옵션을 적용해서 Fragment 생성
        if (savedInstanceState == null) {
            NaverMapOptions options = new NaverMapOptions()
                    .locationButtonEnabled(true)
                    .camera(new CameraPosition(
                            new LatLng(35.8570947, 127.1210231),
                            13))
                    .mapType(NaverMap.MapType.Terrain);

            MapFragment mapFragment = MapFragment.newInstance(options);
            fm.beginTransaction()
                    .add(R.id.map, mapFragment)
                    // .commitNow() 해도 좋습니다
                    .commit();
            mapFragment.getMapAsync(this);
        } else {
            // 이미 붙어 있는 Fragment를 꺼내서

            MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }


    }

    // 하나의 좌표
    LatLng southWest = new LatLng(31.43, 122.37);
    LatLng northEast = new LatLng(44.35, 132);
    LatLngBounds bounds1 = new LatLngBounds(southWest, northEast);

    // 여러 좌표 포함하는 MBR 만들기
    LatLngBounds bounds2 = new LatLngBounds.Builder()
            .include(new LatLng(35.80883, 127.14799))
            .include(new LatLng(35.8068378, 127.1195137))
            .include(new LatLng(35.7968626, 127.1143079))
            .include(new LatLng(35.7980662, 127.1403211))
            .include(new LatLng(35.812153, 127.1198025))
            .build();




    // 권한 설정
    private FusedLocationSource locationSource;
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        // 위치 권한 설정
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }


    @UiThread
    @Override
    // 맵이 완전히 준비된 후 할 일
    public void onMapReady(@NonNull NaverMap naverMap) {
        // 마커, 카메라 이동 등 지도 제어 로직

        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        // 기본 트래킹 모드 설정 (권한이 있으면 위치 표시, 없으면 추적 안 함)
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        UiSettings uiSettings = naverMap.getUiSettings();

        // 무수히 많이 일어남
        naverMap.addOnCameraChangeListener((reason, animated) -> {
            Log.i("NaverMap", "카메라 변경 - reson: " + reason + ", animated: " + animated);
//            Toast.makeText(MainActivity.this, "카메라 변경 - reson: " + reason + ", animated: " + animated, Toast.LENGTH_SHORT).show();

        });

        naverMap.addOnCameraIdleListener(() -> {
//            Toast.makeText(MainActivity.this, "카메라 움직임 종료", Toast.LENGTH_SHORT).show();
        });

        // 지도 최소, 최대 줌 레벨
        naverMap.setMinZoom(5.0);
        naverMap.setMaxZoom(18.0);

        // 현재 줌 레벨 확인
        naverMap.addOnCameraChangeListener((reason, animated) -> {
            double zoom = naverMap.getCameraPosition().zoom;
//            Log.d("ZoomLevel", "현재 줌 레벨: " + zoom);
//            Toast.makeText(MainActivity.this, "현재 줌 레벨: " + zoom, Toast.LENGTH_SHORT).show();
        });

        // 네이버 로고 클릭 비활성화
        uiSettings.setLogoClickEnabled(false);


        // 길게 클릭시 클릭 위치 좌표 띄우기 -> 나만의 마커 추가하기
        naverMap.setOnMapLongClickListener((pointF, latLng) -> {
            // 1) 저장해 둔 최근 long-press 위치
            lastLongPress = latLng;
            // 2) 추가 버튼 보이기
//            showAddButton();
        });





        // 특정한 심벌 클릭시 이벤트
        naverMap.setOnSymbolClickListener(symbol -> {
            if ("전북대학교 전주캠퍼스".equals(symbol.getCaption())) {
                Toast.makeText(this, "전북대 클릭", Toast.LENGTH_SHORT).show();
                // 이벤트 소비, OnMapClick 이벤트는 발생하지 않음
                return true;
            }
            // 이벤트 전파, OnMapClick 이벤트가 발생함
            return false;
        });

        // 마커 클릭 시 클릭 이벤트
        Marker marker = new Marker();
        marker.setPosition(new LatLng(35.80883, 127.14799));
        marker.setMap(naverMap);                    // 지도를 붙여 주고…
        marker.setOnClickListener(overlay -> {
            Toast.makeText(this, "마커 1 클릭", Toast.LENGTH_SHORT).show();
            return true;
        });

//        // 위치 변경에 따라서 좌표 토스트로 표시 -> 진명이형 하는거 이걸로 하면될듯
//        naverMap.addOnLocationChangeListener(location ->
//                Toast.makeText(this,
//                        location.getLatitude() + ", " + location.getLongitude() + " 이동",
//                        Toast.LENGTH_SHORT).show());

        // 나만의 마커 추가할때 사용할 예정 -> 필요없을지도?
//        Projection projection = naverMap.getProjection();
//        // 화면 → 지도
//        PointF screenPt = new PointF(100, 100);
//        LatLng coord = projection.fromScreenLocation(screenPt);
//        Log.d("MapCoord", coord.latitude + ", " + coord.longitude);
//
//        // 지도 → 화면
//        LatLng mapLoc = new LatLng(37.5666102, 126.9783881);
//        PointF screenLoc = projection.toScreenLocation(mapLoc);
//        Log.d("ScreenPt", "x=" + screenLoc.x + ", y=" + screenLoc.y);


        // 지도 유형
        // Basic: 일반 지도, Navi: 차량용 내비게이션에 특화된 지도, Satellite: 위성 지도, Terrain: 지형도
        // Hybrid: 위성 사진과 도로, 심벌을 함께 노출하는 하이브리드 지도, NaviHybrid: 위성 사진과 내비게이션용 도로, 심벌을 함께 노출하는 하이브리드 지도
        // None: 지도를 나타내지 않습니다. 단, 오버레이는 여전히 나타납니다.
        naverMap.setMapType(NaverMap.MapType.Basic);
        naverMap.setIndoorEnabled(true); // 실내 지도
        naverMap.setSymbolScale(1); // 심벌 크기 조절
//        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BUILDING, false); // 건물 그룹
//        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false); // 지적편집도 그룹
//        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRAFFIC, false); // 실시간 교통 정보
//        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, false); // 대중교통 그룹
//        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BICYCLE, false); // 자전거 그룹
//        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_MOUNTAIN, false); // 등산로 그룹
//        naverMap.setNightModeEnabled(true); // 야간모드 -> Navi 지도에서만 사용 가능
//        naverMap.setLightness(0.3f); // 지도 밝기 -1 ~ 1, 1일수록 밝음
//        naverMap.setBuildingHeight(0.5f); // 건물높이 : 0.5f => 50%, 지도가 기울어질때 입체적으로 표현
//        naverMap.setSymbolPerspectiveRatio(0); // 심벌 원근 효과, 지도를 기울일 경우 멀리있는것은 작게보임


        addMarker();
    }



    private void addMarker(){
        // 더미 데이터
        List<Map<String, Object>> dummy = List.of(
                Map.of("latitude",  35.80883,   "longitude", 127.14799, "SSID", "Public WiFi Free", "location", "완산구 곤지산4길 12, 전주시립도서관 (동완산동)"),
                Map.of("latitude",  35.79145,   "longitude", 127.13488, "SSID", "Public WiFi Free", "location", "완산구 평화14길 27-51, 평화도서관 (평화동2가)"),
                Map.of("latitude",  35.8176718, "longitude", 127.1015345, "SSID", "hyoja4", "location", "완산구 우전로 259")
        );

        OverlayImage MarkerImage = OverlayImage.fromResource(R.drawable.wifi_photoroom);

        // 반복문으로 마커 생성
        for (int i = 0; i < dummy.size(); i++) {
            Map<String, Object> item = dummy.get(i);

            double lat = (Double) item.get("latitude");
            double lng = (Double) item.get("longitude");

            // 마커 생성 및 설정
            Marker marker = new Marker();
            marker.setPosition(new LatLng(lat, lng));
            marker.setIcon(MarkerImage);
            marker.setWidth(MARKER_SIZE);
            marker.setHeight(MARKER_SIZE);
//            marker.setMinZoom(MARKER_MIN_ZOOM);

            marker.setOnClickListener(overlay -> {
                showWifiBottomSheet(
                        item.get("SSID").toString(),
                        item.get("location").toString(),
                        "85 Mbps",
                        "24시간",
                        "350m"
                );
                return true;
            });

            marker.setMap(naverMap);
        }





//        marker1.setOnClickListener( o -> {
//            Toast.makeText(MainActivity.this, "마커 " + marker1.getTag() + " 클릭됨", Toast.LENGTH_SHORT).show();
//            return true;
//        });
//        marker1.setTag("1");
//
//        // 클릭 해제
//        marker2.setOnClickListener(null);
//
//        // 지도에 추가
//        marker1.setMap(naverMap);
//
//        // 지도를 클릭하면 정보 창을 닫음
//        naverMap.setOnMapClickListener((coord, point) -> {
//            Log.v("마커클릭","지도 클릭함");
//        });
//
//        // 마커를 클릭하면:
//        Overlay.OnClickListener listener = overlay -> {
//            Marker markers = (Marker)overlay;
//            if (markers.getInfoWindow() == null) {
//                // 현재 마커에 정보 창이 열려있지 않을 경우 엶
//                Log.v("마커클릭","마커 정보 열기");
//            } else {
//                // 이미 현재 마커에 정보 창이 열려있을 경우 닫음
//                Log.v("마커클릭","마커 정보창 닫기");
//            }
//
//            return true;
//        };
//
//        marker1.setOnClickListener(listener);
//        marker2.setOnClickListener(listener);

    }

    private void showWifiBottomSheet(String ssid,
                                     String location,
                                     String speed,
                                     String position,
                                     String name) {
        // 레이아웃 인플레이트
        View sheet = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_wifi, null);

        // 데이터 바인딩
        ((TextView) sheet.findViewById(R.id.tv_ssid)).setText(ssid);
        ((TextView) sheet.findViewById(R.id.tv_location)).setText(location);
        ((TextView) sheet.findViewById(R.id.tv_speed)).setText("예상 속도: " + speed);
        // ... tv_hours, tv_distance 등도 동일하게

        Button btnDir     = sheet.findViewById(R.id.btn_direction);
        Button btnDetails = sheet.findViewById(R.id.btn_details);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheet);
        dialog.show();

        btnDir.setOnClickListener(v -> {
            // 길 찾기 인텐트 실행
            Toast.makeText(MainActivity.this, "길찾기 버튼", Toast.LENGTH_SHORT).show();
//            dialog.dismiss();
        });
        btnDetails.setOnClickListener(v -> {
            // 상세 화면 열기
            Toast.makeText(MainActivity.this, "상세보기 버튼", Toast.LENGTH_SHORT).show();
            //            dialog.dismiss();

        });
    }
}

