/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package ht.vpn.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.widget.Toast;

import java.io.IOException;
import java.io.StringReader;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import ht.vpn.android.activities.MainActivity;
import ht.vpn.android.content.VPNHTConfig;
import ht.vpn.android.network.responses.Server;
import ht.vpn.android.utils.PrefUtils;


public class OnBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if((PrefUtils.get(context, Preferences.RECONNECT_REBOOT, false) && (Intent.ACTION_BOOT_COMPLETED.equals(action)) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))) {
            if(PrefUtils.contains(context, Preferences.LAST_CONNECTED_HOSTNAME)) {
				launchVPN(context);
			}
		}
	}

	void launchVPN(Context context) {
        ConfigParser configParser = new ConfigParser();
        try {
            Server server = new Server();
            server.hostname = PrefUtils.get(context, Preferences.LAST_CONNECTED_HOSTNAME, "hub.digital.ht");
            server.country =  PrefUtils.get(context, Preferences.LAST_CONNECTED_COUNTRY, "Nearest");
            configParser.parseConfig(new StringReader(VPNHTConfig.generate(PrefUtils.getPrefs(context), server, PrefUtils.get(context, Preferences.LAST_CONNECTED_FIREWALL, false))));
            VpnProfile profile = configParser.convertProfile();
            profile.mName = server.country;
            ProfileManager.setTemporaryProfile(VPNhtApplication.getAppContext(), profile);

            Intent intent = new Intent(context, LaunchVPN.class);
            intent.setAction(Intent.ACTION_MAIN);
            intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUIDString());
            intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ConfigParser.ConfigParseError configParseError) {
            configParseError.printStackTrace();
        }
	}

}
