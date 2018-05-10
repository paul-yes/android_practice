/*
 * Project: BluetoothPractice
 * Describe: Use bluetooth to connect Arduino and Android App.
             1. When you press the button in Android App, the led in Arduino will turn on if it is off, and turn off if it is on.
			 2. When you press the button in Arduino, android app will new a dialog to tell this message.
 * Related Program: Arduino_Bluetooth_Practice(Arduino)
 * Create date: 2018/04/05
 * Complete date: 2018/
 * Version: 1.0 //No version control.
*/

package tw.edu.iby_studio.bluetoothpractice;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private TextView textview;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice = null;

    private final int REQUEST_ENABLE_BT = 1;
    private final int BLUETOOTH_PERMISSION_REQ = 8;
    private final String HC08_UUID = "0000FFE1-0000-1000-8000-00805F9B34FB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.button);
        textview = (TextView)findViewById(R.id.textview);
        textview.setText("Start!\n");

        permissionCheck();

        bluetoothSetup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case BLUETOOTH_PERMISSION_REQ:
                bluetoothSetup();
                break;
        }
        permissionCheck();
    }

    public void permissionCheck(){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, BLUETOOTH_PERMISSION_REQ);
        }

    }

    public void bluetoothSetup(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null){
            Toast.makeText(this, "No Bluetooth in this device.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mBluetoothAdapter.enable()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(this, "Enable Bluetooth.", Toast.LENGTH_SHORT).show();
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device:pairedDevices){
                if(device.getAddress().equals("94:E3:6D:9C:78:44")){
                    mBluetoothDevice = device;
                    startupDevice();
                }
                textview.setText(textview.getText() + "\n" + device.getName() + "/" + device.getAddress());
            }
        }

        if(mBluetoothDevice == null){
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
            mBluetoothAdapter.startDiscovery();
        }


    }

    public void startupDevice(){
        Toast.makeText(MainActivity.this, "Find a matching Bluetooth Device.", Toast.LENGTH_SHORT).show();
        ConnectThread mConnectThread = new ConnectThread(mBluetoothDevice);
        mConnectThread.start();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                textview.setText(textview.getText() + "\n" + device.getName() + "/" + device.getAddress());
                if(device.getAddress().equals("94:E3:6D:9C:78:44")){
                    mBluetoothAdapter.cancelDiscovery();
                    mBluetoothDevice = device;
                    startupDevice();
                }
            }
        }
    };



    public class ConnectThread extends Thread{
        private BluetoothDevice mmDevice;
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device){
            BluetoothSocket tmpSocket = null;

            mmDevice = device;

            try{
                tmpSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(HC08_UUID));
            } catch (IOException e) {
                Log.e("ConnectThread-1", e.getMessage());
            }

            mmSocket = tmpSocket;

        }

        public void run(){
            mBluetoothAdapter.cancelDiscovery();

            try {
                //Android 4.2↓
                mmSocket.connect();
                Log.e("ConnectThread", "Connected");
            } catch (IOException e) {
                //Anrdroid 4.2↑
                Log.e("ConnectThread-2", e.getMessage());
                try {
                    mmSocket = (BluetoothSocket)mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice, 1);
                    mmSocket.connect();
                    Log.e("ConnectThread", "Connected");
                } catch (Exception e2) {
                    Log.e("ConnectThread-21", e.getMessage());
                    cancel();
                }
            }

            ConnectedThread connectedThread = new ConnectedThread(mmSocket);
            connectedThread.start();
        }

        public void cancel(){

            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("ConnectThread-3", e.getMessage());
            }

        }

    }

    public class ConnectedThread extends Thread{
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothSocket mmSocket;
        private byte[] mmBuffer;

        public ConnectedThread(BluetoothSocket socket){
            InputStream tmpInStream = null;
            OutputStream tmpOutStream = null;

            mmSocket = socket;

            try {
                tmpInStream = mmSocket.getInputStream();
            } catch (IOException e) {
                Log.e("ConnectedThread", e.getMessage());
            }

            try {
                tmpOutStream = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e("ConnectedThread", e.getMessage());
            }

            mmInStream = tmpInStream;
            mmOutStream = tmpOutStream;

        }

        public void run(){
            mmBuffer = new byte[1024];
            int numBytes;

            while(true){
                try {
                    numBytes = mmInStream.read(mmBuffer);
                    mReadProcess.setup(mmBuffer);
                    runOnUiThread(mReadProcess);
                } catch (IOException e) {
                    Log.e("ConnectedThread", e.getMessage());
                }
            }

        }

        public void write(byte[] bytes){
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("ConnectedThread", e.getMessage());
            }
        }

        public void cancel(){

            try {
                mmSocket.close();
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e) {
                Log.e("ConnectedThread", e.getMessage());
            }

        }

    }

    private ReadProcess mReadProcess = new ReadProcess();
    public class ReadProcess implements Runnable{
        byte[] mData = new byte[1024];

        public void setup(byte[] bytes){
            mData = bytes;
        }

        @Override
        public void run() {
            textview.setText(textview.getText() + "\n" + mData.toString());
        }
    }





}
