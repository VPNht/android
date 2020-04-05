package ht.vpn.android.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.VpnStatus;
import ht.vpn.android.Preferences;
import ht.vpn.android.R;
import ht.vpn.android.utils.NetworkUtils;
import ht.vpn.android.utils.PrefUtils;

public class MainActivity extends BaseActivity implements VpnStatus.StateListener {

    private IOpenVPNServiceInternal mService;
    private Boolean mConnected = false;

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_main);

        if(!PrefUtils.contains(this, Preferences.USERNAME) || !PrefUtils.contains(this, Preferences.PASSWORD)) {
            startLoginActivity();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        if(!NetworkUtils.isNetworkConnected(this)) {
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_prefs).setVisible(!mConnected);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_prefs:
                Intent prefsIntent = new Intent(this, PreferencesActivity.class);
                startActivity(prefsIntent);
                break;
            case R.id.action_log:
                Intent logIntent = new Intent(this, LogActivity.class);
                startActivity(logIntent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startLoginActivity() {
        PrefUtils.remove(this, Preferences.USERNAME);
        PrefUtils.remove(this, Preferences.PASSWORD);
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    };

    public IOpenVPNServiceInternal getService() {
        return mService;
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level, Intent Intent) {
        mConnected = level.equals(ConnectionStatus.LEVEL_CONNECTED);
        supportInvalidateOptionsMenu();
    }

    @Override
    public void setConnectedVPN(String uuid) {
    }
}
