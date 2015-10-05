package ht.vpn.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

import ht.vpn.android.content.ObscuredSharedPreferences;
import ht.vpn.android.utils.PrefUtils;

public class Preferences {
    @Deprecated
    public final static String FILE_NAME = "vpn.ht";

    public final static String USERNAME = "username";
    public final static String PASSWORD = "password";
    public final static String ENC_TYPE = "encryption";
    public final static String KILLSWITCH = "killswitch";
    public final static String RECONNECT_NETCHANGE = "netchangereconnect";
    public final static String RECONNECT_REBOOT = "rebootreconnect";
    public final static String SMARTDNS = "smartdns";
    public final static String LOG_VERBOSITY = "log_verbosity";
    public final static String LAST_CONNECTED_HOSTNAME = "hostname_last";
    public final static String LAST_CONNECTED_COUNTRY = "country_latest";
    public final static String LAST_CONNECTED_FIREWALL = "firewall_last";

    @Deprecated
    public static void movePreferences(Context context, String source) {
        SharedPreferences oldPrefs = context.getSharedPreferences(source, Context.MODE_PRIVATE);
        ObscuredSharedPreferences newPrefs = PrefUtils.getPrefs(context);
        Map<String, Object> map = (Map<String, Object>) oldPrefs.getAll();
        SharedPreferences.Editor editor = newPrefs.edit();
        for(String key : map.keySet()) {
            Object item = map.get(key);
            if(item instanceof Boolean) {
                editor.putBoolean(key, (Boolean) item);
            } else if(item instanceof String) {
                editor.putString(key, (String) item);
            } else if(item instanceof Integer) {
                editor.putInt(key, (Integer) item);
            }
        }
        editor.apply();
        oldPrefs.edit().clear().apply();
    }
}
