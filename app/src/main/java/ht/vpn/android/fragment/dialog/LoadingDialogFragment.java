package ht.vpn.android.fragment.dialog;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import ht.vpn.android.R;

public class LoadingDialogFragment extends DialogFragment {

    private static final String TAG = "loading_dialog";

    public static LoadingDialogFragment show(FragmentManager fm) {
        try {
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment == null) {
                LoadingDialogFragment dialogFragment = new LoadingDialogFragment();
                dialogFragment.show(fm, TAG);
                return dialogFragment;
            }
            return (LoadingDialogFragment) fragment;
        } catch (IllegalStateException e) {
            // Catch 'Can not perform this action after onSaveInstanceState'
        }

        return null;
    }

    public static void dismiss(FragmentManager fm) {
        try {
            Fragment fragment = fm.findFragmentByTag(TAG);
            if (fragment != null) {
                LoadingDialogFragment dialogFragment = (LoadingDialogFragment) fragment;
                dialogFragment.dismiss();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            // Catch 'Can not perform this action after onSaveInstanceState'
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.loading));
        setCancelable(false);
        return dialog;
    }

}