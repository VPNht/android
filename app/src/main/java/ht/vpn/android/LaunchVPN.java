/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package ht.vpn.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.io.IOException;

import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;
import ht.vpn.android.activities.GrantPermissionsActivity;
import ht.vpn.android.activities.LoginActivity;
import ht.vpn.android.utils.PrefUtils;

/**
 * This Activity actually handles two stages of a launcher shortcut's life cycle.
 * <p/>
 * 1. Your application offers to provide shortcuts to the launcher.  When
 * the user installs a shortcut, an activity within your application
 * generates the actual shortcut and returns it to the launcher, where it
 * is shown to the user as an icon.
 * <p/>
 * 2. Any time the user clicks on an installed shortcut, an intent is sent.
 * Typically this would then be handled as necessary by an activity within
 * your application.
 * <p/>
 * We handle stage 1 (creating a shortcut) by simply sending back the information (in the form
 * of an {@link android.content.Intent} that the launcher will use to create the shortcut.
 * <p/>
 * You can also implement this in an interactive way, by having your activity actually present
 * UI for the user to select the specific nature of the shortcut, such as a contact, picture, URL,
 * media item, or action.
 * <p/>
 * We handle stage 2 (responding to a shortcut) in this sample by simply displaying the contents
 * of the incoming {@link android.content.Intent}.
 * <p/>
 * In a real application, you would probably use the shortcut intent to display specific content
 * or start a particular operation.
 */
public class LaunchVPN extends Activity {

    public static final String EXTRA_KEY = "ht.vpn.android.shortcutProfileUUID";
    public static final String EXTRA_NAME = "ht.vpn.android.shortcutProfileName";
    public static final String EXTRA_HIDELOG = "ht.vpn.android.showNoLogWindow";

    private static final int START_VPN_PROFILE = 70, LOGIN_ACTIVITY = 100;


    private ProfileManager mPM;
    private VpnProfile mSelectedProfile;
    private boolean mhideLog = false;

    private boolean mCmfixed = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPM = ProfileManager.getInstance(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Resolve the intent

        final Intent intent = getIntent();
        final String action = intent.getAction();

        // If the intent is a request to create a shortcut, we'll do that and exit


        if (Intent.ACTION_MAIN.equals(action)) {
            // we got called to be the starting point, most likely a shortcut
            String shortcutUUID = intent.getStringExtra(EXTRA_KEY);
            String shortcutName = intent.getStringExtra(EXTRA_NAME);
            mhideLog = intent.getBooleanExtra(EXTRA_HIDELOG, false);

            VpnProfile profileToConnect = ProfileManager.get(this, shortcutUUID);
            if (shortcutName != null && profileToConnect == null)
                profileToConnect = ProfileManager.getInstance(this).getProfileByName(shortcutName);

            if (profileToConnect == null) {
                VpnStatus.logError(R.string.shortcut_profile_notfound);
                // show Log window to display error
                finish();
                return;
            }

            mSelectedProfile = profileToConnect;
            launchVPN();

        }
    }

    private void askForPW(final int type) {
        if(!PrefUtils.contains(this, Preferences.USERNAME) || !PrefUtils.contains(this, Preferences.PASSWORD)) {
            if (type == R.string.password) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.putExtra(LoginActivity.PROCEED_TO_MAIN, false);
                startActivityForResult(intent, LOGIN_ACTIVITY);
            }
        } else {
            onActivityResult(LOGIN_ACTIVITY, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_VPN_PROFILE) {
            if (resultCode == Activity.RESULT_OK) {
                int needpw = mSelectedProfile.needUserPWInput(false);
                if (needpw != 0) {
                    VpnStatus.updateStateString("USER_VPN_PASSWORD", "", R.string.state_user_vpn_password,
                            ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
                    askForPW(needpw);
                } else {
                    new Thread(startOpenVpnRunnable).start();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // User does not want us to start, so we just vanish
                VpnStatus.updateStateString("USER_VPN_PERMISSION_CANCELLED", "", R.string.state_user_vpn_permission_cancelled, ConnectionStatus.LEVEL_NOTCONNECTED);
                finish();
            }
        } else if(requestCode == LOGIN_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                mSelectedProfile.mUsername = PrefUtils.get(this, Preferences.USERNAME, "");
                mSelectedProfile.mPassword = PrefUtils.get(this, Preferences.PASSWORD, "");
                onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
            } else {
                VpnStatus.updateStateString("USER_VPN_PASSWORD_CANCELLED", "", R.string.state_user_vpn_password_cancelled, ConnectionStatus.LEVEL_NOTCONNECTED);
                finish();
            }
        }
    }

    void showConfigErrorDialog(int vpnok) {
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle(R.string.config_error_found);
        d.setMessage(vpnok);
        d.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();

            }
        });
        d.show();
    }

    void launchVPN() {
        int vpnok = mSelectedProfile.checkProfile(this);
        if (vpnok != R.string.no_error_found) {
            showConfigErrorDialog(vpnok);
            return;
        }

        Intent intent = VpnService.prepare(this);
        // Check if we want to fix /dev/tun
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean usecm9fix = prefs.getBoolean("useCM9Fix", false);
        boolean loadTunModule = prefs.getBoolean("loadTunModule", false);

        if (loadTunModule)
            execeuteSUcmd("insmod /system/lib/modules/tun.ko");

        if (usecm9fix && !mCmfixed) {
            execeuteSUcmd("chown system /dev/tun");
        }


        if (intent != null) {
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission, ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            // Start the query
            startActivityForResult(new Intent(this, GrantPermissionsActivity.class), START_VPN_PROFILE);
        } else {
            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
        }

    }

    private void execeuteSUcmd(String command) {
        ProcessBuilder pb = new ProcessBuilder("su", "-c", command);
        try {
            Process p = pb.start();
            int ret = p.waitFor();
            if (ret == 0)
                mCmfixed = true;
        } catch (InterruptedException e) {
            VpnStatus.logException("SU command", e);

        } catch (IOException e) {
            VpnStatus.logException("SU command", e);
        }
    }

    private Runnable startOpenVpnRunnable = new Runnable() {
        @Override
        public void run() {
            VPNLaunchHelper.startOpenVpn(mSelectedProfile, getBaseContext());
            finish();
        }
    };

}
