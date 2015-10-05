package ht.vpn.android.activities;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import butterknife.ButterKnife;
import butterknife.Bind;
import ht.vpn.android.R;

public class BaseActivity extends AppCompatActivity {

    @Nullable
    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    protected void onCreate(Bundle savedInstanceState, int layoutRes) {
        super.onCreate(savedInstanceState);
        setContentView(layoutRes);
        ButterKnife.bind(this);

        if(mToolbar != null) {
            setSupportActionBar(mToolbar);
        }
    }

}
