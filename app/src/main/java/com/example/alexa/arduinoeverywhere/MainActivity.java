package com.example.alexa.arduinoeverywhere;

/*
Ne pas oublier de déclarer le service dans l'AndroidManifest
 */

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.IBinder;
import android.content.Context;
import android.widget.Toast;
import android.os.Handler;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ServiceUSBToIP.Callbacks {

    TextView USBTextRecu;
    TextView mTextNbrAppli;

    // pour maniper les prefs enregistrées
    private SharedPreferences settings=null;

    // Les boutons image
    private ImageButton mBtnServer = null;
    private ImageButton mBtnBlue = null;
    private ImageView   ImgAppli;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnServer = (ImageButton) findViewById(R.id.imgBserver);
        mBtnBlue = (ImageButton) findViewById(R.id.imgBbluetooth);
        ImgAppli = (ImageView)findViewById(R.id.imgVApplis);
        mTextNbrAppli = findViewById(R.id.textNbrAppli);
        //---------------------------
        // section TCP client
        // section TCP client
        //---------------------------
        // section USB
        USBTextRecu = findViewById(R.id.TextRecu);
        // section USB
        //---------------------------
        // section binding avec callback
        //DemarageDuService();
        //--------------------------- startService bindService
        // gestion des preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        if ( serviceIntent==null ) {
            serviceIntent = new Intent(MainActivity.this, ServiceUSBToIP.class);
            startService(serviceIntent);  // cela semble nécessaire sinon le service ferme avec l'activity
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE); //Binding to the service!
        }
        else Toast.makeText(this.getApplicationContext(), "service already running" , Toast.LENGTH_SHORT ).show();
    }

    //-------------------------------------------------------
    // traitement des boutons
    // Les boutons TCP
    public void onClickBeginTCP(View View) {//new ConnectTask().execute("");
/*
        mService.BeginTCP(settings.getString("pref_AdressIP", "192.168.1.20")
                ,Integer.parseInt( settings.getString("pref_Port", "13000")));*/
    }

    public void onClickStop(View View) {
        /*mService.StopTCP();*/
    }

    public void onLaunchSettingNetworks(View view){
        // ouvre l'activity de settings networks
        Intent intent;
        intent = new Intent(this,SettingNetwork.class);
        startActivityForResult(intent, 0);
    }

    // bouton sur l'interface
    public void chooseDevice(View view) {
        // ouvre l'activity de settings mais avant on déconnecte le bluetooth

        mService.BeforeChooseDeviceBlueTooth();

        Intent intent;
        intent = new Intent(this, DeviceScanActivity.class);
        startActivity(intent);

    }


    //-------------------------------------------------------
    // binding

    Intent serviceIntent;  // initié dans OnCreate
    ServiceUSBToIP mService;
    boolean mBound = false;

    public void onStartService(View view){
        /*if ( serviceIntent==null ) {
            serviceIntent = new Intent(MainActivity.this, ServiceUSBToIP.class);
            startService(serviceIntent);  // cela semble nécessaire sinon le service ferme avec l'activity
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE); //Binding to the service!
        }
        else Toast.makeText(this.getApplicationContext(), "service already running" , Toast.LENGTH_SHORT ).show();*/
    }

    public void onTestService(View view){
        if ( serviceIntent == null ){
            Toast.makeText(this.getApplicationContext(), "service not running" , Toast.LENGTH_SHORT ).show();
        }
        else Toast.makeText(this.getApplicationContext(), "service running" , Toast.LENGTH_SHORT ).show();
    }

    public void onStopService(View view){
        //try {
            if ( serviceIntent!=null ) {
                unbindService(mConnection);
                stopService(serviceIntent);
                mService.stopSelf();
                serviceIntent=null;
            }
        //}  catch (Exception e){tvAppend(USBTextRecu,e.printStackTrace());}
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

            mService.StartBlueTooth();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            mBound = false;
        }
    };

    @Override
    //---------------------------modif service mis en callback

    public void montremoiTonTimer (int bimboum, boolean ConnecteTcp, boolean connectBlueTooth, int NombreDappli) {
        final int fBimboum = bimboum, fNombreDappli = NombreDappli;
        final boolean fConnecteTcp = ConnecteTcp, fconnectBlueTooth = connectBlueTooth;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String sDonneEtat;

                if (fConnecteTcp) {
                    sDonneEtat = "Server connecté | ";
                    mBtnServer.setImageResource(R.drawable.serveur_on);
                } else {
                    sDonneEtat = "Server non connecté | ";
                    mBtnServer.setImageResource(R.drawable.serveur_off);
                }

                if (fconnectBlueTooth) {
                    sDonneEtat += "BlueTooth connecté | ";
                    mBtnBlue.setImageResource(R.drawable.bluetooth_on);
                } else {
                    sDonneEtat += "BlueTooth non connecté | ";
                    mBtnBlue.setImageResource(R.drawable.bluetooth_off);
                }

                sDonneEtat +="Nombre d'appli:" + fNombreDappli;
                if ( fNombreDappli == 0 ) {
                    ImgAppli.setImageResource(R.drawable.appli_off);
                } else {
                    ImgAppli.setImageResource(R.drawable.appli_on);
                    if ( fNombreDappli > 1  ) {
                        mTextNbrAppli.setText( String.valueOf(fNombreDappli) );
                    }
                }

                /* USBTextRecu.setText( String.format(Locale.FRANCE, "%s %d\r\n%s"
                        , getResources().getString(R.string.message_numero)
                        , fBimboum
                        , sDonneEtat) ); */
            }
        } );
    }
}

/* Le 15/03/2018 vers 19h ce programme fonctionnais.
 * à 23h00 une nouvelle version avec retrait des commentaires qui n'étaient utiles que le temps
 * de la migration */