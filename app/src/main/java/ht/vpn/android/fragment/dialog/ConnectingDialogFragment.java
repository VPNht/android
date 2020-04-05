package ht.vpn.android.fragment.dialog;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import de.blinkt.openvpn.core.VpnStatus;
import ht.vpn.android.R;
import ht.vpn.android.utils.ThreadUtils;

public class ConnectingDialogFragment extends DialogFragment implements VpnStatus.StateListener {

    private static final String TAG = "conn_dialog";
    private ProgressDialog mDialog;
    private Boolean mAttached = false;

    public static ConnectingDialogFragment show(FragmentManager fm) {
        try {
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment == null) {
                ConnectingDialogFragment dialogFragment = new ConnectingDialogFragment();
                dialogFragment.show(fm, TAG);
                return dialogFragment;
            }
            return (ConnectingDialogFragment) fragment;
        } catch (IllegalStateException e) {
            // Catch 'Cannot perform this action after onSaveInstanceState'
        }

        return null;
    }

    public static void dismiss(FragmentManager fm) {
        try {
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                ConnectingDialogFragment dialogFragment = (ConnectingDialogFragment) fragment;
                dialogFragment.dismiss();
            }
        } catch (IllegalStateException e) {
            // Catch 'Cannot perform this action after onSaveInstanceState'
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDialog = new ProgressDialog(getActivity());
        mDialog.setIndeterminate(true);
        mDialog.setMessage(getString(R.string.state_connecting));
        setCancelable(false);
        return mDialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        VpnStatus.addStateListener(this);
        mAttached = true;
    }

    @Override
    public void onDetach() {
        mAttached = false;
        VpnStatus.removeStateListener(this);
        super.onDetach();
    }

    @Override
    public void updateState(final String state, String logmessage, final int localizedResId, VpnStatus.ConnectionStatus level) {
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!state.equals("NOPROCESS") && mAttached) {
                    mDialog.setMessage(getString(localizedResId));
                }
            }
        });
    }

}