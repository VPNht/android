package ht.vpn.android.fragment;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.LogItem;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;
import ht.vpn.android.LaunchVPN;
import ht.vpn.android.Preferences;
import ht.vpn.android.R;
import ht.vpn.android.VPNhtApplication;
import ht.vpn.android.activities.MainActivity;
import ht.vpn.android.content.VPNHTConfig;
import ht.vpn.android.fragment.dialog.ConnectingDialogFragment;
import ht.vpn.android.fragment.dialog.DisconnectingDialogFragment;
import ht.vpn.android.fragment.dialog.RebootDialogFragment;
import ht.vpn.android.network.IPService;
import ht.vpn.android.network.VPNService;
import ht.vpn.android.network.responses.Server;
import ht.vpn.android.network.responses.ServersResponse;
import ht.vpn.android.utils.PrefUtils;
import ht.vpn.android.utils.ThreadUtils;
import ht.vpn.android.widget.TouchableScrollView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

import static de.blinkt.openvpn.core.VpnStatus.addLogListener;
import static de.blinkt.openvpn.core.VpnStatus.addStateListener;
import static de.blinkt.openvpn.core.VpnStatus.clearLog;
import static de.blinkt.openvpn.core.VpnStatus.getlogbuffer;
import static de.blinkt.openvpn.core.VpnStatus.removeLogListener;
import static de.blinkt.openvpn.core.VpnStatus.removeStateListener;

public class VPNFragment extends BaseFragment implements VpnStatus.LogListener, VpnStatus.StateListener {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private LatLng mCurrentLocation;
    private Boolean mShowsConnected = false, mCreated = false;
    private String mDetectedCountry;
    private VPNService.Client mVPNAPI;
    private ArrayList<Server> mServers = new ArrayList<>();
    private HashMap<Marker, Integer> mMarkers = new HashMap<>();
    private MainActivity mActivity;
    private ConnectionStatus mCurrentVPNState = ConnectionStatus.LEVEL_NOTCONNECTED;
    private Marker mCurrentPosMarker;

    @BindView(R.id.scrollView)
    TouchableScrollView mScrollView;
    @BindView(R.id.connectedCard)
    CardView mConnectedCard;
    @BindView(R.id.connectCard)
    CardView mConnectCard;
    @BindView(R.id.connectButton)
    Button mConnectButton;
    @BindView(R.id.disconnectButton)
    Button mDisconnectButton;
    @BindView(R.id.locationSpinner)
    Spinner mLocationSpinner;
    @BindView(R.id.firewallSwitch)
    CheckBox mFirewallSwitch;
    @BindView(R.id.ipText)
    TextView mIPText;
    @BindView(R.id.locationText)
    TextView mLocationText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vpn, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (MainActivity) getActivity();

        mMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        mMapFragment.getMapAsync(mMapReadyCallback);

        mScrollView.addInterceptScrollView(mMapFragment.getView());

        addLogListener(this);
        addStateListener(this);

