package com.avoronkova.weatherwidget;

import android.annotation.TargetApi;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class ConfigActivity extends Activity {

    public final static String WIDGET_PREF = "WIDGET_PREF";
    public final static String WIDGET_FORECAST = "widget_forecast";


    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    Intent resultValue;
    SharedPreferences sp;
    RadioButton rb_forecast;
    RadioButton rb_forecast_hour_by_hour;
    String forecastXML;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // извлекаем ID конфигурируемого виджета
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // и проверяем его корректность
        if (widgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        // формируем intent ответа
        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);

        // отрицательный ответ
        setResult(RESULT_CANCELED, resultValue);

        sp = getSharedPreferences(WIDGET_PREF, MODE_PRIVATE);

        setContentView(R.layout.config);

        RadioGroup radiogroup = (RadioGroup) findViewById(R.id.radioGroup);
        rb_forecast = (RadioButton) findViewById(R.id.radioButton);
        rb_forecast_hour_by_hour = (RadioButton) findViewById(R.id.radioButton2);
    }

    public void onClick(View v) {
        Log.d(MyWidget.LOG_TAG, "Config Activity :: onClick");

        if (rb_forecast.isChecked()) {
            forecastXML = "forecast.xml";
        } else if (rb_forecast_hour_by_hour.isChecked()) {
            forecastXML = "forecast_hour_by_hour.xml";
        }

        Log.d(MyWidget.LOG_TAG, "Config Activity :: forecast : " + forecastXML);

        SharedPreferences sp = this.getSharedPreferences(ConfigActivity.WIDGET_PREF,
                Context.MODE_PRIVATE);
        sp.edit().putString(ConfigActivity.WIDGET_FORECAST, forecastXML).commit();

        Intent updateWeatherIntent = new Intent(this, MyWidget.class);
        updateWeatherIntent.setAction(MyWidget.ACTION_WEATHER_CHANGE);
        updateWeatherIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        sendBroadcast(updateWeatherIntent);

        setResult(RESULT_OK, resultValue);
        finish();
    }
}


















