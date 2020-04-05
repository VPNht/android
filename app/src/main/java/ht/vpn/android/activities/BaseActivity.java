package ht.vpn.android.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import butterknife.ButterKnife;
import butterknife.BindView;
import ht.vpn.android.R;

public abstract class BaseActivity extends AppCompatActivity {

    @Nullable
    @BindView(R.id.toolbar)
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
