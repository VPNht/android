package ht.vpn.android;

import android.content.Intent;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.ICSOpenVPNApplication;
import de.blinkt.openvpn.core.VpnStatus;
import ht.vpn.android.utils.PrefUtils;
import timber.log.Timber;

public class VPNhtApplication extends ICSOpenVPNApplication implements VpnStatus.StateListener {

    private static VPNhtApplication sThis;
    private boolean mCreated = false;

    @Override
    public void onCreate() {
        super.onCreate();
        sThis = this;

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        VpnStatus.addStateListener(this);
    }

    public static VPNhtApplication getAppContext() {
        return sThis;
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level, Intent intent) {
        if(!mCreated) {
            mCreated = true;
            return;
        }

        if(level.equals(ConnectionStatus.LEVEL_NOTCONNECTED)) {
            PrefUtils.remove(this, Preferences.LAST_CONNECTED_HOSTNAME);
            PrefUtils.remove(this, Preferences.LAST_CONNECTED_FIREWALL);
            PrefUtils.remove(this, Preferences.LAST_CONNECTED_COUNTRY);
        }
    }

    @Override
    public void setConnectedVPN(String uuid) {
    }
}
