package ml.ranked_osudroid.osudroid.socket;

import android.util.Log;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
            socket = IO.socket(BuildConfig.SOCKET_URL);

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

    // TODO : 시작, 끝 할때 socket emit 처리 하기!
    public static void sendStartMap(int mapId) {
        if(!isValid()) {
            return;
        }
        socket.emit("map_start", mapId, System.currentTimeMillis());
    }

    public static void sendStopMap(int mapId) {
        if(!isValid()) {
            return;
        }
        socket.emit("map_stop", mapId, System.currentTimeMillis());
    }

    public static void sendPressCursor(int mapId, float secPressed, int i, float x, float y) {
        if(!isValid()) {
            return;
        }
        socket.emit(CursorUpdateType.PRESS.getAsEventName(), mapId, secPressed, i, x, y);
    }

    public static void sendReleaseCursor(int mapId, float secPressed, int i) {
        if(!isValid()) {
            return;
        }
        socket.emit(CursorUpdateType.RELEASE.getAsEventName(), mapId, secPressed, i);
    }

    public static void sendMoveCursor(int mapId, float secPressed, int i, float x, float y) {
        if(!isValid()) {
            return;
        }
        socket.emit(CursorUpdateType.MOVE.getAsEventName(), mapId, secPressed, i, x, y);
    }
}
