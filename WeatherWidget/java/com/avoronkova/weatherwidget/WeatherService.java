package com.avoronkova.weatherwidget;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeatherService extends Service {

    public static final String PREFS_NAME = "WEATHER_PREF";

    ArrayList<Weather> list = new ArrayList<Weather>();
    String country = null;
    String city = null;
    Intent intent;
    String forecastURI;

    private Handler handler = new Handler();
    public static boolean startNotification = false;
    boolean start = false;


    public void onCreate() {
        super.onCreate();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(MyWidget.LOG_TAG, "WeatherService::onStart");

        if (intent == null) {
            return START_STICKY;
        }
        if (start) {
            return START_STICKY;
        }

        Log.d(MyWidget.LOG_TAG, "WeatherService :: intent: " + intent.getAction());

        final Context context = getApplicationContext();
//        GPSTracker mGPS = new GPSTracker(context);
        CurrentLocation currLoc = new CurrentLocation(context);

        SharedPreferences sp = context.getSharedPreferences(ConfigActivity.WIDGET_PREF,
                Context.MODE_PRIVATE);
        forecastURI = sp.getString(ConfigActivity.WIDGET_FORECAST, "forecast.xml");

        Log.d(MyWidget.LOG_TAG, "SP = " + forecastURI);
        Log.d(MyWidget.LOG_TAG, "WeatherService :: startNotification: " + startNotification);

//        if(mGPS.canGetLocation ){
//            Location mLocation = mGPS.getLocation();

        currLoc.getLocation(new CurrentLocation.LocationListener() {
            @Override
            public void onGotLocation(Location loc) {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) && Geocoder.isPresent() && loc != null) {
                    Log.d(MyWidget.LOG_TAG, "WeatherService.GetAddressTask.execute");
                    (new GetAddressTask(context)).execute(loc);
                    start = true;
                    Intent intent = new Intent(MyWidget.ACTION_START);
                    sendBroadcast(intent);
                } else {
                    Log.d(MyWidget.LOG_TAG, "weatherService.GetAddressTask.not execute");
                    stopSelf();
                }
            }

            @Override
            public void onFail() {
                Log.d(MyWidget.LOG_TAG, "WeatherService::not mGPS.canGetLocation");
                if (startNotification) {
                    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                    Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    PendingIntent pIntent = PendingIntent.getActivity(context, 0, locationIntent, 0);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                    builder.setContentTitle("Location Services Not Active");
                    builder.setContentText("Please enable Location Services and GPS");
                    builder.setSmallIcon(R.drawable.ic_launcher);
                    builder.setContentIntent(pIntent);
                    Notification notification;
                    notification = builder.build();

                    notification.flags |= Notification.FLAG_AUTO_CANCEL;

                    nm.notify(1, notification);
                }
                stopSelf();
            }
        });
