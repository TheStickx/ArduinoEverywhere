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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.os.AsyncTask;


import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

// pour afficher le contenu des exception
import java.io.StringWriter;
import java.io.PrintWriter;
import android.app.Service;
import java.io.*;

// permettre le réveil du téléphone
import android.os.PowerManager;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.os.Handler;
import android.widget.Toast;
/*
 * Created by alexa on 04/03/2018.
 */

public class ServiceUSBToIP extends Service implements VideoActivity.ForTheService.Callbacks {

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    boolean isSerialPortOpen;

    //---------------------------
    // section TCP client
    TcpClient mTcpClient;
    String TCPAdress;
    int TCPPort;

    // section TCP client
    //---------------------------

    //---------------------------

    UsbSerialInterface.UsbReadCallback mCallback;

    {
        mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
            @Override
            public void onReceivedData(byte[] arg0) {
                String data = null;

                try {
                    //---------------------------
                    // section TCP client
                    // ici on recoit de l'USB et on envoie vers le réseau
                    if (mTcpClient != null) {

                        if (arg0.length!=0) mTcpClient.sendMessage(ByteToMsgHex(arg0));

                    } else {
                        data = new String(arg0, "UTF-8");

                    }
                } catch (UnsupportedEncodingException e) {

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    String sStackTrace = sw.toString();
                    Log.d("USB", "Err UsbRead : " + sStackTrace);
                }
            }
        };
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            isSerialPortOpen=true;
                            if(activity!=null){
                                activity.setUiEnabled(true); // modif service mis en call back
                            }
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);

                            Log.d("USB", "Serial Connection Opened!");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                ClickBegin ();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                ClickToStopUSB();

            }
        }


    };

    //---------------------------modif service
    public void ClickBegin ()  {
    //---------------------------modif service
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }

    }


    public void BeginTCP( String tmpTCPAdress , int tmpTCPPort ) {
        // ces variables serviront plus tard dans
        TCPAdress = tmpTCPAdress;
        TCPPort = tmpTCPPort;
        if (mTcpClient == null) {
            new ConnectTask().execute("");

        } else {
            mTcpClient.stopClient();
            mTcpClient = null;
            new ConnectTask().execute("");
        }

        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTcpClient.sendMessage("<client description>=<side=arduino_side multicon=Nok>");
            }
        });*/

        while (mTcpClient == null) {
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mTcpClient.sendMessage("<client description>=<side=arduino_side multicon=Nok>");
    }

    public void StopTCP () {
        if (mTcpClient != null) {
            mTcpClient.stopClient();
            mTcpClient = null;
        }
    }

    public void SendMessageToTCP( String sMess ) {
        if (mTcpClient != null) {
            mTcpClient.sendMessage(sMess);
        }
    }

    public void SendMessageToUSB( String sMess ) {
        serialPort.write(sMess.getBytes());
    }

    public void ClickToStopUSB() {
        serialPort.close();
        isSerialPortOpen=false;
        if (activity != null) {
            activity.setUiEnabled(false);
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

            if (Etiquette.equals("message hexa"))
            {
                // On envoy dans le port série le message
                byte [] body=HexStringToByte (Contenu);
                if (serialPort != null) {
                    serialPort.write(body);
                }
            }

            if (Etiquette.equals("capteurs"))
            {
                // pas encore définis
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
            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });


            mTcpClient.SERVER_IP = TCPAdress;
            mTcpClient.SERVER_PORT = TCPPort;
            mTcpClient.run();

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
                    PowerManager.ACQUIRE_CAUSES_WAKEUP), "Réveil pour la video");
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
                    PowerManager.ACQUIRE_CAUSES_WAKEUP), "Réveil pour la video");
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
    public void TimeOutVideo(){
        //  pour l'instant rien de défini
        Toast.makeText( this ,"relaunch!", Toast.LENGTH_LONG).show();
        //SendMessageToTCP("<Message cool>=<timeout video>");
        mVideo.StartVideo();
    }
    public void FlushStarted(String flush){
        String tFlush =  flush.substring(0,flush.length()-8) ;
        SendMessageToTCP("<video>=<flush=" + tFlush +">");
    }

    // fin de la liaison avec le service de VideoActivity
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
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        return START_NOT_STICKY;
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
            this.activity.setUiEnabled(isSerialPortOpen);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** method for clients */
    //callbacks interface for communication with service clients!
    public interface Callbacks{
        void setUiEnabled(boolean bool);
    }
}
