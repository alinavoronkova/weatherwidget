package com.avoronkova.weatherwidget;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.app.AlarmManager.RTC;
import static android.app.AlarmManager.RTC_WAKEUP;

public class MyWidget extends AppWidgetProvider {

    static final String ACTION_WEATHER_CHANGE = "com.avoronkova.weatherwidget.change_count";
    static final String ACTION_WEATHER = "com.avoronkova.weatherwidget.action_weather";
    static final String ACTION_TIME = "com.avoronkova.weatherwidget.action_time";
    static final String ACTION_START = "com.avoronkova.weatherwidget.action_start";
    static final String ACTION_END = "com.avoronkova.weatherwidget.action_end";

    public static final Map<String, String> forecastMap = new HashMap<String, String>();
    static {
        forecastMap.put("Cloudy", "cloudy");
        forecastMap.put("Clear sky", "clear_sky");
        forecastMap.put("Fair", "fair");
        forecastMap.put("Fog", "fog");
        forecastMap.put("Heavy rain", "heavy_rain");
        forecastMap.put("Partly cloudy", "partly_cloudy");
        forecastMap.put("Rain", "rain");
        forecastMap.put("Rain and thunder", "rain_and_thunder");
        forecastMap.put("Rain showers", "rain_showers");
        forecastMap.put("Rain showers with thunder", "rain_showers_with_thunder");
        forecastMap.put("Sleet", "sleet");
        forecastMap.put("Sleet and thunder", "sleet_and_thunder");
        forecastMap.put("Sleet showers and thunder", "sleet_showers_and_thunder");
        forecastMap.put("Sleet showers", "sleet_showers");
        forecastMap.put("Snow", "snow");
        forecastMap.put("Snow and thunder", "snow_and_thunder");
        forecastMap.put("Snow showers and thunder", "snow_showers_and_thunder");
        forecastMap.put("Snow showers", "snow_showers");
        forecastMap.put("Sun", "sun");
    }

    public static final  String LOG_TAG = "myWeather";
    static final int big_font_size = 100;
    static final int small_font_size = 15;
    static final int middle_font_size = 20;
    static final Locale locale = Locale.ENGLISH;


    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(LOG_TAG, "MyWidget::onEnabled");

        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long curTime = System.currentTimeMillis();
        long updateTime = curTime + 12 * 60 * 60 * 1000;
        Log.d(LOG_TAG, "updateTime: " + updateTime);

//        context.startService(getWeatherIntent(context));

        am.setRepeating(RTC_WAKEUP, updateTime, 1000 * 60 * 60 * 12,
                 getWeatherService(context));

        am.setRepeating(RTC, curTime - curTime % (1000*60) + 1000*60, 1000 * 60, getTimeService(context));
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        // Update instances
        try {
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    static int i = 0;

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetID) {
        Log.d(LOG_TAG, "updateWidget");
        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);

        // set simple date format
        SimpleDateFormat sdfHHmm = new SimpleDateFormat("HH:mm", locale);

        // set time and date
        Date currentTimeDate = new java.util.Date();
        String currentTime = sdfHHmm.format(new Date());
        String date =   new SimpleDateFormat("E, MMMM dd", locale).format(currentTimeDate);
        widgetView.setImageViewBitmap(R.id.imageTime, MyWidget.convertToImg(currentTime,
                context, big_font_size));
        widgetView.setImageViewBitmap(R.id.imageDate, convertToImg(date.toUpperCase(),
                context, middle_font_size));

        SharedPreferences spWeather = context.getSharedPreferences(WeatherService.PREFS_NAME,
                Context.MODE_PRIVATE);