//        Log.d(MyWidget.LOG_TAG, "WeatherService : getLatitude" + mLocation.getLatitude());


        return START_STICKY;
    }



    private class GetAddressTask extends AsyncTask<Location, Void, String[]> {
        Context mContext;
        public GetAddressTask(Context context) {
            super();
            mContext = context;
        }
        /**
         * Get a Geocoder instance, get the latitude and longitude
         * look up the address, and return it
         *
         * @params params One or more Location objects
         * @return A string containing the address of the current
         * location, or an empty string if no address can be found,
         * or an error message
         */
        @Override
        protected String[] doInBackground(Location... params) {
            Geocoder geocoder = new Geocoder(mContext, Locale.ENGLISH);
//            Log.d(MyWidget.LOG_TAG, "Locale: " + Locale.getDefault());
            // Geocoder geocoder = new Geocoder(mContext, MyWidget.locale);
            // Get the current location from the input parameter list
            Location loc = params[0];
            // Create a list to contain the result address
            List<Address> addresses = null;
            try {
                /*
                 * Return 1 address.
                 */
                addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            } catch (IOException e1) {
                Log.e(MyWidget.LOG_TAG, "IO Exception in getFromLocation()");
                e1.printStackTrace();

                handler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Not connected to Internet",
                                Toast.LENGTH_LONG).show();
                    }
                });
                stopSelf();
                return new String[] {"IO Exception trying to get address"};
            } catch (IllegalArgumentException e2) {
                // Error message to post in the log
                String errorString = "Illegal arguments " +
                        Double.toString(loc.getLatitude()) +
                        " , " +
                        Double.toString(loc.getLongitude()) +
                        " passed to address service";
                Log.e(MyWidget.LOG_TAG, errorString);
                e2.printStackTrace();
                stopSelf();
                return new String[] {errorString};
            }
            // If the reverse geocode returned an address
            if (addresses != null && addresses.size() > 0) {
                Log.e(MyWidget.LOG_TAG, "GetAddressTask:: doInBackground: got addresses");
                // Get the first address
                Address address = addresses.get(0);

                String[] fullAddress = new String[] {address.getCountryName().toString(),
                        address.getLocality().toString()};
                return fullAddress;
            } else {
                stopSelf();
                return new String[] {"No address found"};
            }
        }
        /**
         * A method that's called once doInBackground() completes. Turn
         * off the indeterminate activity indicator and set
         * the text of the UI element that shows the address. If the
         * lookup failed, display the error message.
         */
        @Override
        protected void onPostExecute(String[] address) {
            if(address.length == 1) {
                Log.e(MyWidget.LOG_TAG, "GetAddressTask error: " + address[0]);
                stopSelf();
                return;
            }
            country = address[0];
            city = address[1];

            String url = "http://yr.no/place/" + country + "/" + city + "/" + city + "/" + forecastURI;
            connect(url);
        }
    }

    private void connect(String url) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadWebpage().execute(url);
        } else {
            Log.d(MyWidget.LOG_TAG,"No network connection available");
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "No network connection available",
                            Toast.LENGTH_LONG).show();
                }
            });
            stopSelf();
        }
    }

    private class DownloadWebpage extends AsyncTask<String, Void, ArrayList<Weather>> {

        @Override
        protected ArrayList<Weather> doInBackground(String... urls) {
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                stopSelf();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Weather> result) {
            if (result == null) {
                return;
            }
            SharedPreferences spWeather = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = spWeather.edit();
            editor.clear();
            editor.commit();

            editor.putString("country",country);
            editor.putString("city", city);

            int i = 0;

            for (Weather res : result) {
                editor.putString("timeFrom" + i, res.getTimeFrom());
                editor.putString("timeTo" + i, res.getTimeTo());
                editor.putString("forecast" + i, res.getForecast());
                editor.putFloat("precipitation" + i, res.getPrecipitation());
                editor.putString("windDirection" + i, res.getWindDirection());
                editor.putFloat("windSpeed" + i, res.getWindSpeed());
                editor.putString("wind" + i, res.getWind());
                editor.putFloat("temperature" + i, res.getTemperature());
                i++;
            }
            editor.putInt("count", i);
            editor.commit();

            intent = new Intent(MyWidget.ACTION_WEATHER);
            sendBroadcast(intent);

            Log.d(MyWidget.LOG_TAG, "WeatherService::gotWeather!!!");
            stopSelf();
        }
    }

    private ArrayList<Weather> downloadUrl(String myurl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            is = conn.getInputStream();
            // Parse input stream
            list.clear();
            list = parse(is);

            return list;

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private ArrayList<Weather> parse(InputStream is) {
        String timeFrom = null, timeTo = null;
        String forecast = null;
        Float precipitation = null;
        String windDirection = null;
        Float windSpeed = null;
        String wind = null;
        Float temperature = null;
        try {
            XmlPullParser xpp = prepareXpp(is);
            while (xpp.getEventType()!= XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() != XmlPullParser.START_TAG){
                    xpp.next();
                    continue;
                }
                if (xpp.getName().equals("time")) {
                    timeFrom = xpp.getAttributeValue(0);
                    timeTo = xpp.getAttributeValue(1);

                } else if (xpp.getName().equals("symbol")) {
                    forecast = xpp.getAttributeValue(1);

                } else if (xpp.getName().equals("precipitation")) {
                    precipitation = Float.parseFloat(xpp.getAttributeValue(0));

                } else if (xpp.getName().equals("windDirection")) {
                    windDirection = xpp.getAttributeValue(2);

                } else if (xpp.getName().equals("windSpeed")) {
                    windSpeed = Float.parseFloat(xpp.getAttributeValue(0));
                    wind = xpp.getAttributeValue(1);

                } else if (xpp.getName().equals("temperature")) {
                    temperature = Float.parseFloat(xpp.getAttributeValue(1));
                    // add to list
                    list.add(new Weather(timeFrom, timeTo, forecast, precipitation, windDirection,
                            windSpeed, wind, temperature));
//                    Log.d(MyWidget.LOG_TAG, timeFrom + " " + timeTo + " " + forecast + " "
//                            + precipitation + " "  + windDirection + " "  +windSpeed + " "
//                            + wind + " "  + temperature);
                    timeFrom = null;
                    timeTo = null;
                    forecast = null;
                    precipitation = null;
                    windDirection = null;
                    windSpeed = null;
                    wind = null;
                    temperature = null;
                }
                xpp.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    XmlPullParser prepareXpp(InputStream is) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        xpp.setInput(is, null);
        return xpp;
    }

    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void onDestroy() {
        start = false;
        intent = new Intent(MyWidget.ACTION_END);
        sendBroadcast(intent);
        super.onDestroy();
        Log.d(MyWidget.LOG_TAG, "WeatherService::onDestroy");
    }

}
