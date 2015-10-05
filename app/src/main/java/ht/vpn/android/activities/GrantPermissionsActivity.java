/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package ht.vpn.android.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.VpnService;

import de.blinkt.openvpn.core.VpnStatus;
import ht.vpn.android.R;

public class GrantPermissionsActivity extends Activity {
    private static final int VPN_PREPARE = 0;

    @Override
    protected void onStart() {
        super.onStart();
        Intent i = VpnService.prepare(this);
        if (i == null) {
            onActivityResult(VPN_PREPARE, RESULT_OK, null);
        } else {
            try {
                startActivityForResult(i, VPN_PREPARE);
            } catch (ActivityNotFoundException e) {
                VpnStatus.logError(R.string.no_vpn_support_image);
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode);
        finish();
    }
}
