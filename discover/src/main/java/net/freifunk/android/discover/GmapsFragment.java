/*
 * GmapsFragment.java
 *
 * Copyright (C) 2014  Philipp Dreimann
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

package net.freifunk.android.discover;


import com.androidmapsextensions.ClusterGroup;
import com.androidmapsextensions.ClusterOptions;
import com.androidmapsextensions.ClusterOptionsProvider;
import com.androidmapsextensions.ClusteringSettings;
import com.androidmapsextensions.GoogleMap;
import com.androidmapsextensions.Marker;
import com.androidmapsextensions.MarkerOptions;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import net.freifunk.android.discover.model.Node;
import net.freifunk.android.discover.model.NodeMap;
import net.freifunk.android.discover.model.MapMaster;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

import com.google.maps.android.clustering.ClusterManager;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 *
 */
public class GmapsFragment extends com.androidmapsextensions.SupportMapFragment implements Observer, GoogleMap.OnMarkerClickListener {

    public static final String ARG_TYPE = "type_id";
    public static final String COMMUNITY_TYPE = "type_community";
    public static final String NODES_TYPE = "type_nodes";
    private static final String TAG = "GmapsFragment";
    private SharedPreferences sharedPrefs = null;
    private HashMap<Marker, Object> markerMap;
    private Callbacks mCallbacks = sDummyCallbacks;
    private ClusterManager<Node> mClusterManager;
    private Node clickedClusterItem = null;
    private View contents = null;
    private GoogleMap mMap = null;

    private static final double[] CLUSTER_SIZES = new double[]{180, 160, 144, 120, 96};


    public static GmapsFragment newInstance(String type) {
        GmapsFragment fragment = new GmapsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TYPE, type);

        MapMaster.getInstance().addObserver(fragment);