        updateIPData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeStateListener(this);
        removeLogListener(this);
    }

    @OnClick(R.id.connectButton)
    public void connectClick(View v) {
        ConnectingDialogFragment.show(getChildFragmentManager());

        final int spinnerPosition = mLocationSpinner.getSelectedItemPosition();
        final boolean firewall = mFirewallSwitch.isChecked();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                if(mServers.size() < spinnerPosition || spinnerPosition < 0) {
                    try {
                        Snackbar.make(mScrollView, R.string.no_server_selected, Snackbar.LENGTH_SHORT).show();
                    } catch (RuntimeException e) {}
                    ConnectingDialogFragment.dismiss(getChildFragmentManager());
                    return null;
                }

                clearLog();
                Server server = mServers.get(spinnerPosition);
                PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_HOSTNAME, server.hostname);
                PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_COUNTRY, server.country);
                PrefUtils.save(mActivity, Preferences.LAST_CONNECTED_FIREWALL, firewall);
                ConfigParser configParser = new ConfigParser();
                try {
                    configParser.parseConfig(new StringReader(VPNHTConfig.generate(PrefUtils.getPrefs(mActivity), server, firewall)));
                    VpnProfile profile = configParser.convertProfile();
                    profile.mName = server.country;
                    profile.mUsername = PrefUtils.get(mActivity, Preferences.USERNAME, "");
                    profile.mPassword = PrefUtils.get(mActivity, Preferences.PASSWORD, "");
                    profile.mAuthenticationType = VpnProfile.TYPE_USERPASS;
                    ProfileManager.setTemporaryProfile(VPNhtApplication.getAppContext(), profile);


                    Intent vpnPermissionIntent = VpnService.prepare(mActivity);

                    if (vpnPermissionIntent != null) {
                        Intent intent = new Intent(mActivity, LaunchVPN.class);
                        intent.setAction(Intent.ACTION_MAIN);
                        intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUIDString());
                        intent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
                        startActivity(intent);
                    } else {
                        VPNLaunchHelper.startOpenVpn(profile, VPNhtApplication.getAppContext());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ConfigParser.ConfigParseError configParseError) {
                    configParseError.printStackTrace();
                }
                return null;
            }
        }.execute();

    }

    @OnClick(R.id.disconnectButton)
    void diconnectClick(View v) {
        DisconnectingDialogFragment.show(getChildFragmentManager());
        try {
            mActivity.getService().stopVPN(false);
        } catch (RemoteException | NullPointerException e) {
            VpnStatus.logException(e);
        }
    }

    private void updateMapLocation() {
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMap != null && mCurrentLocation != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation, 5));
                    if (mCurrentPosMarker == null) {
                        mCurrentPosMarker = mMap.addMarker(new MarkerOptions().position(mCurrentLocation).icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location)));
                    } else {
                        mCurrentPosMarker.setPosition(mCurrentLocation);
                        mCurrentPosMarker.setVisible(true);
                    }
                }
            }
        });
    }

    private void updateIPData() {
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLocationText.setText(R.string.loading);
                mIPText.setText(R.string.loading);
                if(mCurrentPosMarker != null)
                    mCurrentPosMarker.setVisible(false);
            }
        });
        IPService.get().status(mIPCallback);
    }

    private void processServers() {
        Marker[] markers = mMarkers.keySet().toArray(new Marker[mMarkers.size()]);
        for (int i = 0; i < mServers.size(); i++) {
            Server server = mServers.get(i);
            if (server.getCoordinates() != null && server.countryCode != null && server.countryCode.equals(mDetectedCountry)) {
                mCurrentLocation = server.getCoordinates();

                for (Marker marker : markers) {
                    if (marker.getPosition().equals(mCurrentLocation)) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        }
    }

    private OnMapReadyCallback mMapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            mMap.setOnMarkerClickListener(mMarkerClickListener);
            updateMapLocation();

            if(mActivity != null) {
                mVPNAPI = VPNService.get(PrefUtils.get(mActivity, Preferences.USERNAME, ""), PrefUtils.get(mActivity, Preferences.PASSWORD, ""));
                mVPNAPI.servers(mServersCallback);
            }
        }
    };

    private Callback<IPService.Data> mIPCallback = new Callback<IPService.Data>() {
        @Override
        public void success(final IPService.Data data, Response response) {
            if(mActivity == null) return;

            if(response != null && response.getStatus() == 200) {
                mShowsConnected = data.connected;
                mDetectedCountry = data.country;

                if(!mShowsConnected && mCurrentVPNState.equals(ConnectionStatus.LEVEL_CONNECTED)) {
                    updateIPData();
                    return;
                }

                String location = null;
                if (mShowsConnected) {
                    mConnectedCard.setVisibility(View.VISIBLE);
                } else {
                    mConnectedCard.setVisibility(View.GONE);
                    try {
                        Geocoder coder = new Geocoder(mActivity);
                        List<Address> addressList;
                        if (!data.hasCoordinates()) {
                            addressList = coder.getFromLocationName("Country: " + data.country, 1);
                        } else {
                            addressList = coder.getFromLocation(data.getLat(), data.getLng(), 1);
                        }
                        if (addressList != null && addressList.size() > 0) {
                            Address address = addressList.get(0);
                            if (address.getLocality() == null) {
                                location = address.getCountryName();
                            } else {
                                location = String.format("%s, %s", address.getLocality(), address.getCountryCode());
                            }

                            if (address.hasLatitude() && address.hasLongitude())
                                mCurrentLocation = new LatLng(address.getLatitude(), address.getLongitude());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (location == null && data.country != null) {
                    Locale locale = new Locale("", data.country);
                    location = locale.getDisplayCountry();
                }

                final String finalLocation = location;

                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mIPText.setText(data.ip);
                        mLocationText.setText(finalLocation);
                    }
                });

                processServers();
                updateMapLocation();
            }
        }

        @Override
        public void failure(RetrofitError error) {
            Timber.e(error.getMessage());
            if (error.getResponse() != null && error.getResponse().getStatus() == 401) {
                mActivity.startLoginActivity();
            } else if (error.getCause() instanceof SocketTimeoutException){
                updateIPData();
            }
        }
    };

    private Callback<ServersResponse> mServersCallback = new Callback<ServersResponse>() {
        @Override
        public void success(ServersResponse serversResponse, Response response) {
            if(mActivity == null) return;

            mServers = (ArrayList<Server>) serversResponse.servers;
            for(Marker marker : mMarkers.keySet()) {
                marker.remove();
            }

            ArrayList<String> spinnerList = new ArrayList<>();
            for(Server server : serversResponse.servers) {
                Marker marker;
                if (server.getCoordinates() != null) {
                    marker = mMap.addMarker(new MarkerOptions().position(server.getCoordinates()).icon(BitmapDescriptorFactory.fromResource(R.drawable.server)));
                } else {
                    marker = mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).icon(BitmapDescriptorFactory.fromResource(R.drawable.server)));
                    marker.setVisible(false);
                }
                mMarkers.put(marker, spinnerList.size());

                spinnerList.add(server.country);
            }

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(mActivity, R.layout.simple_spinner_item, spinnerList);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLocationSpinner.setAdapter(arrayAdapter);
                }
            });

            if(mShowsConnected) {
                processServers();
            }
            updateMapLocation();
        }

        @Override
        public void failure(RetrofitError error) {
            Toast.makeText(mActivity, R.string.unknown_error, Toast.LENGTH_SHORT).show();

            if(error.getResponse() != null && error.getResponse().getStatus() == 401)
                mActivity.startLoginActivity();
        }
    };

    private GoogleMap.OnMarkerClickListener mMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if(mMarkers.containsKey(marker)) {
                mLocationSpinner.setSelection(mMarkers.get(marker));
            }

            return true;
        }
    };

    @Override
    public void newLog(LogItem logItem) {
        Timber.i("%s: %s", logItem.getLogLevel(), logItem.getString(mActivity));
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, final ConnectionStatus level, Intent Intent) {
        Timber.d("State: %s", state);
        if(mCurrentVPNState.equals(level))
            return;

        mCurrentVPNState = level;

        if(level.equals(ConnectionStatus.LEVEL_CONNECTED) || level.equals(ConnectionStatus.LEVEL_NOTCONNECTED)) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateIPData();
                }
            }, 500);
            ConnectingDialogFragment.dismiss(getChildFragmentManager());
            DisconnectingDialogFragment.dismiss(getChildFragmentManager());
        }

        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(level.equals(ConnectionStatus.LEVEL_CONNECTED)) {
                    mConnectButton.setVisibility(View.GONE);
                    mConnectedCard.setVisibility(View.VISIBLE);
                    mConnectCard.setVisibility(View.GONE);
                    mDisconnectButton.setVisibility(View.VISIBLE);
                } else if(level.equals(ConnectionStatus.LEVEL_NOTCONNECTED)) {
                    mConnectedCard.setVisibility(View.GONE);
                    mDisconnectButton.setVisibility(View.GONE);
                    mConnectButton.setVisibility(View.VISIBLE);
                    mConnectCard.setVisibility(View.VISIBLE);

                    if(getlogbuffer().length > 10) {
                        for(int i = getlogbuffer().length - 10; i < getlogbuffer().length; i++) {
                            LogItem log = getlogbuffer()[i];
                            String logString = log.getString(mActivity);
                            if (logString.contains("Cannot open TUN")) {
                                Timber.d("NEEDS REBOOT");
                                RebootDialogFragment.show(getChildFragmentManager());
                                break;
                            }
                        }
                    }
                } else {
                    ConnectingDialogFragment.show(getChildFragmentManager());
                }
            }
        });
    }

    @Override
    public void setConnectedVPN(String uuid) {

    }
}
