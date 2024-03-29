package com.example.alexa.arduinoeverywhere; /*
 * Created by alexa on 04/03/2018.
 */
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by alexa on 04/02/2018.
 */

public class TcpClient {

    private static final String TAG = DeviceControlActivity.class.getSimpleName();
    //public static final String SERVER_IP = "192.168.1.20"; //server IP address  192.168.1.20
    //public static final int SERVER_PORT = 13000;
    public String SERVER_IP = "192.168.1.20"; //server IP address  192.168.1.20
    public int SERVER_PORT = 13000;
    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private OnMessageReceived mMessageListener;
    // Inform That Sochet is connected
    private OnSocketConnected mSocketConnected;
    // while this is true, the server will continue running
    private boolean mRun = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;
    public Socket socket;

    /**
     * Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TcpClient(OnMessageReceived listener, OnSocketConnected listenSocket) {
        mMessageListener = listener;
        mSocketConnected = listenSocket;
    }

    /**
     * Sends the message entered by client to the server
     *
     * @param message text entered by client
     */


    public void sendMessage(String message) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (mBufferOut != null ) { // && !mBufferOut.checkError()) {
            if (!mBufferOut.checkError()) {
                mBufferOut.println(message);
                mBufferOut.flush();
            }
        }
    }

    /**
     * Close the connection and release the members
     */
    public void stopClient() {

        mRun = false;

        if (mBufferOut != null) {

            mBufferOut.flush();
            mBufferOut.close();
        }

        if (mBufferIn != null) {
            try {
                socket.shutdownInput();
                /*InetAddress serverAddr = InetAddress.getByName(SERVER_IP);


                //create a socket to make the connection with the server
                socket = new Socket(serverAddr, SERVER_PORT);*/

                mBufferIn.close();
            } catch (Exception e) { Log.e("TCP Client/stopClient", "fermeture mBufferIn", e);}
        }

        try {socket.close();} catch (Exception e) { Log.e("TCP Client/stopClient", "fermeture socket", e);}
        Log.e("TCP Client/stopClient", "C:Point 14");

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;

    }

    public void run() {

        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVER_IP);


            //create a socket to make the connection with the server
            socket = new Socket(serverAddr, SERVER_PORT);


            try {
                //sends the message to the server
                mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                //receives the message which the server sends back
                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // informe le service que le socket est connecté
                mSocketConnected.SocketConnected(socket.isConnected());

                //in this while the client listens for the messages sent by the server
                while (mRun) {

                    // Il est clair que cette fonction s'arrète jusqu'à qu'on recoive un message
                    try {
                        mServerMessage = mBufferIn.readLine();
                    }catch (Exception e) {
                        Log.e("TCP Client/Run", "Run", e);
                        mServerMessage = null;
                        mRun = false;
                    }

                    if ( mBufferIn == null )mRun = false;

                    if (mServerMessage != null && mMessageListener != null) {

                        //call the method messageReceived from MyActivity class
                        Log.d(TAG, "Send debut avant publish" + mServerMessage );
                        mMessageListener.messageReceived(mServerMessage);
                    }

                }

                Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");

            } catch (Exception e) {

                Log.e("TCP Client/Run", "S: Error", e);

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.

                socket.close();
            }

        } catch (Exception e) {

            Log.e("TCP Client/Run", "C: Socket Error", e);

        }

    }

    //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
    //class at on asynckTask doInBackground
    public interface OnMessageReceived {
        void messageReceived(String message);
    }
    public interface OnSocketConnected {
        void SocketConnected(boolean Connected);
    }

}
