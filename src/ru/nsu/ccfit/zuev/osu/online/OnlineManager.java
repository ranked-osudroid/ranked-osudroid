package ru.nsu.ccfit.zuev.osu.online;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.anddev.andengine.util.Base64;
import org.anddev.andengine.util.Debug;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;

import ml.ranked_osudroid.osudroid.CodeMessages;
import okhttp3.OkHttpClient;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calcuator;
import ru.nsu.ccfit.zuev.osu.online.PostBuilder.RequestException;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

public class OnlineManager {
    public static final String hostname = "osudroid.moe";
    public static final String endpoint = "https://" + hostname + "/api/";
    private static final String onlineVersion = "29";

    public static final OkHttpClient client = new OkHttpClient();

    private static OnlineManager instance = null;
    private Context context;
    private String failMessage = "";

    private boolean stayOnline = true;
    private String ssid = "";
    private String userId = "";
    private String playID = "";

    private String username = "";
    private String password = "";
    private String deviceID = "";
    private long rank = 0;
    private long score = 0;
    private float accuracy = 0;
    private String avatarURL = "";
    private int mapRank;
    private int replayID = 0;

    private boolean isMappooler = false;
    private boolean isStaff = false;

    public static OnlineManager getInstance() {
        if (instance == null) {
            instance = new OnlineManager();
        }
        return instance;
    }

    public static String getReplayURL(int playID) {
        return endpoint + "upload/" + playID + ".odr";
    }

    public void Init(Context context) {
        this.stayOnline = Config.isStayOnline();
        this.username = Config.getOnlineUsername();
        this.password = Config.getOnlinePassword();
        this.deviceID = Config.getOnlineDeviceID();
        this.context = context;
    }

    private String getSecuredString(String... strings) {
        StringBuilder sb = new StringBuilder();

        for(String string : strings) {
            sb.append(string);
        }

        String first = new String(Base64.decode(BuildConfig.SECURE_1, Base64.DEFAULT));
        String second = new String(Base64.decode(BuildConfig.SECURE_2, Base64.DEFAULT));
        return MD5Calcuator.getStringMD5(first + sb.toString() + second);
    }

    private ArrayList<String> sendRequest(PostBuilder post, String url) throws OnlineManagerException {
        ArrayList<String> response;
        try {
            response = post.requestWithAttempts(url, 1);
        } catch (RequestException e) {
            Debug.e(e.getMessage(), e);
            failMessage = "Cannot connect to server";
            throw new OnlineManagerException("Cannot connect to server", e);
        }
        failMessage = "";

        //TODO debug code
		/*Debug.i("Received " + response.size() + " lines");
		for(String str: response)
		{
			Debug.i(str);
		}*/

        if (response.size() == 0 || response.get(0).length() == 0) {
            failMessage = "Got empty response";
            Debug.i("Received empty response!");
            return null;
        }

        return response;
    }

    public boolean logIn() throws OnlineManagerException {
        return logIn(username);
    }

