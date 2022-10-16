package com.example.alexa.arduinoeverywhere;

/*
Ne pas oublier de déclarer le service dans l'AndroidManifest


tré important
pour que ça marche, j'ai ajouté dans le ./app/build.gradle
à la section
"dependencies {"
la ligne
"    compile 'com.github.felHR85:UsbSerial:4.5'"

mais aussi dans le ./build.gradle
à la section :
"allprojects {
    repositories {"
j'ai ajouté la ligne
"        maven { url "https://jitpack.io" }"
==> suite à ces manip dans les gradle il faut fermer puis ouvrir le projet

comme mentionné à https://felhr85.net/2014/11/11/usbserial-a-serial-port-driver-library-for-android-v2-0/

*/

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
//import android.hardware.usb.UsbDevice;
//import android.hardware.usb.UsbDeviceConnection;
//import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.os.AsyncTask;


//import com.felhr.usbserial.UsbSerialDevice;
//import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// pour afficher le contenu des exception
import java.io.StringWriter;
import java.io.PrintWriter;
import android.app.Service;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

// permettre le réveil du téléphone
import android.os.PowerManager;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;
/*
 * Created by alexa on 04/03/2018.
 */

public class ServiceUSBToIP extends Service implements VideoActivity.ForTheService.Callbacks {

    //---------------------------
    // section TCP client
    ConnectTask mConnectTask;
    TcpClient mTcpClient;
    String TCPAdress;
    int TCPPort;

    // section TCP client
    //---------------------------
    int iNbrApplis=0;
    //---------------------------


    public void BeginTCP( String tmpTCPAdress , int tmpTCPPort ) {
        // ces variables serviront plus tard dans
        TCPAdress = tmpTCPAdress;
        TCPPort = tmpTCPPort;
        if (mTcpClient == null) {
            try {
                mConnectTask = new ConnectTask();
                mConnectTask.execute("");

            }catch (Exception e) {
                Log.e("Svc/Begin TCP", "Create mConnectTask", e);
            }
        }
    }

    public void StopTCP () {
        /*new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {*/
                if (mTcpClient != null) {
                    mTcpClient.stopClient();
                    Log.e("Svc/STOP TCP", "stop TCP");
                    mConnectTask.cancel(true);
                    Log.e("Svc/STOP TCP", "stop ConTask");
                    mConnectTask = null;
                    mTcpClient = null;
                } else { Log.e("Svc/STOP TCP", "Pas stop"); }
            /*}
        }, 1);*/
    }

    public void SendMessageToTCP( String sMess ) {
        if (mTcpClient != null) {
            mTcpClient.sendMessage(sMess);
        }
    }

// fonctions de conversion personelles pour mon protocole

    private class TabDeByte {
        private byte[] contenu;
        private int Taille;
    }

    private String StringToHex(String sMessageAReformuler) {
        TabDeByte tMessageAReformuler = new TabDeByte();

        tMessageAReformuler.contenu = sMessageAReformuler.getBytes();
        tMessageAReformuler.Taille = sMessageAReformuler.length();

        return StringToHex(tMessageAReformuler);
    }

    private String StringToHex(TabDeByte tMessageAReformuler) {
        int i;
        String messageReformate = "<message hexa>=";

        for (i = 0; i < tMessageAReformuler.Taille; i++) {

            messageReformate += " " + Integer.toHexString(tMessageAReformuler.contenu[i]);
        }
        return messageReformate;
    }

    private String ByteToMsgHex(byte[] bMessageAReformuler) {
        //int i;
        StringBuilder messageReformate = new StringBuilder("<message hexa>=<");
        String sDoblex;

        for (byte btempDoblex : bMessageAReformuler) {

            if (btempDoblex >= 0) {
                // nombre positif
                if (btempDoblex > 15) {
                    // nombre de 2 digit
                    sDoblex = Integer.toHexString(btempDoblex);
                } else {
                    // là y'a qu'un digit alors on met un 0 devant
                    sDoblex = "0" + Integer.toHexString(btempDoblex);
                }
            } else {
                // pour les nombre négatifs
                sDoblex = Integer.toHexString(256 + (int) btempDoblex);
            }

            messageReformate.append(sDoblex);
        }
        return messageReformate.append(">").toString();
    }