        fragment.setArguments(b);
        return fragment;
    }

    public GmapsFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        contents = getLayoutInflater(savedInstanceState).inflate(R.layout.info_window, null);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (getArguments().containsKey(ARG_TYPE)) {

            setupMap();

            String type = (String) getArguments().get(ARG_TYPE);
            if (type.equals(NODES_TYPE)) {

                for (NodeMap m : MapMaster.getInstance().getMaps()) {
                    m.setAddedToMap(false);
                }

                createNodesMap();
            }
        }
    }



    private void setupMap() {
        if (mMap == null) {
            mMap = getExtendedMap();
            mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

            ClusteringSettings clusteringSettings = new ClusteringSettings();
            clusteringSettings.addMarkersDynamically(true);

            clusteringSettings.clusterOptionsProvider(new GmapsClusterOptionsProvider(getResources()));

            double clusterSize = CLUSTER_SIZES[1];

            clusteringSettings.clusterSize(clusterSize);
            mMap.setClustering(clusteringSettings);

            mMap.setMyLocationEnabled(true);
            mMap.setOnMarkerClickListener(this);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51, 9), 6));
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        createNodesMap();
    }

    private void createNodesMap() {

        MapMaster mapMaster = MapMaster.getInstance();

        boolean onlyOnlineNodes = sharedPrefs.getBoolean("nodes_onlyOnline", false);

        markerMap = new HashMap<Marker, Object>();

        for (NodeMap m : mapMaster.getMaps()) {
            if (!m.alreadyAddedToMap()) {
                for(Node n : m.getNodes()) {

                    Marker marker = null;

                    if (m.isActive()) {
                        if (onlyOnlineNodes == false || n.isOnline()) {
                            marker = mMap.addMarker(new MarkerOptions().position(n.getPosition()).title(n.getName()).data(n));
                            n.setMarker(marker);
                        }
                    }
                    else {
                        marker = n.getMarker();

                        if (marker != null) {
                            marker.remove();
                        }
                    }
              }

                m.setAddedToMap(true);
            }
        }
    }




    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onMarkerClicked(Object o) {
            Log.d(TAG, "sDummyCallbacks");
        }
    };



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        public void onMarkerClicked(Object o);
    }

    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            View v = contents;


            if (marker != null && marker.getData() != null) {

                Node n = (Node) marker.getData();
                
                TableLayout table = (TableLayout) v.findViewById(R.id.tableLayout);

                TableRow rowLastUpd = (TableRow) v.findViewById(R.id.tablerow_lastupd);
                TableRow rowHardware = (TableRow) v.findViewById(R.id.tablerow_hardware);
                TableRow rowFirmware = (TableRow) v.findViewById(R.id.tablerow_firmware);
                TableRow rowClients = (TableRow) v.findViewById(R.id.tablerow_clients);
                TableRow rowRxTx = (TableRow) v.findViewById(R.id.tablerow_rxtx);
                TableRow rowUptime = (TableRow) v.findViewById(R.id.tablerow_uptime);
                TableRow rowLoadAvg = (TableRow) v.findViewById(R.id.tablerow_loadavg);

                long lastUpdate = n.getLastUpdate();
                int clientcount = n.getClientCount();
                String firmware = n.getFirmware();
                String hardware = n.getHardware();
                double rxBytes = n.getRxBytes();
                double txBytes = n.getTxBytes();
                int uptime = n.getUptime();
                double loadAvg = n.getLoadavg();

                ImageView ivOnline = (ImageView) v.findViewById(R.id.iv_online);
                ivOnline.setImageResource(n.isOnline() ? R.drawable.ic_action_network_wifi_on : R.drawable.ic_action_network_wifi_off);

                TextView tvName = (TextView) v.findViewById(R.id.tv_name);
                String tvNameStr =  n.getName();
                tvName.setText(tvNameStr);

                TextView tvMapName = (TextView) v.findViewById(R.id.tv_mapname);
                tvMapName.setText(n.getMapname());

                if (lastUpdate > 0) {
                    SimpleDateFormat sdf = null;

                    if ((new Date().getTime() - lastUpdate)  > (60 * 60 *24 * 1000)) {
                        sdf = new SimpleDateFormat("dd.MM.yyyy H:mm");
                    }
                    else {
                        sdf = new SimpleDateFormat("HH:mm");
                    }

                    TextView tvLastUpd = (TextView) v.findViewById(R.id.tv_lastupd);
                    tvLastUpd.setText("" + sdf.format(new Date(lastUpdate)));
                    rowLastUpd.setVisibility(v.VISIBLE);
                }
                else {
                    rowLastUpd.setVisibility(v.GONE);
                }

                if (hardware != null && hardware.length() > 0) {
                    TextView tvHardware = (TextView) v.findViewById(R.id.tv_hardware);
                    tvHardware.setText(hardware);
                    rowHardware.setVisibility(v.VISIBLE);
                }
                else {
                    rowHardware.setVisibility(v.GONE);
                }

                if (firmware!= null && firmware.length() > 0) {
                    TextView tvFirmware = (TextView) v.findViewById(R.id.tv_firmware);
                    tvFirmware.setText(firmware);
                    rowFirmware.setVisibility(v.VISIBLE);
                }
                else {
                    rowFirmware.setVisibility(v.GONE);
                }

                if (clientcount >= 0) {
                    TextView tvClientCount = (TextView) v.findViewById(R.id.tv_clientcount);
                    tvClientCount.setText("" + clientcount);
                    rowClients.setVisibility(v.VISIBLE);
                }
                else {
                    rowClients.setVisibility(v.GONE);
                }


                if (rxBytes > 0 && txBytes > 0) {
                    TextView tvRxTx = (TextView) v.findViewById(R.id.tv_rxtx);
                    tvRxTx.setText("" + (int) (rxBytes / 1024 / 1024) + " MB / " + (int) (txBytes / 1024 / 1024) + " MB");
                    rowRxTx.setVisibility(v.VISIBLE);
                }
                else {
                    rowRxTx.setVisibility(v.GONE);
                }

                if (uptime > 0) {
                    TextView tvUptime = (TextView) v.findViewById(R.id.tv_uptime);
                    int day = (int)TimeUnit.SECONDS.toDays(uptime);
                    long hours = TimeUnit.SECONDS.toHours(uptime) - ( day * 24);
                    long minutes = TimeUnit.SECONDS.toMinutes(uptime) - (TimeUnit.SECONDS.toHours(uptime)* 60);
                    tvUptime.setText("" + day + "days " + hours + "h " + minutes +"m");
                    rowUptime.setVisibility(v.VISIBLE);
                }
                else {
                    rowUptime.setVisibility(v.GONE);
                }

                if (loadAvg > 0) {
                    TextView tvLoadAvg = (TextView) v.findViewById(R.id.tv_loadavg);
                    tvLoadAvg.setText("" + loadAvg);
                    rowLoadAvg.setVisibility(v.VISIBLE);
                }
                else {
                    rowLoadAvg.setVisibility(v.GONE);
                }
            }
            else {
                return null;
            }

            return contents;
        }


    }
}
