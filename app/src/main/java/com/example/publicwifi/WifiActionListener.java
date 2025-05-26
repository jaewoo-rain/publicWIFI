package com.example.publicwifi;

import com.example.publicwifi.model.WifiData;

public interface WifiActionListener {
    void onMove(WifiData data);  // 이동 버튼 클릭 시 실행될 콜백
}
