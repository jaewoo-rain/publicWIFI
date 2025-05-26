package com.example.publicwifi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.publicwifi.model.WifiData;

import java.util.List;

public class WifiAdapter extends BaseAdapter {
    private final Context context;
    private final List<WifiData> wifiList;
    private final MyDBHelper dbHelper;
    private final Runnable onDataChanged;
    private final WifiActionListener actionListener;

    public WifiAdapter(Context context, List<WifiData> wifiList, MyDBHelper dbHelper,
                       Runnable onDataChanged, WifiActionListener actionListener) {
        this.context = context;
        this.wifiList = wifiList;
        this.dbHelper = dbHelper;
        this.onDataChanged = onDataChanged;
        this.actionListener = actionListener;
    }

    @Override
    public int getCount() {
        return wifiList.size();
    }

    @Override
    public Object getItem(int position) {
        return wifiList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return wifiList.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wifilist, parent, false);

        TextView name = view.findViewById(R.id.tv_wifi_name);
        TextView desc = view.findViewById(R.id.tv_wifi_desc);
        Button deleteBtn = view.findViewById(R.id.btn_delete_wifi);
        Button moveBtn = view.findViewById(R.id.btn_move_wifi);

        WifiData data = wifiList.get(position);
        name.setText(data.name);
        desc.setText(data.description);

        deleteBtn.setOnClickListener(v -> {
            dbHelper.deleteWifi(data.id);
            wifiList.remove(position);
            notifyDataSetChanged();
            Toast.makeText(context, "삭제됨", Toast.LENGTH_SHORT).show();
            if (onDataChanged != null) onDataChanged.run();
        });

        moveBtn.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onMove(data);  // MainActivity에 콜백 전달
            }
        });

        return view;
    }
}
