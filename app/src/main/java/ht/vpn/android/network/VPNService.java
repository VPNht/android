package ht.vpn.android.network;

import android.util.Base64;

import ht.vpn.android.network.responses.ServersResponse;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.http.GET;

public class VPNService {
    private static final String API_URL = "https://api.digital.ht/";
    private Client mClient;

    private VPNService(String username, String password) {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder().setEndpoint(API_URL);

        if (username != null && password != null) {
            final String credentials = username + ":" + password;
            adapterBuilder.setRequestInterceptor(new UserAgentInterceptor() {
                @Override
                public void intercept(RequestInterceptor.RequestFacade request) {
                    super.intercept(request);
                    String string = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                    request.addHeader("Authorization", string);
                }
            });
        }

        RestAdapter restAdapter = adapterBuilder.build();
        mClient = restAdapter.create(Client.class);
    }

    public static VPNService.Client get() {
        return new VPNService(null, null).mClient;
    }

    public static VPNService.Client get(String username, String password) {
        return new VPNService(username, password).mClient;
    }

    public interface Client {
        @GET("/servers")
        void servers(Callback<ServersResponse> callback);
    }
}
