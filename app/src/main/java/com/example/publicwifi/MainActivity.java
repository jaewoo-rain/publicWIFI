package com.example.publicwifi;

import android.os.Bundle;
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

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

//        FragmentManager fm = getSupportFragmentManager();
//
//        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
//
//        if (mapFragment == null) {
//
//            NaverMapOptions options = new NaverMapOptions()
//                    .camera(new CameraPosition(new LatLng(35.84598322535996,  127.12894805415627), 8))
//                    .mapType(NaverMap.MapType.Terrain);
//
//
//            mapFragment = MapFragment.newInstance(options);
//            fm.beginTransaction()
//                    .add(R.id.map, mapFragment)
//                    .commit();
//        }
//
//
//        // 지도가 준비되면 onMapReady() 호출
//        mapFragment.getMapAsync(this);

        FragmentManager fm = getSupportFragmentManager();

        // 최초 한 번만 옵션을 적용해서 Fragment 생성
        if (savedInstanceState == null) {
            NaverMapOptions options = new NaverMapOptions()
                    .camera(new CameraPosition(
                            new LatLng(35.84598322535996, 127.12894805415627),
                            13))
                    .mapType(NaverMap.MapType.Terrain);

            MapFragment mapFragment = MapFragment.newInstance(options);
            fm.beginTransaction()
                    .add(R.id.map, mapFragment)
                    // .commitNow() 해도 좋습니다
                    .commit();
            mapFragment.getMapAsync(this);
        }
        else {
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
            .include(new LatLng(37.5640984, 126.9712268))
            .include(new LatLng(37.5651279, 126.9767904))
            .include(new LatLng(37.5625365, 126.9832241))
            .include(new LatLng(37.5585305, 126.9809297))
            .include(new LatLng(37.5590777, 126.974617))
            .build();


    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        // 마커, 카메라 이동 등 지도 제어 로직

        // 지도 유형
        // Basic: 일반 지도, Navi: 차량용 내비게이션에 특화된 지도, Satellite: 위성 지도, Terrain: 지형도
        // Hybrid: 위성 사진과 도로, 심벌을 함께 노출하는 하이브리드 지도, NaviHybrid: 위성 사진과 내비게이션용 도로, 심벌을 함께 노출하는 하이브리드 지도
        // None: 지도를 나타내지 않습니다. 단, 오버레이는 여전히 나타납니다.
        naverMap.setMapType(NaverMap.MapType.Basic);
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BUILDING, false); // 건물 그룹
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false); // 지적편집도 그룹
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRAFFIC, true); // 실시간 교통 정보
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, false); // 대중교통 그룹
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BICYCLE, false); // 자전거 그룹
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_MOUNTAIN, false); // 등산로 그룹

        naverMap.setIndoorEnabled(true); // 실내 지도
        naverMap.setNightModeEnabled(true); // 야간모드 -> Navi 지도에서만 사용 가능
        naverMap.setLightness(0.3f); // 지도 밝기 -1 ~ 1, 1일수록 밝음
        naverMap.setBuildingHeight(0.5f); // 건물높이 : 0.5f => 50%, 지도가 기울어질때 입체적으로 표현
        naverMap.setSymbolScale(2); // 심벌 크기 조절
        naverMap.setSymbolPerspectiveRatio(0); // 심벌 원근 효과, 지도를 기울일 경우 멀리있는것은 작게보임








    }
}

