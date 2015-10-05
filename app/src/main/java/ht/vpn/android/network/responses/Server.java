package ht.vpn.android.network.responses;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;

public class Server {
    @SerializedName("h")
    public String hostname;
    @SerializedName("l")
    public String location;
    @SerializedName("c")
    public String country;
    @SerializedName("cc")
    public String countryCode;
    @SerializedName("ll")
    public double[] longlat;

    public LatLng getCoordinates() {
        if(longlat == null)
            return null;
        return new LatLng(longlat[1], longlat[0]);
    }
}
