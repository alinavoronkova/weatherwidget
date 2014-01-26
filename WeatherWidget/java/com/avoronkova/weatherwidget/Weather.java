package com.avoronkova.weatherwidget;

public class Weather {
    public String timeFrom;
    public String timeTo;
    public String forecast;
    public Float precipitation;
    public String windDirection;
    public Float windSpeed;
    public String wind;
    public Float temperature;

    public Weather(String timeFrom, String timeTo, String forecast, Float precipitation,
                   String windDirection, Float windSpeed, String wind, Float temperature) {
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        this.forecast = forecast;
        this.precipitation = precipitation;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.wind = wind;
        this.temperature = temperature;
    }

    public String getTimeFrom() {
        return timeFrom;
    }

    public String getTimeTo() {
        return timeTo;
    }

    public String getForecast() {
        return forecast;
    }

    public Float getPrecipitation() {
        return precipitation;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public Float getWindSpeed() {
        return windSpeed;
    }

    public String getWind() {
        return wind;
    }

    public Float getTemperature() {
        return temperature;
    }
}
