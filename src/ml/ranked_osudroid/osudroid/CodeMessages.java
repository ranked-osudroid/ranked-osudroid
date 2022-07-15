package ml.ranked_osudroid.osudroid;

public class CodeMessages {
    public static String getErrorMessageCode(String code) {
        switch(Integer.parseInt(code)) {
            case 2:
                return "Secure is broken!\nPlease contact to developers!";
            case 3:
                return "Database is broken!\nPlease contact to developers!";
            case 4:
                return "Server error!\nPlease contact to developers!";
            case 6:
                return "This PlayID is expired!";
            case 7:
                return "Couldn't find PlayID!";
            case 11:
                return "This token is not exist!";
            case 12:
                return "This token is locked!\nPlease contact to staff!";
            case 13:
                return "This token is expired!";
            case 14:
                return "Illegal login detected.\nTherefore, This token will be locked.\nYou may appeal in discord server if you think it is wrong.";

        }
        return null;
    }
}
