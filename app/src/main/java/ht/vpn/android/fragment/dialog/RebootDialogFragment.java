package ht.vpn.android.fragment.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import ht.vpn.android.R;

public class RebootDialogFragment extends DialogFragment {

    private static final String TAG = "reboot_dialog";

    public static RebootDialogFragment show(FragmentManager fm) {
        try {
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment == null) {
                RebootDialogFragment dialogFragment = new RebootDialogFragment();
                dialogFragment.show(fm, TAG);
                return dialogFragment;
            }
            return (RebootDialogFragment) fragment;
        } catch (IllegalStateException e) {
            // Catch 'Can not perform this action after onSaveInstanceState'
        }

        return null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setMessage(R.string.reboot_message);
        dialogBuilder.setTitle(R.string.reboot_title);
        setCancelable(false);
        return dialogBuilder.create();
    }

}
