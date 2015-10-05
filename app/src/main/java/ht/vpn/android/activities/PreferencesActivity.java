package ht.vpn.android.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import ht.vpn.android.Preferences;
import ht.vpn.android.R;
import ht.vpn.android.VpnProfile;
import ht.vpn.android.content.PrefItem;
import ht.vpn.android.content.adapters.PreferencesListAdapter;
import ht.vpn.android.fragment.dialog.StringArraySelectorDialogFragment;
import ht.vpn.android.utils.PrefUtils;

public class PreferencesActivity extends BaseActivity {

    private List<Object> mPrefItems = new ArrayList<>();
    private LinearLayoutManager mLayoutManager;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_preferences);

        setupAppBar();

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        PrefUtils.getPrefs(this).registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

        refreshItems();
    }

    private void setupAppBar() {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);

            actionBar.setTitle(R.string.preferences);
        }
    }

    private void refreshItems() {
        mPrefItems = new ArrayList<>();

        mPrefItems.add(getString(R.string.behaviour));

        mPrefItems.add(new PrefItem(this, R.drawable.ic_prefs_killswitch, R.string.killswitch, Preferences.KILLSWITCH, true,
                        new PrefItem.OnClickListener() {
                            @Override
                            public void onClick(PrefItem item) {
                                item.saveValue(!(Boolean) item.getValue());
                            }
                        },
                        new PrefItem.SubTitleGenerator() {
                            @Override
                            public String get(PrefItem item) {
                                return getString(R.string.killswitch_summary);
                            }
                        })
        );

        mPrefItems.add(new PrefItem(this, R.drawable.ic_prefs_reconnect, R.string.reconnect_reboot, Preferences.RECONNECT_REBOOT, false,
                new PrefItem.OnClickListener() {
                    @Override
                    public void onClick(PrefItem item) {
                        item.saveValue(!(Boolean) item.getValue());
                    }
                },
                new PrefItem.SubTitleGenerator() {
                    @Override
                    public String get(PrefItem item) {
                        return getString(R.string.reconnect_reboot_summary);
                    }
                }
            )
        );

        mPrefItems.add(getString(R.string.connection));

        final List<String> options = Arrays.asList("BF-CBC", "AES-128-CBC", "AES-256-CBC");
        mPrefItems.add(new PrefItem(this, R.drawable.ic_prefs_encryption, R.string.cipher, Preferences.ENC_TYPE, "AES-128-CBC",
                new PrefItem.OnClickListener() {
                    @Override
                    public void onClick(PrefItem item) {
                        if(options.indexOf(item.getValue()) + 1 == options.size()) {
                            item.saveValue(options.get(0));
                        } else {
                            item.saveValue(options.get(options.indexOf(item.getValue()) + 1));
                        }
                    }
                },
                new PrefItem.SubTitleGenerator() {
                    @Override
                    public String get(PrefItem item) {
                        return item.getValue() + " (" + getString(R.string.not_used_firewal) + ")";
                    }
                }
            )
        );

        mPrefItems.add(new PrefItem(this, R.drawable.ic_prefs_smartdns, R.string.smartdns, Preferences.SMARTDNS, true,
                new PrefItem.OnClickListener() {
                    @Override
                    public void onClick(PrefItem item) {
                        item.saveValue(!(Boolean) item.getValue());
                    }
                },
                new PrefItem.SubTitleGenerator() {
                    @Override
                    public String get(PrefItem item) {
                        return getString(R.string.smartdns_summary);
                    }
                }
            )
        );

        mPrefItems.add(getResources().getString(R.string.account));

        mPrefItems.add(new PrefItem(this, R.drawable.ic_prefs_logout, R.string.logout, Preferences.USERNAME, 0, new PrefItem.OnClickListener() {
            @Override
            public void onClick(final PrefItem item) {
                PrefUtils.remove(PreferencesActivity.this, Preferences.USERNAME);
                PrefUtils.remove(PreferencesActivity.this, Preferences.PASSWORD);
                Intent intent = new Intent(PreferencesActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }, new PrefItem.SubTitleGenerator() {
            @Override
            public String get(PrefItem item) {
                return getString(R.string.press_logout);
            }
        }));

        if (mRecyclerView.getAdapter() != null && mLayoutManager != null) {
            int position = mLayoutManager.findFirstVisibleItemPosition();
            View v = mLayoutManager.findViewByPosition(position);
            mRecyclerView.setAdapter(new PreferencesListAdapter(mPrefItems));
            if (v != null) {
                int offset = v.getTop();
                mLayoutManager.scrollToPositionWithOffset(position, offset);
            } else {
                mLayoutManager.scrollToPosition(position);
            }
        } else {
            mRecyclerView.setAdapter(new PreferencesListAdapter(mPrefItems));
        }
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (isUseChangeablePref(key)) {
                refreshItems();
            }
        }
    };

    private boolean isUseChangeablePref(String key) {
        boolean b = false;
        for (Object item : mPrefItems) {
            if (item instanceof PrefItem) {
                PrefItem pref = (PrefItem) item;
                if (pref.getPrefKey().equals(key))
                    b = true;
            }
        }
        return b;
    }

    private void openListSelectionDialog(String title, String[] items, int mode, int defaultPosition, DialogInterface.OnClickListener onClickListener) {
        if (mode == StringArraySelectorDialogFragment.NORMAL) {
            StringArraySelectorDialogFragment.show(getSupportFragmentManager(), title, items, defaultPosition, onClickListener);
        } else if (mode == StringArraySelectorDialogFragment.SINGLE_CHOICE) {
            StringArraySelectorDialogFragment.showSingleChoice(getSupportFragmentManager(), title, items, defaultPosition, onClickListener);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }
}
