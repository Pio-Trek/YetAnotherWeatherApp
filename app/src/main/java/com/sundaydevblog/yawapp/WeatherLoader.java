package com.sundaydevblog.yawapp;

import android.content.AsyncTaskLoader;
import android.content.Context;

/**
 * Loads current weather and forecast data by using an AsyncTask to perform the
 * network request to the given URL.
 */

public class WeatherLoader extends AsyncTaskLoader<WeatherModel> {

    private static final String TAG = WeatherLoader.class.getName();

    /**
     * Query URL
     */
    private String mUrl;

    /**
     * Constructs a new {@link WeatherLoader}.
     *
     * @param context of the activity
     * @param mUrl    to load data from
     */
    public WeatherLoader(Context context, String mUrl) {
        super(context);
        this.mUrl = mUrl;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    /**
     * This is on a background thread.
     */
    @Override
    public WeatherModel loadInBackground() {
        if (mUrl == null) {
            return null;
        }

        // Perform the network request, parse the response, and extract current weather and forecast.
        return HttpHandler.fetchData(mUrl);
    }
}
