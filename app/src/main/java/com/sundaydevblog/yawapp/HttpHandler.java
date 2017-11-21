package com.sundaydevblog.yawapp;

import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper methods related to requesting and receiving weather data from APIXU.com
 * This class is only meant to hold static variables and methods, which can be accessed
 * directly from the class name HttpHandler (and an object instance of HttpHandler is not needed).
 */

public class HttpHandler {

    private static final String TAG = HttpHandler.class.getSimpleName();

    // Private constructor to prevent a class from being instantiated.
    private HttpHandler() {
    }

    /**
     * Query the APIXU.com weather dataset.
     */
    public static WeatherModel fetchData(String requestUrl) {
        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(TAG, "Problem making the HTTP request.", e);
        }

        // Extract relevant fields from the JSON response, create and return an {@link Event} object
        return extractFeatureFromJson(jsonResponse);
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException exception) {
            Log.e(TAG, "Error with creating URL", exception);
            return null;
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response (serialize).
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);

                // Error code 400 when input location is not found.
            } else if (urlConnection.getResponseCode() == 400) {
                Log.e(TAG, "Error response code: " + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage());

            } else {
                Log.e(TAG, "Error response code: " + urlConnection.getResponseCode());
            }

        } catch (IOException e) {
            Log.e(TAG, "Problem retrieving the weather JSON results.", e);

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Return an {@link WeatherModel} object by parsing out information
     * about the first earthquake from the input weatherJSON string.
     */
    private static WeatherModel extractFeatureFromJson(String weatherJSON) {
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(weatherJSON)) {
            return null;
        }

        // Try to parse the JSON response string.
        try {
            // Create a JSONObject from the JSON response string
            JSONObject locationJsonResponse = new JSONObject(weatherJSON);

            // Get the city location from JSONObject.
            JSONObject location = locationJsonResponse.getJSONObject("location");
            String cityName = location.getString("name");
            String country = location.getString("country");

            // Get the current weather data from JSONObject.
            JSONObject currentJsonResponse = new JSONObject(weatherJSON);
            JSONObject current = currentJsonResponse.getJSONObject("current");
            int tempC = (int) Math.round(current.getDouble("temp_c"));
            int humidity = current.getInt("humidity");
            double windKph = current.getDouble("wind_kph");
            String windDirection = current.getString("wind_dir");
            double pressureMb = current.getDouble("pressure_mb");
            double visibilityKm = current.getDouble("vis_km");

            // Get the current weather condition description and icon URL from JSONObject.
            JSONObject condition = current.getJSONObject("condition");
            String conditionIcon = condition.getString("icon");
            String conditionText = condition.getString("text");

            // Get forecast data from JSONArray.
            JSONObject forecastJsonResponse = new JSONObject(weatherJSON);
            JSONObject forecast = forecastJsonResponse.getJSONObject("forecast");
            JSONArray forecastDay = forecast.getJSONArray("forecastday");

            // Create an empty SparseArray to add forecast data for all (four) days.
            SparseArray<ForecastModel> forecastModelArray = new SparseArray<>();

            // For each forecast in JSONArray create an {@link ForecastModel} object.
            // Declare loop control variable as "i=1" because "0" in the "forecastday"
            // JSONArray is forecast for current day, "1" is for a next day.
            try {
                for (int i = 1; i < forecastDay.length(); i++) {

                    // Get the position of a single forecast for a day.
                    JSONObject index = forecastDay.getJSONObject(i);

                    // Extract date in Unix time format and convert to day name.
                    long timeInMilliseconds = index.getLong("date_epoch");
                    String forecastDayName = formatDateTime(timeInMilliseconds);

                    // Get forecast data from JSONObject.
                    JSONObject day = index.getJSONObject("day");
                    int forecastTemp = (int) Math.round(day.getDouble("avgtemp_c"));

                    // Get forecast condition description and icon URL from JSONObject.
                    JSONObject forecastCondition = day.getJSONObject("condition");
                    String forecastConditionIcon = forecastCondition.getString("icon");

                    // Create a new {@link ForecastMode} object.
                    ForecastModel forecastModel = new ForecastModel(forecastDayName, forecastTemp, forecastConditionIcon);

                    // Add a new {@link ForecastMode} object to the array.
                    forecastModelArray.put(i - 1, forecastModel);

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Create a new {@link WeatherModel} object
            return new WeatherModel(cityName, country, tempC, conditionIcon, conditionText, humidity, windKph, windDirection, pressureMb, visibilityKm, forecastModelArray);

        } catch (JSONException e) {
            Log.e(TAG, "Problem parsing the weather JSON results", e);
        }
        return null;
    }


    /**
     * Convert Unix epoch time to day name in week ("Mon", "Tue" etc.).
     */
    private static String formatDateTime(long timeInMilliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat(("E"), Locale.ENGLISH);
        return formatter.format(new Date(timeInMilliseconds * 1000));
    }
}
