package com.amitlab.beaconreceiveralt;

import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BeaconConsumer{

    protected static final String TAG = "MonitoringActivity";
    private BeaconManager beaconManager;

    List<String> messageArray = new ArrayList<>();
    ArrayAdapter adapter;
    List<Beacon> beaconsSeen = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new ArrayAdapter<String>(this, R.layout.listview_item, messageArray);

        ListView listView = (ListView)findViewById(R.id.messageList);
        listView.setAdapter(adapter);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        /// To detect proprietary beacons, you must add a line like below corresponding to your beacon type.
        /// Do a web search for "setBeaconLayout" to get the proper expression.

        beaconManager.getBeaconParsers().clear();
        // AltBeacon Layout
        //beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        // iBeacon Layout
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        //Monitor
        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                logToDisplay("I just saw a beacon for the first time!");
            }

            @Override
            public void didExitRegion(Region region) {
                logToDisplay("I no longer see a beacon");
                beaconsSeen.clear();
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                logToDisplay("I have just switched from seeing/not seeing beacons: " + state);
            }
        });

        try {
            //beaconManager.startMonitoringBeaconsInRegion(new Region("myRegionUniqueId", null, null, null));
            Identifier identifier = Identifier.parse("52414449-5553-4E45-5457-4F524B53434F");
            beaconManager.startMonitoringBeaconsInRegion(new Region("myRegion", identifier, null, null));
        } catch (RemoteException e) {    }

        //Ranger
        beaconManager.addRangeNotifier(new RangeNotifier() {
            //private HashSet<Beacon> beaconsSeen = new HashSet<>();

            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    //Beacon firstBeacon = beacons.iterator().next();
                    //logToDisplay("The first beacon " + firstBeacon.toString() + " is about " + firstBeacon.getDistance() + " meters away.");

                    for (Beacon beacon : beacons) {
                        if (!beaconsSeen.contains(beacon)) {
                            beaconsSeen.add(beacon);
                            logToDisplay("The first beacon " + beacon.toString() + " is about " + beacon.getDistance() + " meters away.");
                        }
                    }
                }
            }
        });

        try {
            //beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            Identifier identifier1 = Identifier.parse("52414449-5553-4E45-5457-4F524B53434F");
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", identifier1, null, null));
        } catch (RemoteException e) {    }
    }

    private void logToDisplay(final String line) {
        runOnUiThread(new Runnable() {
            public void run() {
                //TextView textView = (TextView)MainActivity.this.findViewById(R.id.rangingText);
                //textView.append(TAG + ": " + line + "\n");

                messageArray.add(TAG + ": " + line);
                adapter.notifyDataSetChanged();
                ListView listView = (ListView)findViewById(R.id.messageList);
                listView.setSelection(adapter.getCount() - 1);
            }
        });
    }
}
