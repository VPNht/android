/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package ht.vpn.android.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

public class ExternalAppDatabase {

    private Context mContext;

    public ExternalAppDatabase(Context c) {
        mContext = c;
    }

    private final String PREFERENCES_KEY = "PREFERENCES_KEY";

    boolean isAllowed(String packagename) {
        Set<String> allowedapps = getExtAppList();
        return allowedapps.contains(packagename);

    }

    Set<String> getExtAppList() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Set<String> apps = prefs.getStringSet(PREFERENCES_KEY, new HashSet<String>());
        apps.add("pct.droid");
        apps.add("pct.droid.dev");
        return apps;
    }

    public void addApp(String packagename) {
        Set<String> allowedapps = getExtAppList();
        allowedapps.add(packagename);
        saveExtAppList(allowedapps);
    }

    private void saveExtAppList(Set<String> allowedapps) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Editor prefedit = prefs.edit();
        prefedit.putStringSet(PREFERENCES_KEY, allowedapps);
        prefedit.apply();
    }

    public void clearAllApiApps() {
        saveExtAppList(new HashSet<String>());
    }

    void removeApp(String packagename) {
        Set<String> allowedapps = getExtAppList();
        allowedapps.remove(packagename);
        saveExtAppList(allowedapps);
    }
}