    public synchronized boolean logIn(String token) throws OnlineManagerException {
        username = "Loading...";
        avatarURL = "";
        ssid = "no";
        rank = 1;
        score = 6942;
        accuracy = 100F;

        PostBuilder post = new PostBuilder();

        token = token.replace("\\n", "");
        Debug.i(MessageFormat.format("token : {0}\ndevice id : {1}", token, Config.getOnlineDeviceID()));

        post.addParam("token", token);
        post.addParam("deviceid", Config.getOnlineDeviceID());
        post.addParam("secure", getSecuredString(token, Config.getOnlineDeviceID(), BuildConfig.CODENAME));

        ArrayList<String> response = sendRequest(post, BuildConfig.URL + "api/login");

        if (response == null) {
            return false;
        }

        try {
            JsonObject jsonObject = JsonParser.parseString(response.get(0)).getAsJsonObject();

            String message = jsonObject.get("status").getAsString();
            switch (message) {
                case "0":
                    ToastLogger.showText(
                            MessageFormat.format("Failed to log in.\n{0}",
                                    CodeMessages.getErrorMessageCode(jsonObject.get("code").getAsString())), true);
                    failMessage = "Failed to login.";
                    return false;
                case "1":
                    ToastLogger.showText("Successfully logged in.", true);
                    JsonObject object = jsonObject.get("output").getAsJsonObject();
                    userId = object.get("uuid").getAsString();
                    username = object.get("name").getAsString();
                    if(object.has("profile")) {
                        String discordId = object.get("discord_id").getAsString();
                        String avatarId = object.get("profile").getAsString();
                        avatarURL = "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatarId + ".png?size=100";
                    }
                    isStaff = object.get("staff").getAsInt() == 1;
                    isMappooler = object.get("mappooler").getAsInt() == 1;
                    return true;
                default:
                    ToastLogger.showText("I am sorry but Something went wrong. so I could not log in.", true);
                    failMessage = "Failed to login.";
                    return false;
            }


        }
        catch(Exception e) {
            ToastLogger.showText("ERROR!", true);
            e.printStackTrace();
        }

        // TODO : 이 파이어베이스는 추후에 알아보기

//        Bundle bParams = new Bundle();
//        bParams.putString(FirebaseAnalytics.Param.METHOD, "ingame");
//        GlobalManager.getInstance().getMainActivity().getAnalytics().logEvent(FirebaseAnalytics.Event.LOGIN,
//            bParams);

        return true;
    }

    boolean tryToLogIn() throws OnlineManagerException {
        if (logIn(username) == false) {
            stayOnline = false;
            return false;
        }
        return true;
    }

    // No use
    public boolean register(final String username, final String password, final String email,
                            final String deviceID) throws OnlineManagerException {
        PostBuilder post = new PostBuilder();
        post.addParam("username", username);
        post.addParam("password", MD5Calcuator.getStringMD5(password + "taikotaiko"));
        post.addParam("email", email);
        post.addParam("deviceID", deviceID);

        ArrayList<String> response = sendRequest(post, endpoint + "register.php");

        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.METHOD, "ingame");
        GlobalManager.getInstance().getMainActivity().getAnalytics().logEvent(FirebaseAnalytics.Event.SIGN_UP,
            params);

