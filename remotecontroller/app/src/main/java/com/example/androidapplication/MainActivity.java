package com.example.remotecontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ArrayList<BluetoothDevice> deviceList;
    private ArrayAdapter<String> deviceAdapter;
    private ListView deviceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<>();
        deviceListView = findViewById(R.id.device_list);

        Button connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(view -> onConnectButtonClick());

        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceAdapter);

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = deviceList.get(position);
            connectToDevice(selectedDevice);
        });

        // Register for Bluetooth device discovery events
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    // Triggered when the "Connect" button is clicked
    public void onConnectButtonClick() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        deviceAdapter.clear(); // Clear previous list of devices
        deviceList.clear(); // Clear previous list of Bluetooth devices

        // Start device discovery
        bluetoothAdapter.startDiscovery();
        Toast.makeText(this, "Discovering devices...", Toast.LENGTH_SHORT).show();
    }

    // BroadcastReceiver for discovering Bluetooth devices
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress(); // MAC address

                // Add device to list if it's not already added
                if (!deviceList.contains(device)) {
                    deviceList.add(device);
                    deviceAdapter.add(deviceName + " - " + deviceAddress);
                }
            }
        }
    };

    // Connect to the selected Bluetooth device
    private void connectToDevice(BluetoothDevice device) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();

            // Once connected, send a message
            sendMessage("Hello from RemoteController!");

            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to connect", Toast.LENGTH_SHORT).show();
        }
    }

    // Send a message via Bluetooth
    private void sendMessage(String message) {
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver to stop discovering devices
        unregisterReceiver(receiver);

        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