    private TabDeByte MessageHexToTabDeByte(String MessageARecoder) {
        int iPosition, iLongeurMessageARecoder, iDansContenu;
        TabDeByte tMessageHexa = new TabDeByte();
        tMessageHexa.contenu = null;
        tMessageHexa.Taille = 0;
        String sOctet;

        if (MessageARecoder.substring(0, 16).equalsIgnoreCase("<message hexa>=<")) {
            iPosition = 16;  // on saute l'espace
            iLongeurMessageARecoder = MessageARecoder.length();

            tMessageHexa.Taille = (iLongeurMessageARecoder - iPosition) / 2; // pas +1
            tMessageHexa.contenu = new byte[tMessageHexa.Taille];

            iDansContenu = 0;
            do {
                sOctet = MessageARecoder.substring(iPosition, iPosition + 2);
                tMessageHexa.contenu[iDansContenu] = (byte) Integer.parseInt(sOctet, 16);

                iPosition += 2;
                iDansContenu++;

            } while (iPosition < iLongeurMessageARecoder - 1);

        }

        return tMessageHexa;
    }

    private byte[] HexStringToByte ( String HexString ){
        int iPosition=0, iLongeurMessageARecoder = HexString.length();
        byte[] ByteResult = new byte[iLongeurMessageARecoder/2];
        String sOctet;

        while (iPosition < iLongeurMessageARecoder ) {
            sOctet = HexString.substring ( iPosition, iPosition + 2 );
            ByteResult[iPosition/2] = (byte) Integer.parseInt(sOctet, 16);

            iPosition += 2;
        }

        return ByteResult;
    }

    private String DonneesRecues;
    Camera camForFlashLight;

