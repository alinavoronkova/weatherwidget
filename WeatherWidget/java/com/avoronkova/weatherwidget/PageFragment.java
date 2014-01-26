package com.avoronkova.weatherwidget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class PageFragment extends Fragment {

    static final String ARGUMENT_PAGE_NUMBER = "arg_page_number";

    String data = null;

    int pageNumber;
    Adapter adapter;

    static PageFragment newInstance(int page) {
        PageFragment pageFragment = new PageFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARGUMENT_PAGE_NUMBER, page);
        pageFragment.setArguments(arguments);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageNumber = getArguments().getInt(ARGUMENT_PAGE_NUMBER);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment, null);

        Context context = getActivity();

        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        wallpaperDrawable.setAlpha( 210 );


        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(wallpaperDrawable);
        } else {
            view.setBackground(wallpaperDrawable);
        }

        SharedPreferences spWeather = context.getSharedPreferences(WeatherService.PREFS_NAME,
                Context.MODE_PRIVATE);

        ArrayList<Weather> forecasts = getForecast(spWeather);

        adapter = new Adapter(context, forecasts);
        ListView listViewForecast = (ListView) view.findViewById(R.id.listViewForecast);
        listViewForecast.setAdapter(adapter);

        return view;
    }

    private ArrayList<Weather> getForecast(SharedPreferences spWeather) {

        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);
        c.add(Calendar.DATE, pageNumber);
        data = sdf.format(c.getTime());

        Log.d(ForecastActivity.LOG_TAG_ACTIVITY, "data = " + data);
        Log.d(ForecastActivity.LOG_TAG_ACTIVITY, "position = " + pageNumber);


        String value = null;
        ArrayList<Integer> ids = new ArrayList<Integer>();
        int size = spWeather.getInt("count", 0);

        for (int i = 0; i < size; i++) {
            value = spWeather.getString("timeFrom" + (i), "");
            if (value.contains(data)) {
                ids.add(i);
            }
        }

        String timeFrom = null, timeTo = null, forecast = null, windDirection = null, wind = null;
        Float precipitation = null, windSpeed = null, temperature = null;

        ArrayList<Weather> res = new ArrayList<Weather>();

        for (int id : ids) {
            timeFrom = spWeather.getString("timeFrom" + id, "");
            timeTo = spWeather.getString("timeTo" + id, "");
            forecast = spWeather.getString("forecast" + id, "");
            precipitation = spWeather.getFloat("precipitation" + id, 0);
            windDirection = spWeather.getString("windDirection" + id, "");
            windSpeed = spWeather.getFloat("windSpeed" + id, 0);
            wind = spWeather.getString("wind" + id, "");
            temperature = spWeather.getFloat("temperature" + id, 0);
            res.add(new Weather(timeFrom, timeTo, forecast, precipitation, windDirection, windSpeed,
                    wind, temperature));
        }
        return res;
    }
}
