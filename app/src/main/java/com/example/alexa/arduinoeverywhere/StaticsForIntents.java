package com.example.alexa.arduinoeverywhere;

/**
 * Created by alexa on 05/03/2018.
 */

public final class StaticsForIntents {
    public final static String ORDRE_vers_SERVICE = "com.example.alexa.serviceusbtoip.Ordre";

    // liste des messages et de leurs arguments
    // usage : ( cas d'un message avec arguments )
    //          il conviens de n'oublier aucun argument ( les arguments sont décalés )
    //
    //  Intent i = new Intent(MainActivity.this, ServiceUSBToIP.class);
    //  i.putExtra(StaticsForIntents.ORDRE_vers_SERVICE, StaticsForIntents.SendTCPcool);
    //  i.putExtra(StaticsForIntents.MessageTCPaEnvoyer, "<Message cool>=<" +
    //          TextAEnvoyer.getText().toString() + ">");
    //  startService(i);

    public final static int BeginTCP = 1;
        public final static String TCPAddress = "com.example.alexa.serviceusbtoip.TCPAddress";
        public final static String TCPPort = "com.example.alexa.serviceusbtoip.TCPPort";
    public static final int StopTCP = 2;
    public static final int SendTCPcool = 3;
        public final static String MessageTCPaEnvoyer = "com.example.alexa.serviceusbtoip.MessageTCPaEnvoyer";


    public static final int BeginUSB = 10;
    public static final int SendUSB = 11;
        public final static String MessageUSBaEnvoyer = "com.example.alexa.serviceusbtoip.MessageUSBaEnvoyer";
    public static final int StopUSB = 12;
}