    public void ProcessReception(String Reception)
    {
        int iDebutEtiquette, iDebutContenu, iFinContenu;
        String Etiquette, Contenu;

        // ajoutons ce qu'on viens de recevoir à ce qu'on a déjà.
        // voyons ensuite si on sait le traiter.
        DonneesRecues += Reception;

        iDebutEtiquette = DonneesRecues.indexOf("<");
        iDebutContenu = DonneesRecues.indexOf(">=<");
        if (iDebutContenu >= 0) iFinContenu = DonneesRecues.indexOf(">", iDebutContenu + 3);
        else iFinContenu = -1;
        // si on a pas d'étiquette complette ou de contenu complet.
        // on sort ce sera la prochaine fois
        while ((iDebutEtiquette >= 0) && (iDebutContenu > 0) && (iFinContenu > 0))
        {
            // on a une étiquette
            Etiquette = DonneesRecues.substring(iDebutEtiquette + 1, iDebutContenu );
            Contenu = DonneesRecues.substring(iDebutContenu + 3, iFinContenu );
            //On rétrécis le Contenu de ce qui est traité
            DonneesRecues = DonneesRecues.substring(iFinContenu + 1);

            // Selon l'etiquette on fait un truc
            if (Etiquette.equals("Message cool") )
            {
            }

            // ce message est la réponse à CheckTask{run()}
            if (Etiquette.equals("NBAppli") )
            {
                try {
                    iNbrApplis = Integer.parseInt(Contenu);
                } catch(Exception e) {
                    iNbrApplis=0;
                }
                IsServerResponding=true;  // la preuve que oui
            }

            if (Etiquette.equals("message hexa"))
            {
                // On envoy dans le port série le message
                byte [] body=HexStringToByte (Contenu);
                /* suppression USB
                if (serialPort != null) {
                    serialPort.write(body);
                }
                */
                // On envoy dans le bluetooth le message
                SendDataToBlueTooth(new String(body));
            }

            if (Etiquette.equals("capteurs"))
            {
                // allumage du flash
                if (Contenu.equals("FlashLightON")) {
                    //if ( camForFlashLight == null )
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                        // this is really unlikely, but I suppose it's possible.
                        camForFlashLight = Camera.open();
                        Parameters params = camForFlashLight.getParameters();
                        /*params.setFlashMode(Parameters.FLASH_MODE_ON);
                        camForFlashLight.setParameters(params);*/
                        if(params != null) {
                            List<String> supportedFlashModes = params.getSupportedFlashModes();

                            if(supportedFlashModes != null) {

                                if(supportedFlashModes.contains(Parameters.FLASH_MODE_TORCH)) {
                                    params.setFlashMode( Parameters.FLASH_MODE_TORCH );
                                } else if(supportedFlashModes.contains(Parameters.FLASH_MODE_ON)) {
                                    params.setFlashMode( Parameters.FLASH_MODE_ON );
                                }else camForFlashLight = null;
                                camForFlashLight.setParameters(params);
                                camForFlashLight.startPreview();


                            } else Log.d(TAG, "Camera is null.");
                        }
                    }
                }
                // extinction du flash
                if (Contenu.equals("FlashLightOFF")) {
                    // if ( camForFlashLight == null )
                    camForFlashLight = Camera.open();
                    Parameters params = camForFlashLight.getParameters();
                    params.setFlashMode(Parameters.FLASH_MODE_OFF);
                    camForFlashLight.setParameters(params);
                    camForFlashLight.release();
                }
            }

            if (Etiquette.equals("video"))
            {
                // on peut recevoir 2 requetes : démarrer ou arrèter   request   stop

                if (Contenu.equals("request")) {
                    wakeupScreen();
                    DemarreVideo();
                }

                if (Contenu.equals("stop")) {
                    mVideo.StopVideoEtFermeActivity();
                    wakedownScreen();
                }
            }

            iDebutEtiquette = DonneesRecues.indexOf("<");
            iDebutContenu = DonneesRecues.indexOf(">=<");
            if (iDebutContenu >= 0) iFinContenu = DonneesRecues.indexOf(">", iDebutContenu + 3);
        }
    }

    public class ConnectTask extends AsyncTask<String, String, TcpClient> {

        @Override
        protected TcpClient doInBackground(String... message) {

            //we create a TCPClient object

            Log.e("Svc/ConnectTask/dobgrd", "before new tcp client");
            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            }, new TcpClient.OnSocketConnected() {
                @Override
                public void SocketConnected(boolean Connected) {
                    if (Connected) {

                        mTcpClient.sendMessage("<client description>=<side=arduino_side multicon=Ok>");
                        Log.e("Svc/Begin TCP", "Send Message");

                    }
                    mJobCheck.CheckTaskStart();
                }
            });


            Log.e("Svc/ConnectTask/dobgrd", "new tcp client");
            mTcpClient.SERVER_IP = TCPAdress;
            Log.e("Svc/ConnectTask/dobgrd", "tcp adress");
            mTcpClient.SERVER_PORT = TCPPort;
            Log.e("Svc/ConnectTask/dobgrd", "tcp port");
            mTcpClient.run();
            Log.e("Svc/ConnectTask/dobgrd", "tcp run");

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            // -----------------------------------------------------
            //  Ici on recoit du réseau et on envoie vers USB
            ProcessReception(values[0]);
            /*TabDeByte tMessageDecode = MessageHexToTabDeByte( values[0] );
            //---------------------------
            // section USB send
            if (serialPort != null) {
                serialPort.write(tMessageDecode.contenu);
                //serialPort.write(values[0].getBytes());
            }*/
        }
    }

    //---------------------------------------------------------------------
    // en rapport avec la video
    // permettre le réveil du téléphone
    private PowerManager.WakeLock mWakeLock;
    KeyguardManager.KeyguardLock keyguardLock;

    public void wakeupScreen() {

        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP), "ArduinoEveryWhere:Réveil pour la video");
        }
        mWakeLock.acquire();

        if (keyguardLock==null) {
            KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
            keyguardLock = keyguardManager.newKeyguardLock("TAG");
        }
        keyguardLock.disableKeyguard();

        ContinueLockUnLock=false; // interromp le cycle unlock/lock

        Log.e("WakeUp Manager", "Réveil");
    }
    public void wakedownScreen() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP), "ArduinoEveryWhere:Réveil pour la video");
        }
        if (mWakeLock.isHeld()) mWakeLock.release();

        if (keyguardLock==null) {
            KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
            keyguardLock = keyguardManager.newKeyguardLock("TAG");
        }
        // keyguardLock.reenableKeyguard();
        ContinueLockUnLock=true; // autorise le cycle unlock/lock
        UnLockLock();
        Log.e("WakeUp Manager", "Dodo");
    }

    private boolean ContinueLockUnLock=false;

    private void UnLockLock(){
        Log.e("WakeUp Manager", "unlock");
        if (mWakeLock!=null)  mWakeLock.acquire();
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if (ContinueLockUnLock)LockUnLock();
            }
        }, 1000);
    }

    private void LockUnLock(){
        Log.e("WakeUp Manager", "lock");
        if (mWakeLock!=null)if (mWakeLock.isHeld()) mWakeLock.release();
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                UnLockLock();
            }
        },  1500000);
    }
    // fin fonctions sortir de veille
    //---------------------------modif service

    /******
     *  crée une connection avec VideoActivity
     */
    Intent VideoIntent;
    Intent ServiceVideoIntent;
    VideoActivity.ForTheService mVideo;

    private void DemarreVideo() {
        // démarre l'acivitée
        VideoIntent= new Intent(ServiceUSBToIP.this, VideoActivity.class);
        VideoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(VideoIntent);

        ServiceVideoIntent= new Intent(ServiceUSBToIP.this,
                    VideoActivity.ForTheService.class);
        bindService(ServiceVideoIntent,mConnectionVideo,Context.BIND_AUTO_CREATE);

        // il ne faut pas le faire de suite mais peut être qu'on peut faire mieux
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                mVideo.StartVideo();
            }
        }, 1000);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnectionVideo = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            VideoActivity.ForTheService.LocalBinder binder =
                        (VideoActivity.ForTheService.LocalBinder) service;
            mVideo = binder.getService(); //Get instance of your service!
            mVideo.registerClient(ServiceUSBToIP.this); //Activity register in the service as client for callabcks!
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)  {
            //mBound = false;
        }
    };
    //callbacks interface pour dialoguer avec
    public void FlushStarted(String flush){
        String tFlush =  flush.substring(0,flush.length()-8) ;
        SendMessageToTCP("<video>=<flush=" + tFlush +">");
    }

    // fin de la liaison avec le service de VideoActivity
    //--------------------------------------------
    // Pour le BlueTooth
    //      Bluetooth Variables
    private final static String TAG = DeviceControlActivity.class.getSimpleName(); // conflit possible
    private SharedPreferences settings=null;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mBlueToothConnected = false;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";


    public void StartBlueTooth() {

        //--------------------------------------------------
        //      Bluetooth
        // Récupère l'@MAC bluetooth
        mDeviceAddress = settings.getString("BlueMacAdress", "undefined");
        // active le service  seulement si mDeviceAddress est défini
        if ( mDeviceAddress != "undefined") {
            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    public void StopBlueTooth() {
        mBluetoothLeService.disconnect();
        mBluetoothLeService = null;
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
    }

    public void BeforeChooseDeviceBlueTooth() {
        // Avant d'ouvrir l'activity de settings . on déconnecte le bluetooth
        if (mBluetoothLeService != null) mBluetoothLeService.disconnect();
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mBlueToothConnected = true;
                mJobCheck.CheckTaskStart();
                // updateConnectionState(R.string.connected);
                // invalidateOptionsMenu();  ici on peut générer un évent pour signaler connecté
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mBlueToothConnected = false;
                mJobCheck.CheckTaskStart();
                // updateConnectionState(R.string.disconnected);
                // invalidateOptionsMenu();  ici on peut générer un évent pour signaler déconnecté
                //  clearUI();    Typiquement j'en veux pas
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                RecupereHM10(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                // gestion de la réception  de données
                // displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                // on envoie sur le réseau ce qui viens du bluetooth
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(ByteToMsgHex(intent.getStringExtra(BluetoothLeService.EXTRA_DATA).getBytes()));
                }
            }
        }
    };

    private void RecupereHM10(List<BluetoothGattService> gattServices) {

        UUID UUID_HM_10 =
                UUID.fromString(SampleGattAttributes.HM_10);

        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

                //Check if it is "HM_10"
                if(uuid.equals(SampleGattAttributes.HM_10)){
                    bluetoothGattCharacteristicHM_10 = gattService.getCharacteristic(UUID_HM_10);


                    if (mGattCharacteristics != null) {
                        final int charaProp = gattCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(gattCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = gattCharacteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    gattCharacteristic, true);

                            Toast.makeText(this, "HM10 trouvé", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void SendDataToBlueTooth(String tempsString) {
        String TwentyString;

        Log.d(TAG, "Send debut " + tempsString );
        while (tempsString.length() > 0) {
            byte[] txBytes;
            if (tempsString.length() > 20) {
                txBytes = new byte[20];
                TwentyString = tempsString.substring(0,20); // 20 premier
                tempsString = tempsString.substring(20);    // coupe les 20 premiers
            }
            else {
                txBytes = new byte[tempsString.length()];
                TwentyString = tempsString;
                tempsString="";
            }

            try {
                txBytes = TwentyString.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (bluetoothGattCharacteristicHM_10 != null) {
                bluetoothGattCharacteristicHM_10.setValue(txBytes);
                mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
                mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristicHM_10, true);
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException  e) {
                e.printStackTrace();
            }

            //Log.d(TAG, "Send fin " + tempsString );
        }
    }


    // fin du BlueTooth
    //--------------------------------------------
    // Binder given to clients
    Callbacks activity;
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Do what you need in onStartCommand when service has been started
        //---------------------------
        // ici ce qui était avant dans le OnCreate

        mJobCheck.CheckTaskStart();

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                BeginTCP(settings.getString("pref_AdressIP", "192.168.1.20")
                        ,Integer.parseInt( settings.getString("pref_Port", "13000")));
            }
        }, 50);

        return START_NOT_STICKY;
    }

    public void onDestroy () {
        mJobCheck.CheckTaskDestroy();
        StopBlueTooth();

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                StopTCP();
            }
        }, 1);

        System.exit(0);
    }
    // ---------------------------------------------------
    // ici je créer un timer qui va faire des opérations sur le
    // fichier video
    JobCheck mJobCheck = new JobCheck();
    boolean IsServerResponding=false, IsRessetingSocket=false;

    class JobCheck {
        Timer timer = new Timer();
        TimerTask mCheckTask = new CheckTask();

        int bimCombien;
        long duree;

        class CheckTask extends TimerTask {

            public void run() {
                boolean bSocketConnected;
                /* try {
                    bSocketConnected = mTcpClient.socket.isConnected();
                } catch (Exception e) { bSocketConnected=false; }*/

                if(activity!=null) {   // bSocketConnected
                    activity.montremoiTonTimer( bimCombien , IsServerResponding
                            , mBlueToothConnected, iNbrApplis);
                }

                // Gestion du reset de connection
                try {
                    bSocketConnected = mTcpClient.socket.isConnected();
                } catch (Exception e) { bSocketConnected=false; }

                if (!IsServerResponding){

                    // si not responding alors que connected => reset de connection
                    if ( bSocketConnected ){
                        // s'il ne répond pas alors qu'il est connecté il faut reseter
                        if (IsRessetingSocket) StopTCP ();
                        duree = 1000;
                        IsRessetingSocket = true;
                    } else {
                        // s'il ne répond pas, qu'il n'est pas connecté et qu'on l'a reseté
                        // on redémarre
                        if ( IsRessetingSocket ) {
                            BeginTCP(settings.getString("pref_AdressIP", "192.168.1.20")
                                    ,Integer.parseInt( settings.getString("pref_Port", "13000")));
                            IsRessetingSocket = false;
                            duree = 1000;
                        } else if (duree<30000)duree+=100;
                        // s'il ne répond pas, qu'il n'est pas connecté et qu'on n'a pas lancé de reset
                        // On ne fait rien.
                    }
                }
                else // on stabilise la durée si on atteind 30sec
                    if (duree<30000)duree+=100;

                // -------------------------------------------------
                // ping du serveur  et statut BlueTooth
                IsServerResponding = false;  // on verra si le serveur répond
                if (mBlueToothConnected) {
                    SendMessageToTCP("<BTstat>=<OK>");
                } else {
                    SendMessageToTCP("<BTstat>=<NOK>");
                }

                ChangeFrequence();
                bimCombien++;
            }
        }

        void ChangeFrequence(  ) {
            timer.cancel();
            mCheckTask.cancel();
            timer = new Timer();
            mCheckTask = new CheckTask();
            timer.scheduleAtFixedRate(mCheckTask, duree, duree);
        }

        void CheckTaskStart() {
            duree=1000;
            ChangeFrequence();
            bimCombien=0;
        }

        void CheckTaskDestroy() {
            timer.cancel();
            mCheckTask.cancel();
        }

    }

    private final IBinder mBinder = new LocalBinder();

    class LocalBinder extends Binder {
        ServiceUSBToIP getService() {
            // Return this instance of LocalService so clients can call public methods
            return ServiceUSBToIP.this;
        }
    }

    //Here Activity register to the service as Callbacks client
    public void registerClient(Activity activity){
        this.activity = (Callbacks)activity;

        if(this.activity!=null){
            // USB   this.activity.setUiEnabled(isSerialPortOpen);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** method for clients */
    //callbacks interface for communication with service clients!
    public interface Callbacks{
        void montremoiTonTimer (int bimboum, boolean ConnecteTcp, boolean connectBlueTooth, int NombreDappli);
    }
}
