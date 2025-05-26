package com.example.publicwifi;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.example.publicwifi.model.WifiData;
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
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {
    private Button btn1, btn2, btn3;
    private NaverMap naverMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final int MARKER_MIN_ZOOM = 11;
    private static final int MARKER_SIZE = 180;
    private LatLng lastLongPress; // 마커 추가하기위한 좌표

    MyDBHelper myHelper;
    SQLiteDatabase sqlDB;

    FloatingActionButton fab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        fab = findViewById(R.id.fab_wifi_list);
        fab.setOnClickListener(v -> showWifiListDialog());

        myHelper = new MyDBHelper(this);
        FragmentManager fm = getSupportFragmentManager();

//        btn1 = findViewById(R.id.btn1);
//        btn2 = findViewById(R.id.btn2);
//        btn3 = findViewById(R.id.btn3);

//        // 화면움직임 + 여러가지 화면 세팅 가능
//        btn1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (naverMap == null) return;  // 아직 준비 안 됐으면 무시
//                CameraPosition cameraPosition = new CameraPosition(
//                        new LatLng(35.80883, 127.14799), // 대상 지점
//                        16, // 줌 레벨
//                        20, // 기울임 각도
//                        180 // 베어링 각도
//                );
//                // 카메라만 부드럽게 이동
//                naverMap.moveCamera(CameraUpdate.toCameraPosition(cameraPosition));
//            }
//        });

//        // 다른 세팅은 그대로, 화면만 움직임 + 애니메이션 가능 (duration은 없어도됨)
//        btn2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(35.8068378,127.1195137)).animate(CameraAnimation.Easing, 1000)
//                        .finishCallback(() -> {
//                            Toast.makeText(MainActivity.this, "카메라 이동 완료", Toast.LENGTH_SHORT).show();
//
//                        })
//                        .cancelCallback(()-> {
//                            Toast.makeText(MainActivity.this, "카메라 이동 취소", Toast.LENGTH_SHORT).show();
//                        });
//                naverMap.moveCamera(cameraUpdate);
//            }
//        });


        // 1) FusedLocationSource 인스턴스 생성 (requestCode는 아무 값이나 정해두면 됩니다)
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);


        // 최초 한 번만 옵션을 적용해서 Fragment 생성
        if (savedInstanceState == null) {
            NaverMapOptions options = new NaverMapOptions()
                    .locationButtonEnabled(true)
                    .camera(new CameraPosition(
                            new LatLng(35.8570947, 127.1210231), 13))
                    .mapType(NaverMap.MapType.Terrain);

            MapFragment mapFragment = MapFragment.newInstance(options);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.map, mapFragment)
                    .commit();

            mapFragment.getMapAsync(this);
        } else {
            // 이미 붙어 있는 Fragment를 꺼내서

            MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }


    }