        return (response != null);
    }

    public boolean sendRecord(StatisticV2 stats) throws OnlineManagerException {

        if(userId.equals("")) {
            ToastLogger.showText("Please log in to submit the score!", true);
            return false;
        }

        long time = System.currentTimeMillis() / 1000L;

        Debug.i("Sending record...");

        StringBuilder sb = new StringBuilder();
        sb.append(stats.getHit300k());
        sb.append(stats.getHit300());
        sb.append(stats.getHit100k());
        sb.append(stats.getHit100());
        sb.append(stats.getHit50());
        sb.append(stats.getMisses());
        sb.append(stats.getModifiedTotalScore());
        sb.append(String.format(Locale.ENGLISH, "%2.2f%%", stats.getAccuracy() * 100));
        sb.append(stats.getMapHash());
        sb.append(time);
        sb.append(stats.getModString());
        sb.append(stats.getMark());
        sb.append(stats.getMaxCombo());

        PostBuilder post = new PostBuilder();

        post.addParam("secure", getSecuredString(sb.toString()));
        post.addParam("_300x", String.valueOf(stats.getHit300k()));
        post.addParam("_300", String.valueOf(stats.getHit300()));
        post.addParam("_100x", String.valueOf(stats.getHit100k()));
        post.addParam("_100", String.valueOf(stats.getHit100()));
        post.addParam("_50", String.valueOf(stats.getHit50()));
        post.addParam("miss", String.valueOf(stats.getMisses()));
        post.addParam("score", String.valueOf(stats.getModifiedTotalScore()));
        post.addParam("acc", String.format(Locale.ENGLISH, "%2.2f%%", stats.getAccuracy() * 100));
        post.addParam("mapHash", String.valueOf(stats.getMapHash()));
        post.addParam("time", String.valueOf(time));
        post.addParam("modList", stats.getModString());
        post.addParam("rank", stats.getMark());
        post.addParam("maxCombo", String.valueOf(stats.getMaxCombo()));
        post.addParam("uuid", userId);

        ArrayList<String> response = sendRequest(post, BuildConfig.URL + "api/submitRecord");

        if (response == null) {
            return false;
        }

        JsonObject object = JsonParser.parseString(response.get(0)).getAsJsonObject();

        String message = object.get("status").getAsString();

        switch (message) {
            case "0":
                ToastLogger.showText("Failed to upload the score.\n" + CodeMessages.getErrorMessageCode(object.get("code").getAsString()), true);
                break;
            case "1":
                ToastLogger.showText("Successfully uploaded the score.", true);
                break;
            default:
                ToastLogger.showText("I am sorry but Something went wrong. so I could not upload the score.", true);
                break;
        }

        return true;
    }

    public ArrayList<String> getTop(final File trackFile, final String hash) throws OnlineManagerException {
        PostBuilder post = new PostBuilder();
        post.addParam("filename", trackFile.getName());
        post.addParam("hash", hash);

        ArrayList<String> response = sendRequest(post, endpoint + "getrank.php");

        if (response == null) {
            return new ArrayList<String>();
        }

        response.remove(0);

        return response;
    }

    public boolean loadAvatarToTextureManager() {
        return loadAvatarToTextureManager(this.avatarURL, "userAvatar");
    }

    public boolean loadAvatarToTextureManager(String avatarURL, String userName) {
        if (avatarURL == null || avatarURL.length() == 0) return false;

        String filename = MD5Calcuator.getStringMD5(avatarURL + userName);
        Debug.i("Loading avatar from " + avatarURL);
        Debug.i("filename = " + filename);
        File picfile = new File(Config.getCachePath(), filename);

        if(!picfile.exists()) {
            OnlineFileOperator.downloadFile(avatarURL, picfile.getAbsolutePath());
        }else if(picfile.exists() && picfile.length() < 1) {
            picfile.delete();
            OnlineFileOperator.downloadFile(avatarURL, picfile.getAbsolutePath());
        }
        int imageWidth = 0, imageHeight = 0;
        boolean fileAvailable = true;

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            imageWidth = BitmapFactory.decodeFile(picfile.getPath()).getWidth();
            imageHeight = BitmapFactory.decodeFile(picfile.getPath()).getHeight();
            options.inJustDecodeBounds = false;
            options = null;
        } catch (NullPointerException e) {
            fileAvailable = false;
        }
        if (fileAvailable && (imageWidth * imageHeight) > 0) {
            //头像已经缓存好在本地
            ResourceManager.getInstance().loadHighQualityFile(userName, picfile);
            if (ResourceManager.getInstance().getTextureIfLoaded(userName) != null) {
                return true;
            }
        }

        Debug.i("Success!");
        return false;
    }

    public String getScorePack(int playid) throws OnlineManagerException {
        PostBuilder post = new PostBuilder();
        post.addParam("playID", String.valueOf(playid));

        ArrayList<String> response = sendRequest(post, endpoint + "gettop.php");

        if (response == null || response.size() < 2) {
            return "";
        }

        return response.get(1);
    }

    public String getFailMessage() {
        return failMessage;
    }

    public long getRank() {
        return rank;
    }

    public long getScore() {
        return score;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public String getAvatarURL() {
        return avatarURL;
    }

    public String getUsername() {
        return username;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public boolean isStayOnline() {
        return stayOnline;
    }

    public void setStayOnline(boolean stayOnline) {
        this.stayOnline = stayOnline;
    }

    public boolean isReadyToSend() {
        return (playID != null);
    }

    public int getMapRank() {
        return mapRank;
    }

    public static class OnlineManagerException extends Exception {
        private static final long serialVersionUID = -5703212596292949401L;

        public OnlineManagerException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public OnlineManagerException(final String message) {
            super(message);
        }
    }
}
