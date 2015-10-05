package ht.vpn.android.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import butterknife.ButterKnife;

public abstract class BaseFragment extends Fragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }
}
