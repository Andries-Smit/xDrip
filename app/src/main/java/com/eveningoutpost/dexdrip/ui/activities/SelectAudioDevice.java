package com.eveningoutpost.dexdrip.ui.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.HeadsetStateReceiver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * jamorham
 *
 * Selection of Bluetooth audio devices for vehicle mode.
 * Supports multiple devices via toggle list of bonded Bluetooth devices.
 */
public class SelectAudioDevice extends BaseAppCompatActivity {

    private static final String PREF_MAC_STORE = "vehicle-mode-audio-mac";   // legacy single
    private static final String PREF_MACS_STORE = "vehicle-mode-audio-macs"; // multi JSON

    private static final Gson gson = new GsonBuilder().create();

    private DeviceAdapter mAdapter;

    // ---- static API used by HeadsetStateReceiver ----

    public static Set<String> getAudioMacs() {
        migrateLegacyIfNeeded();
        final String json = Pref.getString(PREF_MACS_STORE, "[]");
        try {
            final String[] arr = gson.fromJson(json, String[].class);
            if (arr != null) return new HashSet<>(Arrays.asList(arr));
        } catch (Exception ignored) {
        }
        return new HashSet<>();
    }

    private static void saveAudioMacs(final Set<String> macs) {
        Pref.setString(PREF_MACS_STORE, gson.toJson(macs.toArray(new String[0])));
    }

    /** One-time migration of the old single-device preference into the new set. */
    private static void migrateLegacyIfNeeded() {
        final String legacy = Pref.getString(PREF_MAC_STORE, "");
        if (legacy.isEmpty() || legacy.equals("<NOT SET>")) return;
        if (!Pref.getString(PREF_MACS_STORE, "[]").equals("[]")) return; // already migrated
        final Set<String> macs = new HashSet<>();
        macs.add(legacy);
        saveAudioMacs(macs);
        Pref.setString(PREF_MAC_STORE, ""); // clear legacy
    }

    // ---- Activity ----

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_audio_device);
        JoH.fixActionBar(this);

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            JoH.static_toast_long(getString(R.string.no_bluetooth_audio_found));
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            JoH.static_toast_long(getString(R.string.no_bluetooth_audio_found));
            finish();
            return;
        }

        final Set<BluetoothDevice> bonded = adapter.getBondedDevices();
        if (bonded == null || bonded.isEmpty()) {
            JoH.static_toast_long(getString(R.string.no_bluetooth_audio_found));
            finish();
            return;
        }

        final Set<String> savedMacs = getAudioMacs();
        final List<DeviceItem> items = new ArrayList<>();
        for (BluetoothDevice device : bonded) {
            items.add(new DeviceItem(device, savedMacs.contains(device.getAddress())));
        }

        final RecyclerView recyclerView = findViewById(R.id.audio_devices_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DeviceAdapter(items);
        recyclerView.setAdapter(mAdapter);
    }

    // ---- Data model ----

    private static class DeviceItem {
        final String mac;
        final String name;
        boolean selected;

        DeviceItem(BluetoothDevice device, boolean selected) {
            this.mac = device.getAddress();
            this.name = device.getName() != null ? device.getName() : device.getAddress();
            this.selected = selected;
        }
    }

    // ---- Adapter ----

    private class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

        private final List<DeviceItem> items;

        DeviceAdapter(List<DeviceItem> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_audio_device, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final DeviceItem item = items.get(position);
            holder.nameView.setText(item.name);
            holder.macView.setText(item.mac);
            holder.toggle.setOnCheckedChangeListener(null); // avoid spurious callbacks during bind
            holder.toggle.setChecked(item.selected);
            holder.toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.selected = isChecked;
                persistSelection(item.mac, isChecked);
                if (isChecked) {
                    HeadsetStateReceiver.reprocessConnectionIfAlreadyConnected(item.mac);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private void persistSelection(final String mac, final boolean add) {
            final Set<String> macs = getAudioMacs();
            if (add) macs.add(mac); else macs.remove(mac);
            saveAudioMacs(macs);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView nameView;
            final TextView macView;
            final Switch toggle;

            ViewHolder(View view) {
                super(view);
                nameView = view.findViewById(R.id.audio_device_name);
                macView = view.findViewById(R.id.audio_device_mac);
                toggle = view.findViewById(R.id.audio_device_toggle);
            }
        }
    }
}