//    // 하나의 좌표
//    LatLng southWest = new LatLng(31.43, 122.37);
//    LatLng northEast = new LatLng(44.35, 132);
//    LatLngBounds bounds1 = new LatLngBounds(southWest, northEast);
//
//    // 여러 좌표 포함하는 MBR 만들기
//    LatLngBounds bounds2 = new LatLngBounds.Builder()
//            .include(new LatLng(35.80883, 127.14799))
//            .include(new LatLng(35.8068378, 127.1195137))
//            .include(new LatLng(35.7968626, 127.1143079))
//            .include(new LatLng(35.7980662, 127.1403211))
//            .include(new LatLng(35.812153, 127.1198025))
//            .build();

    /*
     * 권한 설정
     */
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
    public void onMapReady(@NonNull NaverMap naverMap) {
        /*
         * 맵이 완전히 준비된 후 할 일
         */
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        // 기본 트래킹 모드 설정 (권한이 있으면 위치 표시, 없으면 추적 안 함)
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        UiSettings uiSettings = naverMap.getUiSettings();

//        // 무수히 많이 일어남
//        naverMap.addOnCameraChangeListener((reason, animated) -> {
//            Log.i("NaverMap", "카메라 변경 - reson: " + reason + ", animated: " + animated);
////            Toast.makeText(MainActivity.this, "카메라 변경 - reson: " + reason + ", animated: " + animated, Toast.LENGTH_SHORT).show();
//
//        });
//
//        naverMap.addOnCameraIdleListener(() -> {
////            Toast.makeText(MainActivity.this, "카메라 움직임 종료", Toast.LENGTH_SHORT).show();
//        });

        // 지도 최소, 최대 줌 레벨
        naverMap.setMinZoom(5.0);
        naverMap.setMaxZoom(18.0);

//        // 현재 줌 레벨 확인
//        naverMap.addOnCameraChangeListener((reason, animated) -> {
//            double zoom = naverMap.getCameraPosition().zoom;
////            Log.d("ZoomLevel", "현재 줌 레벨: " + zoom);
////            Toast.makeText(MainActivity.this, "현재 줌 레벨: " + zoom, Toast.LENGTH_SHORT).show();
//        });

        // 네이버 로고 클릭 비활성화
        uiSettings.setLogoClickEnabled(false);


        /*
         * 나만의 wifi 추가하기
         */
        // 길게 클릭시 클릭 위치 좌표 띄우기 -> 나만의 마커 추가하기
        naverMap.setOnMapLongClickListener((pointF, latLng) -> {
            // 1) 저장해 둔 최근 long-press 위치
            lastLongPress = latLng;
            // 2) 추가 버튼 보이기
//            showAddButton();

            showWifiInputDialog(latLng.latitude, latLng.longitude);
        });



//        // 마커 클릭 시 클릭 이벤트
//        Marker marker = new Marker();
//        marker.setPosition(new LatLng(35.80883, 127.14799));
//        marker.setMap(naverMap); // 지도를 붙여 주고…
//        marker.setOnClickListener(overlay -> {
//            Toast.makeText(this, "마커 1 클릭", Toast.LENGTH_SHORT).show();
//            return true;
//        });

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


    /**
     * 와이파이 목록
     */
    private void showWifiListDialog() {
        MyDBHelper dbHelper = new MyDBHelper(this);
        List<WifiData> wifiList = dbHelper.getAllWifi();

        if (wifiList.isEmpty()) {
            Toast.makeText(this, "저장된 와이파이가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_wifi_list, null);
        ListView listView = dialogView.findViewById(R.id.wifi_list_view);

        final AlertDialog[] dialog = new AlertDialog[1];

        WifiAdapter adapter = new WifiAdapter(
                this,
                wifiList,
                dbHelper,
                () -> {
                    if (wifiList.isEmpty()) dialog[0].dismiss();
                },
                selected -> {
                    LatLng target = new LatLng(selected.latitude, selected.longitude);
                    if (naverMap != null) {

                        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(target).animate(CameraAnimation.Easing, 1000)
                        .finishCallback(() -> {
                            Toast.makeText(MainActivity.this, "카메라 이동 완료", Toast.LENGTH_SHORT).show();

                        }).cancelCallback(()-> {
                            Toast.makeText(MainActivity.this, "카메라 이동 취소", Toast.LENGTH_SHORT).show();
                        });
                        naverMap.moveCamera(cameraUpdate);
                        Toast.makeText(this, selected.name + " 위치로 이동", Toast.LENGTH_SHORT).show();
                    }
                    dialog[0].dismiss();
                }
        );

        listView.setAdapter(adapter);

        dialog[0] = new AlertDialog.Builder(this)
                .setTitle("저장된 와이파이 목록")
                .setView(dialogView)
                .setNegativeButton("닫기", null)
                .setPositiveButton("전체 삭제", (d, i) -> {
                    dbHelper.deleteAllWifi();
                    wifiList.clear();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "모든 와이파이를 삭제했습니다", Toast.LENGTH_SHORT).show();
                    dialog[0].dismiss();
                })
                .create();

        dialog[0].show();
    }


    /*
     * 공공 wifi 넣기
     */
    List<Map<String, Object>> dummy;
    private void addMarker() {
        OverlayImage markerImage = OverlayImage.fromResource(R.drawable.wifi_photoroom);

        // 1. 더미 데이터
        dummy = List.of(
                Map.of("latitude", 35.80883,   "longitude", 127.14799,  "SSID", "Public WiFi Free", "location", "전주시립도서관 (동완산동)"),
                Map.of("latitude", 35.79145,   "longitude", 127.13488,  "SSID", "Public WiFi Free", "location", "평화도서관 (평화동2가)"),
                Map.of("latitude", 35.8176718, "longitude", 127.1015345,"SSID", "hyoja4",           "location", "우전로 259")
        );

        // 1-1. 더미 마커 표시
        for (Map<String, Object> item : dummy) {
            double lat = (Double) item.get("latitude");
            double lng = (Double) item.get("longitude");

            Marker marker = new Marker();
            marker.setPosition(new LatLng(lat, lng));
            marker.setIcon(markerImage);
            marker.setWidth(MARKER_SIZE);
            marker.setHeight(MARKER_SIZE);

            marker.setOnClickListener(overlay -> {
                showWifiBottomSheet(
                        item.get("SSID").toString(), // 와이파이이름
                        item.get("location").toString(), // 와이파이 주소
                        "85 Mbps",    // 더미 속도
                        "비밀번호 없음",      // 비밀번호
                        "350m"        // 더미 거리
                );
                return true;
            });

            marker.setMap(naverMap);
        }

        // 2. SQLite 사용자 저장 Wi-Fi 마커 추가
        List<WifiData> userWifiList = myHelper.getAllWifi(); // 이미 선언된 dbHelper 사용

        OverlayImage markerImage2 = OverlayImage.fromResource(R.drawable.wifi_removebg);

        for (WifiData wifi : userWifiList) {
            Marker marker = new Marker();
            marker.setPosition(new LatLng(wifi.latitude, wifi.longitude));
            marker.setIcon(markerImage2);
            marker.setWidth(MARKER_SIZE);
            marker.setHeight(MARKER_SIZE);

            marker.setOnClickListener(overlay -> {
                showWifiBottomSheet(
                        wifi.name,
                        wifi.description,
                        "사용자 등록",
                        wifi.password,
                        wifi.name  // 마지막 인자: 이름 또는 임시 거리
                );
                return true;
            });

            marker.setMap(naverMap);
        }
    }


    /**
     * 와이파이 상세 정보 보여주기
     */
    private void showWifiBottomSheet(String ssid,
                                     String location,
                                     String speed,
                                     String password,
                                     String name) {
        // 레이아웃 인플레이트
        View sheet = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_wifi, null);

        // 데이터 바인딩
        ((TextView) sheet.findViewById(R.id.tv_ssid)).setText("ssid "+ssid);
        ((TextView) sheet.findViewById(R.id.tv_location)).setText("location "+location);
        ((TextView) sheet.findViewById(R.id.tv_speed)).setText("예상 속도: " + speed);
        ((TextView) sheet.findViewById(R.id.password)).setText("password: " + password);
        ((TextView) sheet.findViewById(R.id.name)).setText("position: ");

        Button btnDir = sheet.findViewById(R.id.btn_direction);
        Button btnDetails = sheet.findViewById(R.id.btn_details);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheet);
        dialog.show();

        btnDir.setOnClickListener(v -> {
            // 길 찾기 인텐트 실행
            Toast.makeText(MainActivity.this, "길찾기 버튼", Toast.LENGTH_SHORT).show();
            dialog.dismiss(); // 다이얼로그 창 닫기
        });
        btnDetails.setOnClickListener(v -> {
            // 상세 화면 열기
            Toast.makeText(MainActivity.this, "상세보기 버튼", Toast.LENGTH_SHORT).show();
            //  dialog.dismiss();
        });
    }

    /**
     * 와이파이 추가하기 창
     */
    private void showWifiInputDialog(double lat, double lng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("와이파이 정보 입력");

        // XML 레이아웃 inflate
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_wifi_input, null);
        builder.setView(dialogView);

        // XML에서 EditText 가져오기
        EditText inputName = dialogView.findViewById(R.id.edit_wifi_name);
        EditText inputPassword = dialogView.findViewById(R.id.edit_wifi_password);
        EditText inputDescription = dialogView.findViewById(R.id.edit_wifi_description);

        builder.setPositiveButton("저장", (dialog, which) -> {
            String wifiName = inputName.getText().toString();
            String password = inputPassword.getText().toString();
            String description = inputDescription.getText().toString();

            MyDBHelper dbHelper = new MyDBHelper(this);
            dbHelper.saveWifi(wifiName, password, lat, lng, description);

            Toast.makeText(this, "와이파이 저장됨!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}

