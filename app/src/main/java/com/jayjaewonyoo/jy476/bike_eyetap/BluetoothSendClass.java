package com.jayjaewonyoo.jy476.bike_eyetap;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothSendClass {
    private static final String TAG = "BLUETOOTHSERVICE";
    private static final String appName = "OPENEYETAP";
    private static final UUID OET_UUID = UUID.fromString("c9209064-3be0-40ff-868d-d8704abc7984");

    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    // Threads
    //private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    Context mContext;

    public BluetoothSendClass(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        start();
    }

    /*
    private class AcceptThread extends Thread {
        private BluetoothServerSocket mServerSocket;
        // Server socket
        public AcceptThread() {
            Log.d(TAG, "Starting AcceptThread");
            BluetoothServerSocket tempSocket = null;
            try {
                tempSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(appName, OET_UUID);
                Log.d(TAG,"Server socket has been set up");
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            mServerSocket = tempSocket;
        }
        public void run() {
            BluetoothSocket socket = null;
            try {
                socket = mServerSocket.accept();
                Log.d(TAG,"Connection accepted");
            } catch (IOException e) {
                Log.d(TAG, "Connection failed");
            }
            if(socket != null) {
                // TODO
                Log.d(TAG, "Conntected...");
                connected(socket, mDevice);
            }
        }
        public void cancel() {
            Log.d(TAG, "Cancelling AcceptThread");
            try {
                mServerSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    */

    private class ConnectThread extends Thread {
        private final BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "Starting ConnectThread");

            mDevice = device;
            deviceUUID = uuid;

            BluetoothSocket tmp = null;

            try {
                Log.d(TAG, "tmp Established.");
                tmp = mDevice.createRfcommSocketToServiceRecord(mDevice.getUuids()[0].getUuid());
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            mSocket = tmp;
            Log.d(TAG, deviceUUID.toString());
        }

        public void run() {
            Log.d(TAG, "Running Connect Thread");

            mBluetoothAdapter.cancelDiscovery();

            // TODO return to scan activity if connection fails
            try {
                mSocket.connect(); // Issue here?
                Log.d(TAG, "Connection established");
            } catch (IOException e) {

                //mSocket.close();
                cancel();

                e.printStackTrace();
                Log.d(TAG, "Connection failed");
                return;
            }

            connected(mSocket, mDevice);
        }

        public void cancel() {
            Log.d(TAG, "Cancelling ConnectThread");

            try {
                mSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "Starting Connected Thread");

            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            mProgressDialog.dismiss();

            try {
                Log.d(TAG, "Sockets established.");
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "Running Connected Thread");
            byte[] buffer = new byte[1024];

            int bytes;

            while(true) {
                // read from stream
                try {
                    bytes = mInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void cancel() {
            Log.d(TAG, "Cancelling ConnectedThread");
            try {
                mSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void write(byte[] bytes) {
            // legacy JSON write
            /*
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "Writing to OutputStream: " + text);
            try {
                mOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            */
            Log.d(TAG, "Writing " + bytes.length + " bytes to OutputStream");
            try {
                mOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void start() {
        Log.d(TAG, "Start");

        if (mConnectThread != null) {
            Log.d(TAG, "ConnectThread exists, terminating");
            mConnectThread.cancel();
            mConnectThread = null;
        }

        /*
        if(mAcceptThread == null) {
            Log.d(TAG, "ConnectThread doesn't exist, starting");
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }*/
    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "Start Client");

        //mProgressDialog = ProgressDialog.show(mContext, "Bluetooth", "Bluetooth is connecting", true);
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice mDevice) {
        Log.d(TAG, "Starting connected");
        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    public void closeConnections() {
        Log.d(TAG, "Closing connection...");
        if(mConnectThread != null) {
            Log.d(TAG, "Closing connect thread");
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if(mConnectedThread != null) {
            Log.d(TAG, "Closing connected thread");
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    public void write(byte[] bytes) {

        mConnectedThread.write(bytes);
    }

    // header: JSON object, 128 bytes max
    // {
    //  "type": String  LENGTH: 8 chars max
    //  "bytes": int    SIZE: 8 bytes
    //  if type == "file":
    //  "name": String 8 chars
    //  "ext": String   LENGTH: 8 chars max
    // }

    public void writeJSON(byte[] bytes, String type) {
        Log.d(TAG, "Received request to send json");

        // create header
        JSONObject header = new JSONObject();
        try {
            header.put("type", "notif");
            header.put("bytes", bytes.length);
        } catch (JSONException e) {
            Log.d(TAG, "Failed to package JSON object");
        }
        Log.d(TAG, "Packaged JSON header");

        byte[] headerTextBytes = (header.toString() + "\r\n").getBytes(Charset.defaultCharset());

        byte[] byteMessage = formMessage(headerTextBytes, bytes);

        mConnectedThread.write(byteMessage);

        // Log.d(TAG, "Header sent");
        // Log.d(TAG, "JSON sent");

        // write file bytes
        // mConnectedThread.write(bytes);
        //Log.d(TAG, "Data sent");
    }

    public void writeFile(byte[] bytes, String name, String ext) {
        Log.d(TAG, "Received request to send file");

        if(bytes != null) {
            // check size of name and ext
            name = name.substring(0, Math.min(8, name.length()));
            ext = ext.substring(0, Math.min(8, ext.length()));
            // create header
            JSONObject header = new JSONObject();
            try {
                header.put("type", "file");
                header.put("bytes", bytes.length);
                header.put("name", name);
                header.put("ext", ext);
            } catch (JSONException e) {
                Log.d(TAG, "Failed to package JSON object");
            }
            Log.d(TAG, "Packaged JSON header");

            byte[] headerTextBytes = header.toString().getBytes(Charset.defaultCharset());

            byte[] byteMessage = formMessage(headerTextBytes, bytes);

            mConnectedThread.write(byteMessage);
        }
        else {
            Log.d(TAG, "File is empty.");
        }

    }

    private byte[] formMessage(byte[] headerText, byte[] message) {
        byte[] fullMessage = new byte[128 + message.length];
        System.arraycopy(headerText, 0, fullMessage, 0, headerText.length);
        System.arraycopy(message, 0, fullMessage, 128, message.length);
        return fullMessage;
    }
}
