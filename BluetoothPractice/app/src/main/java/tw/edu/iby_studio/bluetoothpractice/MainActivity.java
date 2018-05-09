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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private TextView textview;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice = null;

    private final int REQUEST_ENABLE_BT = 1;
    private final int BLUETOOTH_PERMISSION_REQ = 8;

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
        if(pairedDevices.size()>0){
            for(BluetoothDevice device:pairedDevices){
                if(device.getAddress().equals("94:E3:6D:9C:78:44")){
                    mBluetoothDevice = device;
                    Toast.makeText(this, "Find a matching Bluetooth Device.", Toast.LENGTH_SHORT).show();
                }
                textview.setText(textview.getText() + "\n" + device.getName() + "/" + device.getAddress());
            }
        }

        if(mBluetoothDevice!=null){
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
            mBluetoothAdapter.startDiscovery();
        }




    }



    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                textview.setText(textview.getText() + "\n" + device.getName() + "/" + device.getAddress());
                if(device.getAddress().equals("94:E3:6D:9C:78:44")){
                    mBluetoothDevice = device;
                    mBluetoothAdapter.cancelDiscovery();
                    Toast.makeText(MainActivity.this, "Find a matching Bluetooth Device.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };


}
