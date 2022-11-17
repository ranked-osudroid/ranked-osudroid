package ml.ranked_osudroid.osudroid.socket;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.MessageFormat;

import io.socket.client.IO;
import io.socket.client.Socket;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SocketSession {

    private static Socket socket;

    @Getter
    private static String sessionId;

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
                sessionSync();
                sessionId = socket.id();
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

    private static void sessionSync() {
        if(!isValid()) {
            return;
        }

        socket.emit("session_sync", OnlineManager.getInstance().getUsername());
    }

    public static void disconnect() {
        if(!isValid()) {
            return;
        }
        socket.disconnect();
        socket.close();
    }

    public static void sendStartMap(int mapId) {
        if(!isValid()) {
            return;
        }
        socket.emit("map_start", mapId, System.currentTimeMillis(), Config.getRES_WIDTH(), Config.getRES_HEIGHT());
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
