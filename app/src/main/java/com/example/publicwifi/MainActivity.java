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
            // 이미 붙어 있는 Fragment를 꺼내기
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



    /**
     * 맵이 완전히 준비된 후 할 일
     */
    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        addPublicMarkers(); // 공공와이파이 추가
        addUserMarkers(); // 나만의 와이파이 추가

        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLogoClickEnabled(false); // 네이버 로고 클릭 비활성화
        naverMap.setMapType(NaverMap.MapType.Basic);
        naverMap.setIndoorEnabled(true); // 실내 지도
        naverMap.setSymbolScale(1); // 심벌 크기 조절

        // 지도 최소, 최대 줌 레벨
        naverMap.setMinZoom(5.0);
        naverMap.setMaxZoom(18.0);

        /*
         * 나만의 wifi 추가하기
         */
        // 길게 클릭시 클릭 위치 좌표 띄우기 -> 나만의 마커 추가하기
        naverMap.setOnMapLongClickListener((pointF, latLng) -> {
            // 1) 저장해 둔 최근 long-press 위치
            lastLongPress = latLng;

            showWifiInputDialog(latLng.latitude, latLng.longitude);
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

            // naverMap 이 준비된 상태라면, 바로 마커 추가
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


    /**
     * 와이파이 목록 조회
     */
    private void showWifiListDialog() {
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
            // 기존 userMarkers만 지도에서 제거
            for (Marker m : userMarkers) {
                m.setMap(null);
            }
            userMarkers.clear();

            // DB에서 다시 읽어서 사용자 마커만 다시 그리기
            addUserMarkers();

            // 리스트가 비어 있으면 다이얼로그 닫기
            if (wifiList.isEmpty() && dialog[0] != null) {
                dialog[0].dismiss();
            }
        };

        // 2) 어댑터에 Runnable 전달
        WifiAdapter adapter = new WifiAdapter(
                this,
                wifiList,
                myHelper ,
                refreshMarkers,
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

    // 공공와이파이
    private void addPublicMarkers() {
        OverlayImage icon = OverlayImage.fromResource(R.drawable.wifi1);

        // 기존 마커 제거
        for (Marker m : publicMarkers) m.setMap(null);
        publicMarkers.clear();

        List<Map<String, Object>> publicWifi = new ArrayList<>();
        WifiService wifiService = RetrofitClient.getInstance().create(WifiService.class);

        // DB 연결하여 공공와이파이 정보가져오기
        wifiService.getWifiList().enqueue(new Callback<WifiResponse>() {
            @Override
            public void onResponse(Call<WifiResponse> call, Response<WifiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WifiItem> wifiList = response.body().getWifi();

                    // WifiItem을 이용하여 정보들 저장하기
                    for (WifiItem item : wifiList) {
                        publicWifi.add(
                                Map.of("latitude", item.getLatitude(),   "longitude", item.getLongitude(),  "SSID", item.getName(),
                                        "location", item.getAddress(), "manager_center",item.getCenter(),"contact",item.getContact()));
                        Log.d("RETROFIT", "이름: " + item.getName() + ", 주소: " + item.getAddress());
                    }

                    // 와이파이 마커 다시 표시하기
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
    // 직접 작성한 마커
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

}


