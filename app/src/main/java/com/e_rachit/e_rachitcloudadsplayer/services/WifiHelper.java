package com.e_rachit.e_rachitcloudadsplayer.services;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rohitranjan on 24/01/18.
 */
public class WifiHelper {

    private Activity mActivity;
    private WifiManager mWifiManager;

    public WifiHelper(Activity mActivity) {
        this.mActivity = mActivity;
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mWifiManager != null) {
                List<ScanResult> results = mWifiManager.getScanResults();

                try {
                    mActivity.unregisterReceiver(wifiReceiver);
                } catch (Exception e) {

                }

                showWifiListDialog(results);
            }
        }
    };

    /**
     * checks for the permission
     *
     * @return
     */
    public boolean checkPermission() {
        List<String> permissionsList = new ArrayList<>();
        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mActivity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    mActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                mActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                return false;
            }
        }
        return true;
    }

    /**
     * starts the wifi scan
     */
    public void startWifiScans() {
        mWifiManager = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiManager.startScan();
        try {
            mActivity.unregisterReceiver(wifiReceiver);
        } catch (Exception e) {

        }
        mActivity.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }


    private void showWifiListDialog(List<ScanResult> results) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                mActivity);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                mActivity,
                android.R.layout.select_dialog_item);

        for (ScanResult r : results) {
            if (r == null || r.SSID == null) continue;
            if ("".equalsIgnoreCase(r.SSID.trim())) continue;
            String name = r.SSID.replace("\"", "");
            arrayAdapter.add(name);
        }
        builderSingle.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strName = arrayAdapter.getItem(which);
                        takePassword(strName);
                        Toast.makeText(mActivity, "Selected " + strName, Toast.LENGTH_SHORT).show();
                    }
                });
        AlertDialog dialog = builderSingle.create();
        dialog.show();
    }

    /**
     * take password as input
     *
     * @param ssid
     */
    private void takePassword(final String ssid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Password");

        // Set up the input
        final EditText input = new EditText(mActivity);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString();
                if (password.isEmpty())
                    takePassword(ssid);
                else
                    connectToWifiNetwork(ssid, password);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    /**
     * is Connected
     *
     * @return
     */
    public String isConnected() {
        ConnectivityManager connManager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        /*if(mWifi.isConnected())
            return mWifi.ge*/
        WifiManager wifiManager = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifi.isConnected()) {
            WifiInfo info = wifiManager.getConnectionInfo();
            String ssid = info.getSSID();
            return ssid;
        }
        return null;
    }

    /**
     * connect To Wifi Network
     *
     * @param ssid
     * @param password
     */
    private void connectToWifiNetwork(String ssid, String password) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + ssid + "\"";
        conf.preSharedKey = "\"" + password + "\"";
        WifiManager wifiManager = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.addNetwork(conf);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                break;
            }
        }
    }

}
