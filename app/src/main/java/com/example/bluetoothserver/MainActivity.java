package com.example.bluetoothserver;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    /*Variabili inerenti al sensore*/
    private SensorManager sensorManager;
    private Sensor sensoreLuce;
    private Boolean isLightSensorAviable;
    TextView luceTW, valoreLuceTW,ListaSensori, temperaturaTW, valoreTemperaturaTW, umiditaTW, valoreUmiditaTW;
    long lastUpdate = 0;


    /*Variabili inerenti al Bluetooth*/
    Button MettitiInAscoltoBtn, inviaDatiBtn;
    TextView stato, StatoConnessioneTW;
    BluetoothAdapter bluetoothAdapter;
    Send send;
    private boolean sendData = false;
    int REQUEST_ENABLE_BLUETOOTH=1;

    /*Costanti che rappresentano lo stato di connesione del Bluetooth*/
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int REQUEST_ENABLE_PERMISSION = 1;

    private static final String APP_NAME = "MaMange";
    private static final UUID MY_UUID=UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewByIdes();
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            checkBTPermission();
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
            sensoreLuce = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            Toast.makeText(getApplicationContext(),"Sensor is Aviable", Toast.LENGTH_SHORT);
            isLightSensorAviable = true;
        } else {
            Toast.makeText(getApplicationContext(),"Sensor is not Aviable", Toast.LENGTH_SHORT);
            isLightSensorAviable = false;
        }

        implementListeners();


    }

    public boolean checkBTPermission(){
        ArrayList<String> permissions = new ArrayList<>();
        int count = 0;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_DENIED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            } else{
                count++;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_DENIED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            } else{
                count++;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_DENIED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }else {
                count++;
            }
            if (permissions.size() != 0) {
                System.out.println("Missing the following permissions: " + permissions.toString());
                requestPermissions(permissions.toArray(new String[0]),REQUEST_ENABLE_PERMISSION);

                return false;
            }



        } else if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                    == PackageManager.PERMISSION_DENIED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            } else{
                count++;
            }
            if (permissions.size() != 0) {
                System.out.println("Missing the following permissions: " + permissions.toString());
                requestPermissions(permissions.toArray(new String[0]),REQUEST_ENABLE_PERMISSION);

                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                    == PackageManager.PERMISSION_DENIED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            } else{
                count++;
            }
            if (permissions.size() != 0) {
                System.out.println("Missing the following permissions: " + permissions.toString());
                requestPermissions(permissions.toArray(new String[0]),REQUEST_ENABLE_PERMISSION);
                return false;
            }
        }

        return true;
    }

    private void implementListeners() {

        MettitiInAscoltoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClasseServer classeServer =new ClasseServer();

                classeServer.start();

            }
        });

        inviaDatiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData = !sendData;

            }
        });

    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message messaggio) {

            switch (messaggio.what)
            {
                case STATE_CONNECTING:
                    stato.setText("Connessione in corso...");
                    break;
                case STATE_CONNECTED:
                    stato.setText("Connesso");
                    break;
                case STATE_CONNECTION_FAILED:
                    stato.setText("Connessione fallita");
                    break;
            }
            return true;
        }
    });

    private void findViewByIdes() {

        MettitiInAscoltoBtn = findViewById(R.id.MettitiInAscolto);
        inviaDatiBtn = findViewById(R.id.InviaDati);
        stato = findViewById(R.id.Stato);
        StatoConnessioneTW = findViewById(R.id.StatoConnessioneTW);
        valoreLuceTW = findViewById(R.id.valoreLuceSendore);
        luceTW = findViewById(R.id.luceSensore);
        ListaSensori = findViewById(R.id.ListaSensori);
        valoreTemperaturaTW = findViewById(R.id.valoreTemperaturaSensore);
        temperaturaTW = findViewById(R.id.temperaturaSensore);
        valoreUmiditaTW = findViewById(R.id.valoreUmiditaSensore);
        umiditaTW = findViewById(R.id.umiditaSensore);

    }

    private double randomTemperature(){
        return Math.random() * (16 - 12 + 1) + 12  ;
    }

    private double randomHumidity(){
        return Math.random() * (70 - 50 + 1) + 50  ;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {



        long actualTime = sensorEvent.timestamp; //get the event's timestamp

        if(actualTime - lastUpdate > 2000000000) {

            lastUpdate = actualTime;

            String lightTemp = String.valueOf(sensorEvent.values[0])+" NTU!";
            String temperatureTemp = String.valueOf((int) randomTemperature())+" °C!";
            String humidityTemp = String.valueOf((int) randomHumidity())+" kg/m³";
            String unica = lightTemp+temperatureTemp+humidityTemp;
            if(sendData) {

                send.write(unica.getBytes());

            }

            valoreLuceTW.setText(lightTemp);
            valoreTemperaturaTW.setText(temperatureTemp);
            valoreUmiditaTW.setText(humidityTemp);

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isLightSensorAviable){
            sensorManager.registerListener(this, sensoreLuce, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(isLightSensorAviable){
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private class ClasseServer extends Thread
    {
        private BluetoothServerSocket serverSocket;

        public ClasseServer(){
            try {
                checkBTPermission();
                serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            BluetoothSocket socket=null;

            while (socket==null)
            {
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket=serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    socket = null;
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket!=null)
                {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);
                    send =new Send(socket);
                    send.start();

                    break;
                }
            }
        }



    }

    private class Send extends Thread
    {
        private final BluetoothSocket bluetoothSocket;
        private final OutputStream outputStream;

        public Send(BluetoothSocket socket)
        {
            bluetoothSocket=socket;
            OutputStream tempOut=null;

            try {
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();

            }

            outputStream=tempOut;
        }

        public void write(byte[] bytes)
        {

            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                Log.e("TAG", "Error occurred when sending data", e);
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);


            }


        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e("TAG", "Could not close the connect socket", e);
            }
        }



    }

}
