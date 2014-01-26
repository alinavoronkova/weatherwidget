package com.avoronkova.weatherwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ForecastActivity extends FragmentActivity {

    public static final String LOG_TAG_ACTIVITY = "myLogs";

    ViewPager pager;
    PagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forecast);

        pager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                Log.d(LOG_TAG_ACTIVITY, "onPageSelected, position = " + position);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(MyWidget.ACTION_WEATHER);
        registerReceiver(receiver, filter);
    }

    private class MyFragmentPagerAdapter extends FragmentPagerAdapter {

        public MyFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getCurrentDate(position);
        }

        @Override
        public Fragment getItem(int position) {
            return PageFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            Context context = getApplicationContext();
            SharedPreferences spWeather = context.getSharedPreferences(WeatherService.PREFS_NAME,
                    Context.MODE_PRIVATE);

            int lastID = spWeather.getInt("count", 0) - 1;

            if (lastID == -1) {
                return 0;
            }

            SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
            String firstDateStr = spWeather.getString("timeFrom" + 0, "");
            String lastDateStr = spWeather.getString("timeFrom" + lastID, "");


            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            long firstTime = 0;//cal.getTimeInMillis();
            long lastTime = 0;

            try {
                firstTime = sdf.parse(firstDateStr).getTime();
                lastTime = sdf.parse(lastDateStr).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            int diffInDays = (int) Math.ceil( (lastTime - firstTime) //firstDate.getTime()
                    / (1000.0 * 60.0 * 60.0 * 24.0) );

            return diffInDays;
        }

        private String getCurrentDate(int position) {
            Date currentDate = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("E, MMMM dd", Locale.ENGLISH);
            Calendar cal = Calendar.getInstance();
            cal.setTime(currentDate);
            cal.add(Calendar.DATE, position);

            return sdf.format(cal.getTime());
        }

    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(MyWidget.LOG_TAG, "ForecastActivity::BrRec::onRecive");
            pagerAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onDestroy () {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

}