package com.avoronkova.weatherwidget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Adapter extends BaseAdapter {
    Context context;
    LayoutInflater lInflater;
    ArrayList<Weather> forecast;

    Adapter(Context context, ArrayList<Weather> forecast) {
        this.context = context;
        this.forecast = forecast;
        lInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public int getCount() {
        return forecast.size();
    }


    @Override
    public Object getItem(int position) {
        return forecast.get(position);
    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(R.layout.item, parent, false);
        }

        Weather weather = getWeather(position);

        SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        SimpleDateFormat sdfHHmm = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        Date timeFrom = null, timeTo = null, morningTime = null, nightTime = null;
        String average = null;
        int id = 0;

        try {
            timeFrom = sdf.parse(weather.getTimeFrom());
            timeTo = sdf.parse(weather.getTimeTo());
            morningTime = sdfHHmm.parse("6:00");
            nightTime = sdfHHmm.parse("23:00");

            long diff = timeTo.getTime() - timeFrom.getTime();
            Date averageDate = new Date(timeFrom.getTime() + diff/2);
            average = sdfHHmm.format(averageDate);

            if ( morningTime.before(sdfHHmm.parse(average)) && nightTime.after(sdfHHmm.parse(average))) {
                id = context.getResources().getIdentifier(MyWidget.forecastMap.get(weather.getForecast())
                        + "_d", "drawable", context.getPackageName());
            } else {
                id = context.getResources().getIdentifier(MyWidget.forecastMap.get(weather.getForecast())
                        + "_n", "drawable", context.getPackageName());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        TextView tvTime = ((TextView) view.findViewById(R.id.tvTimeItem));
        tvTime.setText(sdfHHmm.format(timeFrom) + " -\n"  + sdfHHmm.format(timeTo));

        ImageView imForecast = ((ImageView) view.findViewById(R.id.imForecastItem));
        imForecast.setImageResource(id);

        TextView tvTemp = ((TextView) view.findViewById(R.id.tvTemperatureItem));
        tvTemp.setText(Math.round(weather.getTemperature()) + "°");

        TextView tvPresip = ((TextView) view.findViewById(R.id.tvPrecipitationItem));
        tvPresip.setText(weather.getPrecipitation() + " mm");

        TextView tvWind = ((TextView) view.findViewById(R.id.tvWindItem));
        tvWind.setText(weather.getWindSpeed() + " m/s");

        float angle = ForecastActivity.windMap.get(weather.getWindDirection());
        ImageView imWind = ((ImageView) view.findViewById(R.id.imWindItem));
        imWind.setImageResource(R.drawable.wind);

        RotateAnimation anim = new RotateAnimation(0f, angle, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setInterpolator(new LinearInterpolator());
        anim.setDuration(0);
        anim.setFillEnabled(true);
        anim.setFillAfter(true);
        imWind.startAnimation(anim);

        return view;
    }


    Weather getWeather(int position) {
        return ((Weather) getItem(position));
    }
}
