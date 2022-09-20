package ml.ranked_osudroid.osudroid.socket;

import android.util.Log;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketSession {

    private static Socket socket;

    public static boolean isValid() {
        return socket != null && socket.isActive();
    }

    public static void connect() {
        if(isValid()) {
            return;
        }
        try {
            socket = IO.socket("http://192.168.0.7:8080");

            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d("socket", "Connected with socket server.");
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d("socket", "Disconnect with socket server.");
            });

            socket.connect();
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        if(!isValid()) {
            return;
        }
        socket.disconnect();
        socket.close();
    }

    public static void sendPressCursor(int i, float x, float y) {
        if(!isValid()) {
            return;
        }
        socket.emit(CursorUpdateType.PRESS.getAsEventName(), System.currentTimeMillis(), i, x, y);
    }

    public static void sendReleaseCursor(int i) {
        if(!isValid()) {
            return;
        }
        socket.emit(CursorUpdateType.RELEASE.getAsEventName(), System.currentTimeMillis(), i);
    }

    public static void sendMoveCursor(int i, float x, float y) {
        if(!isValid()) {
            return;
        }
        socket.emit(CursorUpdateType.MOVE.getAsEventName(), System.currentTimeMillis(), i, x, y);
    }
}
