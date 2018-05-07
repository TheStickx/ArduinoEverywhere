package com.example.alexa.arduinoeverywhere;

/*
Ne pas oublier de déclarer le service dans l'AndroidManifest
 */

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.IBinder;
import android.content.Context;

public class MainActivity extends AppCompatActivity implements ServiceUSBToIP.Callbacks {

    // USB
    Button USBstartButton, USBsendButton, USBclearButton, USBstopButton;
    EditText USBTextAEnvoyer;
    TextView USBTextRecu;
    // TCP
    EditText TCPTextAEnvoyer;
    EditText EditIPAddress;
    EditText EditIPPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //---------------------------
        // section TCP client
        TCPTextAEnvoyer = findViewById(R.id.TCPTextAEnvoyer);
        EditIPAddress = findViewById(R.id.editIP );
        EditIPPort = findViewById(R.id.editPort );
        // section TCP client
        //---------------------------
        // section USB
        USBstartButton = findViewById(R.id.ButtonBegin);
        USBsendButton = findViewById(R.id.ButtonSend);
        USBclearButton = findViewById(R.id.ButtonClear);
        USBstopButton = findViewById(R.id.ButtonStop);
        USBTextAEnvoyer = findViewById(R.id.EditTextAEnvoyer); //editText = (EditText) findViewById(R.id.EditTextAEnvoyer);
        USBTextRecu = findViewById(R.id.TextRecu);
        // section USB
        //---------------------------
        // section binding avec callback
        serviceIntent = new Intent(MainActivity.this, ServiceUSBToIP.class);
        startService(serviceIntent);  // cela semble nécessaire sinon le service ferme avec l'activity
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE); //Binding to the service!
        //--------------------------- startService bindService
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        }); }


    //-------------------------------------------------------
    // traitement des boutons
    // Les boutons TCP
    public void onClickBeginTCP(View View) {//new ConnectTask().execute("");
        mService.BeginTCP(EditIPAddress.getText().toString()
                ,Integer.parseInt( EditIPPort.getText().toString()));

    }

    public void onClickStop(View View) {
        mService.StopTCP();
    }

    public void onClickSend(View View) {
        mService.SendMessageToTCP("<Message cool>=<" +
                TCPTextAEnvoyer.getText().toString() + ">");
    }

    // Les boutons USB
    public void onClickBegin(View view) {
        mService.ClickBegin();
    }

    public void onClickToSend(View view) {
        String MessageUSB = USBTextAEnvoyer.getText().toString();
        mService.SendMessageToUSB( MessageUSB );
        tvAppend(USBTextRecu, "\nData Sent : " + MessageUSB + "\n");
    }

    public void onClickCToClear(View view) {
        USBTextRecu.setText(" ");
    }

    public void onClickToStop(View view) {
        mService.ClickToStopUSB();
        setUiEnabled(false);
        tvAppend(USBTextRecu,"\nSerial Connection Closed! \n");
    }


    //-------------------------------------------------------
    // binding

    Intent serviceIntent;  // initié dans OnCreate
    ServiceUSBToIP mService;
    boolean mBound = false;

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, ServiceUSBToIP.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        mBound = false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ServiceUSBToIP.LocalBinder binder = (ServiceUSBToIP.LocalBinder) service;
            mService = binder.getService(); //Get instance of your service!
            mService.registerClient(MainActivity.this); //Activity register in the service as client for callabcks!
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    //---------------------------modif service mis en callback
    public void setUiEnabled(boolean bool) {
        USBstartButton.setEnabled(!bool);
        USBsendButton.setEnabled(bool);
        USBstopButton.setEnabled(bool);
        USBTextAEnvoyer.setEnabled(bool);
    }
    //---------------------------modif service

}

/* Le 15/03/2018 vers 19h ce programme fonctionnais.
 * à 23h00 une nouvelle version avec retrait des commentaires qui n'étaient utiles que le temps
 * de la migration */