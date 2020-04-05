package ht.vpn.android.fragment.dialog;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import ht.vpn.android.R;

public class DisconnectingDialogFragment extends DialogFragment {

    private static final String TAG = "disc_dialog";

    public static DisconnectingDialogFragment show(FragmentManager fm) {
        try {
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment == null) {
                DisconnectingDialogFragment dialogFragment = new DisconnectingDialogFragment();
                dialogFragment.show(fm, TAG);
                return dialogFragment;
            }
            return (DisconnectingDialogFragment) fragment;
        } catch (IllegalStateException e) {
            // Catch 'Cannot perform this action after onSaveInstanceState'
        }

        return null;
    }

    public static void dismiss(FragmentManager fm) {
        try {
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                DisconnectingDialogFragment dialogFragment = (DisconnectingDialogFragment) fragment;
                dialogFragment.dismiss();
            }
        } catch (IllegalStateException e) {
            // Catch 'Cannot perform this action after onSaveInstanceState'
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.disconnecting));
        setCancelable(false);
        return dialog;
    }

}