        try {

            if (!spWeather.getString("city","").equalsIgnoreCase("")) {
                String city = spWeather.getString("city","");
                int count = spWeather.getInt("count",0);

                ArrayList<Date> timeFrom = new ArrayList<Date>();
                ArrayList<Date> timeTo = new ArrayList<Date>();
                ArrayList<Float> temperatureArray = new ArrayList<Float>();
                ArrayList<String> forecasts = new ArrayList<String>();

                SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                for (int i = 0; i < count; i++) {
                    timeFrom.add(sdf.parse(spWeather.getString("timeFrom" + i, "")));
                    timeTo.add(sdf.parse(spWeather.getString("timeTo" + i, "")));
                    temperatureArray.add(spWeather.getFloat("temperature" + i, 0));
                    forecasts.add(spWeather.getString("forecast" + i,""));
                }

                Date nextTime = sdf.parse(spWeather.getString("timeFrom1", ""));
                Date nightTime = sdfHHmm.parse("23:00:00");
                Date morningTime = sdfHHmm.parse("6:00:00");

                // set default values
                float temperature = temperatureArray.get(0);
                float nextTemperature = temperatureArray.get(1);
                String forecast = forecasts.get(0);

                for (int i = 0; i < count; i++) {
                    if ( timeFrom.get(i).before(currentTimeDate) &&
                            timeTo.get(i).after(currentTimeDate)) {
                        temperature = temperatureArray.get(i);
                        nextTemperature = temperatureArray.get(i + 1);
                        nextTime = timeFrom.get(i + 1);
                        forecast = forecasts.get(i);
                    }
                }

                String nextTimeStr = sdfHHmm.format(nextTime);
                int id;
                if ( morningTime.before(sdfHHmm.parse(currentTime)) &&
                        nightTime.after(sdfHHmm.parse(currentTime))) {
                    id = context.getResources().getIdentifier(forecastMap.get(forecast)
                            + "_d", "drawable", context.getPackageName());
                } else {
                    id = context.getResources().getIdentifier(forecastMap.get(forecast)
                            + "_n", "drawable", context.getPackageName());
                }

                String country = spWeather.getString("country", "");
                String uri = "http://yr.no/place/" + country +"/" + city + "/" + city + "/";
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                PendingIntent webPendingIntent = PendingIntent.getActivity(context, 0, webIntent, 0);

                widgetView.setImageViewBitmap(R.id.imageCity, convertToImg(city, context,
                        small_font_size));
                widgetView.setImageViewBitmap(R.id.imageForecast, convertToImg(forecast.toLowerCase(),
                        context, small_font_size));
                widgetView.setImageViewBitmap(R.id.imageMore, convertToImg("yr.no", context,
                        small_font_size));
                widgetView.setOnClickPendingIntent(R.id.imageMore, webPendingIntent);

                widgetView.setImageViewResource(R.id.image, id);

                widgetView.setImageViewBitmap(R.id.imageTemperature, convertToImg(temperature
                        + "°C", context, middle_font_size));
                widgetView.setImageViewBitmap(R.id.imageNextTemperature, convertToImg(nextTemperature
                        + "°C, " + nextTimeStr, context, small_font_size));

                Intent forecastChange = new Intent(context, ConfigActivity.class);
                forecastChange.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
                PendingIntent forecastChangePendingIntent = PendingIntent.getActivity(context,
                        widgetID, forecastChange, 0);
                widgetView.setOnClickPendingIntent(R.id.image, forecastChangePendingIntent);

                Intent weatherActivityIntent = new Intent(context, ForecastActivity.class);
//            weatherActivityIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
                PendingIntent weatherPendingIntent = PendingIntent.getActivity(context, 0,
                        weatherActivityIntent, 0);
                widgetView.setOnClickPendingIntent(R.id.imageDate, weatherPendingIntent);

            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // Вызов будильника
        Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
        PendingIntent alarmPendingIntent = PendingIntent.getActivity(context, 0, alarmIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.imageTime, alarmPendingIntent);

        // Обновляем виджет
        appWidgetManager.updateAppWidget(widgetID, widgetView);
    }

    private PendingIntent getTimeService(Context context) {
        Intent timeIntent = new Intent(context, MyWidget.class);
        timeIntent.setAction(ACTION_TIME);
//        timeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        return PendingIntent.getBroadcast(context, 0, timeIntent, 0);
    }

    private PendingIntent getWeatherService(Context context) {
        return PendingIntent.getService(context, 0, getWeatherIntent(context), 0);
    }

    private Intent getWeatherIntent(Context context) {
        Intent weatherIntent = new Intent(context, WeatherService.class);
        weatherIntent.setAction(ACTION_WEATHER);
        return weatherIntent;
    }

    private static Calendar getCalendar(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);

        Log.d( LOG_TAG, "MyWidget::onReceive: " + intent.getAction() );

        if (intent.getAction().equalsIgnoreCase(ACTION_WEATHER_CHANGE)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widget = new ComponentName(context, MyWidget.class);
            for(int id : appWidgetManager.getAppWidgetIds(widget)) {
                updateWidget(context, appWidgetManager, id);
            }
            context.startService(getWeatherIntent(context));
        }

        else if (intent.getAction().equalsIgnoreCase(ACTION_WEATHER)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName widget = new ComponentName(context, MyWidget.class);
            for(int id : appWidgetManager.getAppWidgetIds(widget)) {
                updateWidget(context, appWidgetManager, id);
            }
        }

        else if (intent.getAction().equalsIgnoreCase(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE );
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetInfo != null && activeNetInfo.isConnectedOrConnecting();
            Log.d(LOG_TAG, "isConnected" + isConnected);

            if ( isConnected ) {
                context.startService(getWeatherIntent(context));
            }
        }

        else if (intent.getAction().equalsIgnoreCase(ACTION_TIME)) {
            Log.d(MyWidget.LOG_TAG, "MyWidget::ACTION_TIME");
            SimpleDateFormat sdf;
            String currentTime = new Date().toString();

            try {
                sdf = new SimpleDateFormat("HH:mm", locale);
                currentTime = sdf.format(new Date());
            } catch (java.lang.NumberFormatException e) {
                Log.d(MyWidget.LOG_TAG, "TimeService::NumberFormatException");
            }

            RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
            widgetView.setImageViewBitmap(R.id.imageTime, MyWidget.convertToImg(currentTime,
                    context, MyWidget.big_font_size));

            ComponentName widget = new ComponentName(context, MyWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.partiallyUpdateAppWidget(manager.getAppWidgetIds(widget), widgetView);
        }

        else if (intent.getAction().equalsIgnoreCase(ACTION_START)) {
            RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
            widgetView.setBoolean(R.id.imageDate, "setEnabled", false);
            widgetView.setViewVisibility(R.id.progress, View.VISIBLE);
            widgetView.setViewVisibility(R.id.image, View.INVISIBLE);

            ComponentName widget = new ComponentName(context, MyWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.partiallyUpdateAppWidget(manager.getAppWidgetIds(widget), widgetView);
        }

        else if (intent.getAction().equalsIgnoreCase(ACTION_END)) {
            RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
            widgetView.setBoolean(R.id.imageDate, "setEnabled", true);
            widgetView.setViewVisibility(R.id.progress, View.INVISIBLE);
            widgetView.setViewVisibility(R.id.image, View.VISIBLE);

            ComponentName widget = new ComponentName(context, MyWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.partiallyUpdateAppWidget(manager.getAppWidgetIds(widget), widgetView);
        }
    }


    public static Bitmap convertToImg(String text, Context context, int size) {

        Typeface tf = Typeface.createFromAsset(context.getAssets(),"fonts/Limelight.ttf");

        final float scale = context.getResources().getDisplayMetrics().density;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setSubpixelText(true);
        paint.setTypeface(tf);
        paint.setColor(Color.WHITE);
        paint.setTextSize(size*scale);
        paint.setShadowLayer(2.0f, 2.0f, 2.0f, Color.BLACK);

        float ascent = -paint.ascent();
        float descent = paint.descent();
        int height = (int)(ascent + descent);
        int width = (int)(paint.measureText(text) + 0.5f);

        Bitmap btmText = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas cnvText = new Canvas(btmText);

        cnvText.drawText(text, 0, ascent, paint);
        return btmText;
    }


    @Override
    public void onDisabled(Context context) {
        Log.d(LOG_TAG, "MyWidget::onDisabled");
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getTimeService(context));
        am.cancel(getWeatherService(context));
    }
}
