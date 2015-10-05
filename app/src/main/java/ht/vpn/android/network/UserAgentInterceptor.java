package ht.vpn.android.network;

import android.os.Build;

import ht.vpn.android.BuildConfig;
import retrofit.RequestInterceptor;

public class UserAgentInterceptor implements RequestInterceptor {
    @Override
    public void intercept(RequestFacade request) {
        request.addHeader("User-Agent", String.format("VPN.HT Android/%s (%d); SDK %d", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, Build.VERSION.SDK_INT));
        request.addHeader("Accept", "application/json");
    }
}
