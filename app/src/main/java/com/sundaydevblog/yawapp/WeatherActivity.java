package com.sundaydevblog.yawapp;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.constraint.Group;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

/**
 * Displays information about a weather.
 */

public class WeatherActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<WeatherModel> {

    public static final String TAG = WeatherActivity.class.getSimpleName();

    /**
     * URL to query the APIXU.com dataset for weather information
     */
    // TODO - insert your the APIXU.com API KEY here
    private static final String API_KEY = "";


    private ProgressBar mLoadingSpinner;
    private EditText mSearchInput;
    private TextView mEmptyStateTextView;
    private LoaderManager mLoaderManager;
    private Group mWeatherDetailsGroup;
    private Group mErrorGroup;
    private String cityName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_activity);

        /*
          Initialising default Views
         */
        Group mInputGroup = findViewById(R.id.input_group);
        mInputGroup.setVisibility(View.GONE);
        mErrorGroup = findViewById(R.id.error_group);
        mWeatherDetailsGroup = findViewById(R.id.weather_details_group);
        mWeatherDetailsGroup.setVisibility(View.GONE);
        mLoadingSpinner = findViewById(R.id.loading_indicator);
        mLoadingSpinner.setVisibility(View.GONE);
        mEmptyStateTextView = findViewById(R.id.empty_view);

        // Check if API key exists
        if (API_KEY.isEmpty()) {

            // Set empty state text to display "Please obtain your API KEY first from APIXU.com"
            mEmptyStateTextView.setText(R.string.no_api_key);

            // Check internet connectivity
        } else if (!NetworkCheck.isOnline(this)) {

            // Set empty state text to display "No internet connection."
            mEmptyStateTextView.setText(R.string.no_internet);

        } else {
            // Hiding all error messages and weather Views
            mErrorGroup.setVisibility(View.GONE);
            mInputGroup.setVisibility(View.VISIBLE);
            Button mSearchButton = findViewById(R.id.search_button);
            mSearchInput = findViewById(R.id.search_input);
            mLoaderManager = getLoaderManager();

            mSearchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mErrorGroup.setVisibility(View.GONE);
                    mLoadingSpinner.setVisibility(View.VISIBLE);
                    cityName = mSearchInput.getText().toString();
                    if (TextUtils.isEmpty(cityName)) {
                        Toast.makeText(WeatherActivity.this, "Type the city name", Toast.LENGTH_SHORT).show();

                    } else {
                        // Restart a Loader to refresh data
                        mLoaderManager.restartLoader(1, null, WeatherActivity.this);
                        mLoaderManager.initLoader(1, null, WeatherActivity.this);
                    }

                    // Hide keyboard when button is pressed.
                    dismissKeyboard(WeatherActivity.this);
                }
            });

            // Deliver results after screen rotation
            if (mLoaderManager.getLoader(1) != null) {
                mLoaderManager.initLoader(1, null, this);
            }

        }

    }

    /**
     * Update the screen to display information from the given {@link WeatherModel}.
     */
    private void updateUi(WeatherModel weather) {

        // Display the city name in the UI.
        TextView city = findViewById(R.id.city_name);
        city.setText(weather.getCityName());

        // Display the country name in the UI.
        TextView country = findViewById(R.id.country);
        country.setText(weather.getCountry());

        // Display the current temperature inside the circle that changes
        // colours depends on temperature heights.
        TextView tempCelsius = findViewById(R.id.temp);
        GradientDrawable tempCircle = (GradientDrawable) tempCelsius.getBackground();
        int tempColor = getTemperatureColor(weather.getTemp());
        tempCircle.setColor(tempColor);
        tempCelsius.setText(getString(R.string.temp_degree, Integer.toString(weather.getTemp())));

        // Display the weather condition icon in the UI.
        ImageView conditionIcon = findViewById(R.id.condition_icon);
        Picasso.with(WeatherActivity.this)
                .load("http:" + weather.getConditionIconUrl())
                .into(conditionIcon);

        // Display the condition text in the UI.
        TextView conditionText = findViewById(R.id.condition_text);
        conditionText.setText(weather.getConditionText());

        // Display the humidity value in the UI.
        TextView humidity = findViewById(R.id.humidity);
        humidity.setText(getString(R.string.humidity, Integer.toString(weather.getHumidity())));

        // Display the wind value in the UI.
        TextView windKph = findViewById(R.id.wind);
        windKph.setText(getString(R.string.wind, weather.getWindDirection(), Double.toString(weather.getWindKph())));

        // Display the pressure value in the UI.
        TextView pressureMb = findViewById(R.id.pressure);
        pressureMb.setText(getString(R.string.pressure, Double.toString(weather.getPressureMb())));

        // Display the visibility value in the UI.
        TextView visibilityKm = findViewById(R.id.visibility);
        visibilityKm.setText(getString(R.string.visibility, Double.toString(weather.getVisibilityKm())));

        // Update forecast section.
        Resources r = getResources();
        String name = getPackageName();

        // Create an array of integers for resource IDs for forecast day name, condition icon and
        // temperature (4 days).
        int[] resId = new int[4];

        for (int i = 0; i < 4; i++) {

            resId[i] = r.getIdentifier("forecast_day" + (i + 1) + "_name", "id", name);
            TextView forecastDayName = findViewById(resId[i]);

            // Get the day name from from forecast array at current position.
            forecastDayName.setText(weather.getForecastModelArray().get(i).getDayName());

            resId[i] = r.getIdentifier("forecast_day" + (i + 1) + "_icon", "id", name);

            // Get the condition icon URL from from forecast array at current position.
            ImageView forecastConditionIcon = findViewById(resId[i]);
            Picasso.with(WeatherActivity.this)
                    .load("http:" + weather.getForecastModelArray().get(i).getConditionIconUrl())
                    .into(forecastConditionIcon);

            // Get the temperature from forecast array and display it inside the circle that changes
            // colours depends on temperature heights.
            resId[i] = r.getIdentifier("forecast_day" + (i + 1) + "_temp", "id", name);
            TextView tempForecast = findViewById(resId[i]);
            GradientDrawable tempCircleForecast = (GradientDrawable) tempForecast.getBackground();
            int tempColorForecast = getTemperatureColor(weather.getForecastModelArray().get(i).getTemp());
            tempCircleForecast.setColor(tempColorForecast);
            tempForecast.setText(getString(R.string.temp_degree, Integer.toString(weather.getForecastModelArray().get(i).getTemp())));

            mLoadingSpinner.setVisibility(View.GONE);
            mWeatherDetailsGroup.setVisibility(View.VISIBLE);
        }
    }


    /**
     * Return the colour for the temperature circle based on the temperature heights
     */
    private int getTemperatureColor(int temp) {
        int tempColorResourceId;

        if (temp <= -21)
            tempColorResourceId = R.color.tempM21;
        else if (-20 <= temp && temp <= -16)
            tempColorResourceId = R.color.tempM20_M16;
        else if (-15 <= temp && temp <= -11)
            tempColorResourceId = R.color.tempM15_M11;
        else if (-10 <= temp && temp <= -6)
            tempColorResourceId = R.color.tempM10_M6;
        else if (-5 <= temp && temp <= -1)
            tempColorResourceId = R.color.tempM5_M1;
        else if (0 <= temp && temp <= 4)
            tempColorResourceId = R.color.temp0_P4;
        else if (5 <= temp && temp <= 9)
            tempColorResourceId = R.color.tempP5_P9;
        else if (10 <= temp && temp <= 14)
            tempColorResourceId = R.color.tempP10_P14;
        else if (15 <= temp && temp <= 19)
            tempColorResourceId = R.color.tempP15_P19;
        else if (20 <= temp && temp <= 24)
            tempColorResourceId = R.color.tempP20_P24;
        else if (25 <= temp && temp <= 29)
            tempColorResourceId = R.color.tempP25_P29;
        else if (30 <= temp && temp <= 34)
            tempColorResourceId = R.color.tempP30_P34;
        else if (temp >= 35)
            tempColorResourceId = R.color.tempP35;
        else
            tempColorResourceId = R.color.colorAccent;

        return ContextCompat.getColor(WeatherActivity.this, tempColorResourceId);
    }

    @Override
    public Loader<WeatherModel> onCreateLoader(int i, Bundle bundle) {
        // Create a new loader for the given URL, API Key and city name
        return new WeatherLoader(this, String.format("http://api.apixu.com/v1/forecast.json?key=%s&days=5&q=%s", API_KEY, cityName));
    }

    @Override
    public void onLoadFinished(Loader<WeatherModel> loader, WeatherModel model) {
        // If {@link WeatherModel} object is empty then hide the weather UI and display error message.
        if (model == null) {
            mLoadingSpinner.setVisibility(View.GONE);
            mWeatherDetailsGroup.setVisibility(View.GONE);
            mErrorGroup.setVisibility(View.VISIBLE);
            mEmptyStateTextView.setText(R.string.location_no_found);
        } else {

            // If the model is valid then populate it in the UI.
            updateUi(model);
        }
    }


    // Force to hide keyboard using InputMethodManager in current Activity view.
    private void dismissKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != activity.getCurrentFocus())
            if (imm != null) {
                imm.hideSoftInputFromWindow(activity.getCurrentFocus()
                        .getApplicationWindowToken(), 0);
            }
    }

    @Override
    public void onLoaderReset(Loader<WeatherModel> loader) {
    }
}