package com.amitlab.beaconreceiveralt;

import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

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

                // Call API exit method
                String exit_path = getString(R.string.api_exit_path);
                new ExecuteTask().execute("exit", exit_path);
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

                            // Call API enter method
                            String enter_path = getString(R.string.api_enter_path);
                            new ExecuteTask().execute("enter", enter_path);
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

    class ExecuteTask extends AsyncTask<String, Integer, String> //params, progress, result
    {
        String path_type = "";
        @Override
        protected String doInBackground(String... params) {
            path_type = params[0];
            String res = PostData(params);
            return res;
        }

        @Override
        protected void onPostExecute(String result) {
            // This method has access to UI thread
            String abc = result;

            if (abc == "200" )
            {
                if(path_type == "enter") {
                    logToDisplay("API: Device Enter");
                }
                else if(path_type == "exit") {
                    logToDisplay("API: Device Exit");
                }
            }
            else
            {
                logToDisplay("API: Unable to connect");
            }
        }
    }

    public String PostData(String[] values) {
        String s = "";

        try
        {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(values[1]);

            //List<NameValuePair> list = new ArrayList<NameValuePair>();
            //list.add(new BasicNameValuePair("name", values[0]));
            //list.add(new BasicNameValuePair("pass",values[1]));
            //httpPost.setEntity(new UrlEncodedFormEntity(list));

            HttpResponse httpResponse = httpClient.execute(httpPost);

            HttpEntity httpEntity = httpResponse.getEntity();
            s = readResponse(httpResponse);
        }
        catch(Exception exception) {}
        return s;
    }

    public String readResponse(HttpResponse res) {
        String return_text = "";

//        InputStream is = null;

//        try {
//            is = res.getEntity().getContent();
//            BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(is));
//            String line = "";
//            StringBuffer sb = new StringBuffer();
//            while ((line = bufferedReader.readLine())!=null)
//            {
//                sb.append(line);
//            }
//            return_text = sb.toString();
//        } catch (Exception e)
//        {
//
//        }

        try {
            int statusCode = res.getStatusLine().getStatusCode();
            if(statusCode == 200)
                return_text = "200";
        }
        catch (Exception e) {

        }
        return return_text;
    }
}
