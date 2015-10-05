package ht.vpn.android.network;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.http.GET;

public class IPService {
    private static final String API_URL = "http://myip.ht/";
    private Client mClient;

    private IPService() {
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint(API_URL).setRequestInterceptor(new UserAgentInterceptor()).build();
        mClient = restAdapter.create(Client.class);
    }

    public static IPService.Client get() {
        return new IPService().mClient;
    }

    public interface Client {
        @GET("/status")
        void status(Callback<Data> callback);
    }

    public class Data {
        public String ip;
        public String country;
        public double[] coordinates;
        public Boolean connected = false;
        public Object server;

        public boolean hasCoordinates() {
            return coordinates != null;
        }

        public double getLat() {
            if(coordinates != null)
                return coordinates[0];
            return 0;
        }

        public double getLng() {
            if(coordinates != null)
                return coordinates[1];
            return 0;
        }

        public Server getServer() {
            return (Server) server;
        }

        public class Server {
            public String ip;
            public String country;
            public String host;
        }
    }
}
