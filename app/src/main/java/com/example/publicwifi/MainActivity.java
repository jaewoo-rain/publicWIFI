package com.example.publicwifi;

import android.os.Bundle;
import android.util.Log;
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
import com.example.publicwifi.retrofit.RetrofitClient;
import com.example.publicwifi.retrofit.WifiItem;
import com.example.publicwifi.retrofit.WifiResponse;
import com.example.publicwifi.retrofit.WifiService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.naver.maps.geometry.LatLng;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {
    private NaverMap naverMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final int MARKER_MIN_ZOOM = 11;
    private static final int MARKER_SIZE = 180;
    private LatLng lastLongPress; // 마커 추가하기위한 좌표

    MyDBHelper myHelper;

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


    /**
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
        /**
         * 맵이 완전히 준비된 후 할 일
         */

        this.naverMap = naverMap;
        addPublicMarkers();
        addUserMarkers();
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



    }


    /**
     * 와이파이 목록 조회
     */
    private void showWifiListDialog() {
//        MyDBHelper dbHelper = new MyDBHelper(this);
        List<WifiData> wifiList = myHelper .getAllWifi();

        if (wifiList.isEmpty()) {
            Toast.makeText(this, "저장된 와이파이가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_wifi_list, null);
        ListView listView = dialogView.findViewById(R.id.wifi_list_view);

        final AlertDialog[] dialog = new AlertDialog[1];

        // 1) 지도 마커 갱신 로직을 Runnable 으로 정의
        Runnable refreshMarkers = () -> {
            // (a) 기존 userMarkers만 지도에서 제거
            for (Marker m : userMarkers) {
                m.setMap(null);
            }
            userMarkers.clear();

            // (b) DB에서 다시 읽어서 사용자 마커만 다시 그리기
            addUserMarkers();

            // (c) 리스트가 비어 있으면 다이얼로그 닫기
            if (wifiList.isEmpty() && dialog[0] != null) {
                dialog[0].dismiss();
            }
        };

        // 2) 어댑터에 Runnable 전달
        WifiAdapter adapter = new WifiAdapter(
                this,
                wifiList,
                myHelper ,
                refreshMarkers,        // ← 여기!
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


//        WifiAdapter adapter = new WifiAdapter(
//                this,
//                wifiList,
//                dbHelper,
//                () -> {
//                    if (wifiList.isEmpty()) dialog[0].dismiss();
//                },
//                selected -> {
//                    LatLng target = new LatLng(selected.latitude, selected.longitude);
//                    if (naverMap != null) {
//
//                        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(target).animate(CameraAnimation.Easing, 1000)
//                        .finishCallback(() -> {
//                            Toast.makeText(MainActivity.this, "카메라 이동 완료", Toast.LENGTH_SHORT).show();
//
//                        }).cancelCallback(()-> {
//                            Toast.makeText(MainActivity.this, "카메라 이동 취소", Toast.LENGTH_SHORT).show();
//                        });
//                        naverMap.moveCamera(cameraUpdate);
//                        Toast.makeText(this, selected.name + " 위치로 이동", Toast.LENGTH_SHORT).show();
//                    }
//                    dialog[0].dismiss();
//                }
//        );

        listView.setAdapter(adapter);

        dialog[0] = new AlertDialog.Builder(this)
                .setTitle("저장된 와이파이 목록")
                .setView(dialogView)
                .setNegativeButton("닫기", null)
                .setPositiveButton("전체 삭제", (d, i) -> {
                    myHelper .deleteAllWifi();
                    wifiList.clear();
                    adapter.notifyDataSetChanged();

                    // 전체 삭제
                    refreshMarkers.run();
                    Toast.makeText(this, "모든 와이파이를 삭제했습니다", Toast.LENGTH_SHORT).show();
                    dialog[0].dismiss();

                })
                .create();

        dialog[0].show();
    }


    /**
     * 와이파이 마커 넣기
     */
    private final List<Marker> publicMarkers = new ArrayList<>();
    private final List<Marker> userMarkers  = new ArrayList<>();

    // 1) 더미 마커만 그리는 메서드
    private void addPublicMarkers() {
        OverlayImage icon = OverlayImage.fromResource(R.drawable.wifi);

        // 기존 더미 마커 제거
        for (Marker m : publicMarkers) m.setMap(null);
        publicMarkers.clear();

        // 화면에 띄울 더미 데이터
        List<Map<String, Object>> publicWifi = new ArrayList<>();

        // 공공 와이파이 불러오기
        WifiService wifiService = RetrofitClient.getInstance().create(WifiService.class);

        wifiService.getWifiList().enqueue(new Callback<WifiResponse>() {
            @Override
            public void onResponse(Call<WifiResponse> call, Response<WifiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WifiItem> wifiList = response.body().getWifi();

                    for (WifiItem item : wifiList) {
                        publicWifi.add(
                                Map.of("latitude", item.getLatitude(),   "longitude", item.getLongitude(),  "SSID", item.getName(), "location", item.getAddress(), "manager_center",item.getCenter(),"contact",item.getContact()));
                        Log.d("RETROFIT", "이름: " + item.getName() + ", 주소: " + item.getAddress());
                    }

                    for (Map<String,Object> item : publicWifi) {
                        double lat = (Double)item.get("latitude");
                        double lng = (Double)item.get("longitude");
                        Marker m = new Marker();
                        m.setPosition(new LatLng(lat,lng));
                        m.setIcon(icon);
                        m.setWidth(MARKER_SIZE);
                        m.setHeight(MARKER_SIZE);
                        m.setMap(naverMap);
                        m.setOnClickListener(overlay -> {
                            showWifiBottomSheet(
                                    item.get("SSID").toString(), // 와이파이이름
                                    item.get("location").toString(), // 와이파이 주소
                                    item.get("manager_center").toString(),    // 관리기관
                                    "비밀번호 없음",      // 비밀번호
                                    "관리자번호 - "+item.get("contact").toString()        // 관리자 번호
                            );
                            return true;
                        });
                        publicMarkers.add(m);
                    }

                    Toast.makeText(MainActivity.this, "성공적으로 데이터를 불러왔습니다!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("RETROFIT", "응답 실패 - 코드: " + response.code());
                    Toast.makeText(MainActivity.this, "응답 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WifiResponse> call, Throwable t) {
                Log.e("RETROFIT", "요청 실패: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });




    }

    // 2) 사용자 마커만 그리는 메서드
    private void addUserMarkers() {

        for (Marker m : userMarkers) {
            m.setMap(null);
        }
        userMarkers.clear();

        OverlayImage icon = OverlayImage.fromResource(R.drawable.wifi);
        for (WifiData w : myHelper.getAllWifi()) {
            Marker m = new Marker();
            m.setPosition(new LatLng(w.latitude,w.longitude));
            m.setIcon(icon);
            m.setWidth(MARKER_SIZE);
            m.setHeight(MARKER_SIZE);
            m.setMap(naverMap);
            m.setOnClickListener(o -> {
                showWifiBottomSheet(w.name, w.address, "없음", w.password, w.description);
                return true;
            });
            userMarkers.add(m);
        }
    }

    /**
     * 와이파이 상세 정보 보여주기
     */
    private void showWifiBottomSheet(String ssid, // 와이파이이름
                                     String location,// 와이파이 주소
                                     String center,// 관리기관
                                     String password, // 비밀번호
                                     String description // 관리자 번호 or 세부 셜명
    ) {
        // 레이아웃 인플레이트
        View sheet = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_wifi, null);


        // 데이터 바인딩
        ((TextView) sheet.findViewById(R.id.tv_ssid)).setText(ssid);
        ((TextView) sheet.findViewById(R.id.tv_location)).setText("주소: "+location);
        ((TextView) sheet.findViewById(R.id.center)).setText("관리자: "+center);
        ((TextView) sheet.findViewById(R.id.password)).setText("비밀번호: "+password);
        ((TextView) sheet.findViewById(R.id.description)).setText("세부사항: "+description);

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
     * 와이파이 추가하기
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
        EditText inputAdress = dialogView.findViewById(R.id.edit_wifi_address);

        builder.setPositiveButton("저장", (dialog, which) -> {
            String wifiName = inputName.getText().toString();
            String password = inputPassword.getText().toString();
            String description = inputDescription.getText().toString();
            String address = inputAdress.getText().toString();

            MyDBHelper dbHelper = new MyDBHelper(this);
            dbHelper.saveWifi(wifiName, password, lat, lng, description, address);

            Toast.makeText(this, "와이파이 저장됨!", Toast.LENGTH_SHORT).show();

            // 2) naverMap 이 준비된 상태라면, 바로 마커 추가
            if (naverMap != null) {
                Marker newMarker = new Marker();
                newMarker.setPosition(new LatLng(lat, lng));
                newMarker.setIcon(OverlayImage.fromResource(R.drawable.wifi));
                newMarker.setWidth(MARKER_SIZE);
                newMarker.setHeight(MARKER_SIZE);
                // 클릭 시 BottomSheet 띄우는 로직도 동일하게
                newMarker.setOnClickListener(overlay -> {
                    showWifiBottomSheet(wifiName, address, "없음", password, description);
                    return true;
                });
                newMarker.setMap(naverMap);
                userMarkers.add(newMarker);

            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}

