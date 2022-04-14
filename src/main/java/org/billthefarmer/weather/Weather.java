////////////////////////////////////////////////////////////////////////////////
//
//  Weather - an android weather app
//
//  Copyright (C) 2022	Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.weather;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.lang.ref.WeakReference;

import java.net.URL;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Weather extends Activity
{
    public static final String TAG = "Weather";

    public static final String GOOGLE_URL =
        "https://www.google.com/search?hl=en&q=weather %s";

    public static final String YAHOO_URL =
        "https://search.yahoo.com/search?p=weather %s";

    public static final String ADDR_FORMAT = "%s, %s, %s";

    public static final String PREF_TEMP = "pref_temp";

    public static final String PREF_DATE = "pref_date";
    public static final String PREF_DESC = "pref_desc";
    public static final String PREF_LOCN = "pref_locn";
    public static final String PREF_CENT = "pref_cent";
    public static final String PREF_FAHR = "pref_fahr";
    public static final String PREF_WIND = "pref_wind";
    public static final String PREF_HUMID = "pref_humid";
    public static final String PREF_PRECIP = "pref_precip";

    public static final String DATE = "date";
    public static final String DESC = "desc";
    public static final String LOCN = "locn";
    public static final String CENT = "cent";
    public static final String FAHR = "fahr";
    public static final String WIND = "wind";
    public static final String HUMID = "humid";
    public static final String PRECIP = "precip";

    public static final String DAYS = "days";
    public static final String DESCS = "descs";
    public static final String DTEMPS = "dtemps";
    public static final String NTEMPS = "ntemps";

    public static final String WOB_DC = "wob_dc";
    public static final String WOB_DF = "wob_df";
    public static final String WOB_DP = "wob_dp";
    public static final String WOB_HM = "wob_hm";
    public static final String WOB_PP = "wob_pp";
    public static final String WOB_TM = "wob_tm";
    public static final String WOB_WC = "wob_wc";
    public static final String WOB_WS = "wob_ws";

    public static final String WOB_DTS = "wob_dts";
    public static final String WOB_LOC = "wob_loc";
    public static final String WOB_TTM = "wob_ttm";

    public static final String Z1VZSB = "Z1VzSb";
    public static final String UW5PK = "uW5pk";
    public static final String WOB_T = "wob_t";

    /*
      Blowing widespread dust
      Clear
      Clear with periodic clouds
      Cloudy
      Fog
      Haze
      Light drizzle
      Light rain showers
      Light snow
      Light thunderstorms and rain
      Mist
      Mostly cloudy
      Mostly sunny
      Partly cloudy
      Patches of fog
      Rain
      Rain and snow
      Scattered showers
      Scattered thunderstorms
      Smoke
      Snow
      Snow showers
      Sunny
      Thunderstorm
      Wind and rain
      Windy
    */

    public static final String DESCRIPTIONS[] =
    {
        "Sunny",
        "Mostly sunny",
        "Partly cloudy",
        "Mostly cloudy",
        "Cloudy",
        "Haze",
        "Mist",
        "Fog",
        "Scattered showers",
        "Light rain showers",
        "Showers",
        "Light rain",
        "Rain",
        "Hail",
        "Rain and snow",
        "Rain and sleet",
        "Snow showers",
        "Light snow",
        "Snow",
        "Thundery showers",
        "Thunder",
        "Clear",
        "Clear with periodic clouds"
    };

    public static final int DAY_IMAGES[] =
    {
        R.drawable.ic_sunny,
        R.drawable.ic_mostly_sunny,
        R.drawable.ic_partly_cloudy,
        R.drawable.ic_mostly_cloudy,
        R.drawable.ic_cloudy,
        R.drawable.ic_haze,
        R.drawable.ic_mist,
        R.drawable.ic_fog,
        R.drawable.ic_scattered_showers,
        R.drawable.ic_scattered_showers,
        R.drawable.ic_showers,
        R.drawable.ic_light_rain,
        R.drawable.ic_rain,
        R.drawable.ic_hail,
        R.drawable.ic_rain_and_snow,
        R.drawable.ic_rain_and_sleet,
        R.drawable.ic_snow_showers,
        R.drawable.ic_light_snow,
        R.drawable.ic_snow,
        R.drawable.ic_thunder,
        R.drawable.ic_thundery_showers,
        R.drawable.ic_clear,
        R.drawable.ic_clear_cloudy
    };

    public static final int NIGHT_IMAGES[] =
    {
        R.drawable.ic_sunny,
        R.drawable.ic_mostly_sunny,
        R.drawable.ic_clear_cloudy,
        R.drawable.ic_clear_cloudy,
        R.drawable.ic_cloudy,
        R.drawable.ic_haze,
        R.drawable.ic_mist,
        R.drawable.ic_fog,
        R.drawable.ic_clear_scattered_showers,
        R.drawable.ic_clear_scattered_showers,
        R.drawable.ic_clear_showers,
        R.drawable.ic_light_rain,
        R.drawable.ic_rain,
        R.drawable.ic_hail,
        R.drawable.ic_rain_and_snow,
        R.drawable.ic_rain_and_sleet,
        R.drawable.ic_clear_snow_showers,
        R.drawable.ic_light_snow,
        R.drawable.ic_snow,
        R.drawable.ic_thunder,
        R.drawable.ic_clear_thundery_showers,
        R.drawable.ic_clear,
        R.drawable.ic_clear_cloudy
    };

    public static final int REQUEST_PERMS = 1;
    public static final int LONG_DELAY = 300000;
    public static final int ADDRESSES = 10;

    public static final int CENTIGRADE = 1;
    public static final int FAHRENHEIT = 2;

    private Map<CharSequence, Integer> imageMap;
    private Map<CharSequence, Integer> nightMap;

    private Toast toast;

    private ImageView weatherImage;

    private TextView dateText;
    private TextView windText;
    private TextView humidityText;
    private TextView descriptionText;
    private TextView centigradeText;
    private TextView fahrenheitText;
    private TextView precipitationText;

    private ViewGroup dayGroup;

    private ProgressBar progress;

    private LocationListener listener;

    private int temperature;

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        temperature = preferences.getInt(PREF_TEMP, CENTIGRADE);

        setContentView(R.layout.main);

        weatherImage = findViewById(R.id.weather);

        dateText = findViewById(R.id.date);
        windText = findViewById(R.id.wind);
        humidityText = findViewById(R.id.humidity);
        descriptionText = findViewById(R.id.description);
        centigradeText = findViewById(R.id.centigrade);
        fahrenheitText = findViewById(R.id.fahrenheit);
        precipitationText = findViewById(R.id.precipitation);

        dayGroup = findViewById(R.id.days);

        progress = findViewById(R.id.progress);

        imageMap = new HashMap<CharSequence, Integer>();
        nightMap = new HashMap<CharSequence, Integer>();
        int index = 0;
        for (String key: DESCRIPTIONS)
        {
            imageMap.put(key, DAY_IMAGES[index]);
            nightMap.put(key, NIGHT_IMAGES[index++]);
        }

        Weather weather = this;
        listener = new LocationListener()
        {
            @Override
            public void onLocationChanged(Location location)
            {
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                if (!Geocoder.isPresent())
                {
                    progress.setVisibility(View.GONE);
                    showToast(R.string.noGeo);
                    return;
                }

                Geocoder geocoder = new Geocoder(weather);

                try
                {
                    List<Address> addressList =
                        geocoder.getFromLocation(lat, lng, ADDRESSES);

                    if (addressList == null)
                    {
                        showToast(R.string.noAddr);
                        return;
                    }

                    String locality = null;
                    for (Address address: addressList.toArray(new Address[0]))
                    {
                        if (address.getLocality() != null)
                        {
                            locality = String.format(ADDR_FORMAT,
                                                     address.getLocality(),
                                                     address.getSubAdminArea(),
                                                     address.getCountryName());
                            break;
                        }
                    }

                    progress.setVisibility(View.VISIBLE);

                    String url = String.format(GOOGLE_URL, locality);
                    GoogleTask task = new GoogleTask(weather);
                    task.execute(url);
                }

                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {}
            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}

        };
    }

    // onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        setTitle(savedInstanceState.getCharSequence(LOCN));

        if (savedInstanceState.getCharSequence(DATE) == null)
            return;

        dateText.setText(savedInstanceState.getCharSequence(DATE));
        windText.setText(savedInstanceState.getCharSequence(WIND));
        humidityText.setText(savedInstanceState.getCharSequence(HUMID));
        descriptionText.setText(savedInstanceState
                                .getCharSequence(DESC));
        centigradeText.setText(savedInstanceState
                               .getCharSequence(CENT));
        fahrenheitText.setText(savedInstanceState
                               .getCharSequence(FAHR));
        precipitationText.setText(savedInstanceState
                                  .getCharSequence(PRECIP));

        Calendar calendar = Calendar.getInstance();
        boolean night = ((calendar.get(Calendar.HOUR_OF_DAY) < 6) ||
                         (calendar.get(Calendar.HOUR_OF_DAY) > 18));

        weatherImage.setImageResource
            (night? nightMap.get(descriptionText.getText()):
             imageMap.get(descriptionText.getText()));

        ArrayList<String> days = savedInstanceState.getStringArrayList(DAYS);
        ArrayList<String> descs = savedInstanceState.getStringArrayList(DESCS);
        ArrayList<String> dtemps =
            savedInstanceState.getStringArrayList(DTEMPS);
        ArrayList<String> ntemps =
            savedInstanceState.getStringArrayList(NTEMPS);

        for (int i = 0; i < dayGroup.getChildCount(); i++)
        {
            ViewGroup group = (ViewGroup) dayGroup.getChildAt(i);
            ViewGroup g = (ViewGroup) group.getChildAt(1);
            ViewGroup gw = (ViewGroup) g.getChildAt(0);
            ViewGroup gt = (ViewGroup) g.getChildAt(1);
            ViewGroup gc = (ViewGroup) gt.getChildAt(0);
            ViewGroup gf = (ViewGroup) gt.getChildAt(1);

            TextView text = (TextView) gw.getChildAt(0);
            text.setText(days.get(i));
            text = (TextView) gw.getChildAt(1);
            text.setText(descs.get(i));
            switch (temperature)
            {
            case CENTIGRADE:
                text = (TextView) gc.getChildAt(0);
                text.setText(dtemps.get(i));
                text = (TextView) gc.getChildAt(1);
                text.setText(ntemps.get(i));
                break;

            case FAHRENHEIT:
                text = (TextView) gf.getChildAt(0);
                text.setText(dtemps.get(i));
                text = (TextView) gf.getChildAt(1);
                text.setText(ntemps.get(i));
                break;
            }

            ImageView image = (ImageView) group.getChildAt(0);
            image.setImageResource(imageMap.get(descs.get(i)));
        }
    }

    // onResume
    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        temp(preferences.getInt(PREF_TEMP, CENTIGRADE));

        if (preferences.contains(PREF_DATE))
        {
            setTitle(preferences.getString(PREF_LOCN, ""));
            dateText.setText(preferences.getString(PREF_DATE, ""));
            windText.setText(preferences.getString(PREF_WIND, ""));
            humidityText.setText(preferences.getString(PREF_HUMID, ""));
            descriptionText.setText(preferences.getString(PREF_DESC, ""));
            centigradeText.setText(preferences.getString(PREF_CENT, ""));
            fahrenheitText.setText(preferences.getString(PREF_FAHR, ""));
            precipitationText.setText(preferences.getString(PREF_PRECIP, ""));

            Calendar calendar = Calendar.getInstance();
            boolean night = ((calendar.get(Calendar.HOUR_OF_DAY) < 6) ||
                             (calendar.get(Calendar.HOUR_OF_DAY) > 18));

            weatherImage.setImageResource
                (night? nightMap.get(descriptionText.getText()):
                 imageMap.get(descriptionText.getText()));
        }

        getActionBar().setIcon(R.drawable.ic_action_location_searching);
        refresh();
    }

    // onSaveInstanceState
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putCharSequence(LOCN, getTitle());

        if (dateText.getText() == null || dateText.getText().length() == 0)
            return;

        outState.putCharSequence(DATE, dateText.getText());
        outState.putCharSequence(DESC, descriptionText.getText());
        outState.putCharSequence(CENT, centigradeText.getText());
        outState.putCharSequence(FAHR, fahrenheitText.getText());
        outState.putCharSequence(WIND, windText.getText());
        outState.putCharSequence(HUMID, humidityText.getText());
        outState.putCharSequence(PRECIP, precipitationText.getText());

        ArrayList<String> days = new ArrayList<String>();
        ArrayList<String> descs = new ArrayList<String>();
        ArrayList<String> dtemps = new ArrayList<String>();
        ArrayList<String> ntemps = new ArrayList<String>();

        for (int i = 0; i < dayGroup.getChildCount(); i++)
        {
            ViewGroup group = (ViewGroup) dayGroup.getChildAt(i);
            ViewGroup g = (ViewGroup) group.getChildAt(1);
            ViewGroup gw = (ViewGroup) g.getChildAt(0);
            ViewGroup gt = (ViewGroup) g.getChildAt(1);
            ViewGroup gc = (ViewGroup) gt.getChildAt(0);
            ViewGroup gf = (ViewGroup) gt.getChildAt(1);

            TextView text = (TextView) gw.getChildAt(0);
            days.add(text.getText().toString());
            text = (TextView) gw.getChildAt(1);
            descs.add(text.getText().toString());
            switch(temperature)
            {
            case CENTIGRADE:
                text = (TextView) gc.getChildAt(0);
                dtemps.add(text.getText().toString());
                text = (TextView) gc.getChildAt(1);
                ntemps.add(text.getText().toString());
                break;

            case FAHRENHEIT:
                text = (TextView) gf.getChildAt(0);
                dtemps.add(text.getText().toString());
                text = (TextView) gf.getChildAt(1);
                ntemps.add(text.getText().toString());
                break;
            }
        }

        outState.putStringArrayList(DAYS, days);
        outState.putStringArrayList(DESCS, descs);
        outState.putStringArrayList(DTEMPS, dtemps);
        outState.putStringArrayList(NTEMPS, ntemps);
    }

    // onPause
    @Override
    public void onPause()
    {
        super.onPause();

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt(PREF_TEMP, temperature);

        editor.putString(PREF_LOCN, getTitle().toString());
        editor.putString(PREF_DATE, dateText.getText().toString());
        editor.putString(PREF_DESC, descriptionText.getText().toString());
        editor.putString(PREF_CENT, centigradeText.getText().toString());
        editor.putString(PREF_FAHR, fahrenheitText.getText().toString());
        editor.putString(PREF_WIND, windText.getText().toString());
        editor.putString(PREF_HUMID, humidityText.getText().toString());
        editor.putString(PREF_PRECIP, precipitationText.getText().toString());

        editor.apply();

        // Update widgets
        updateWidgets();

        LocationManager locationManager = (LocationManager)
            getSystemService(LOCATION_SERVICE);

        locationManager.removeUpdates(listener);
    }

    // On create options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it
        // is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        return true;
    }

    // On options item selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Get id
        int id = item.getItemId();
        switch (id)
        {
        case R.id.cent:
            temp(CENTIGRADE);
            break;

        case R.id.fahr:
            temp(FAHRENHEIT);
            break;

        case R.id.help:
            help();
            break;

        case R.id.about:
            about();
            break;
        }

        return true;
    }

    // onRequestPermissionsResult
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        switch (requestCode)
        {
        case REQUEST_PERMS:
            for (int i = 0; i < grantResults.length; i++)
                if (permissions[i].equals(Manifest.permission
                                          .ACCESS_FINE_LOCATION) &&
                    grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    refresh();
        }
    }

    // refresh
    private void refresh()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]
            {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMS);
            return;
        }

        LocationManager locationManager = (LocationManager)
            getSystemService(LOCATION_SERVICE);

        String provider = locationManager.getBestProvider(new Criteria(), true);
        Location location = locationManager.getLastKnownLocation(provider);
        locationManager.requestLocationUpdates(provider, LONG_DELAY,
                                               0, listener);

        if (location == null)
        {
            locationManager.requestSingleUpdate(provider, listener, null);
            progress.setVisibility(View.VISIBLE);
            return;
        }

	double lat = location.getLatitude();
	double lng = location.getLongitude();

        if (!Geocoder.isPresent())
        {
            progress.setVisibility(View.GONE);
            showToast(R.string.noGeo);
            return;
        }

        Geocoder geocoder = new Geocoder(this);

        try
        {
            List<Address> addressList =
                geocoder.getFromLocation(lat, lng, ADDRESSES);

            if (addressList == null)
            {
                showToast(R.string.noAddr);
                return;
            }

            String locality = null;
            for (Address address: addressList.toArray(new Address[0]))
            {
                if (address.getLocality() != null)
                {
                    locality = String.format(ADDR_FORMAT,
                                             address.getLocality(),
                                             address.getSubAdminArea(),
                                             address.getCountryName());
                    break;
                }
            }

            progress.setVisibility(View.VISIBLE);

            String url = String.format(GOOGLE_URL, locality);
            GoogleTask task = new GoogleTask(this);
            task.execute(url);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // display
    private void display(Element doc)
    {
        progress.setVisibility(View.GONE);

        if (doc == null)
        {
            showToast(R.string.noData);
            return;
        }

        Element weather = doc.getElementById(WOB_WC);
        if (weather == null)
        {
            showToast(R.string.noData);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        boolean night = ((calendar.get(Calendar.HOUR_OF_DAY) < 6) ||
                         (calendar.get(Calendar.HOUR_OF_DAY) > 18));

        String location = weather.getElementById(WOB_LOC).text();
        getActionBar().setIcon(R.drawable.ic_action_location_found);
        setTitle(location);

        String date = weather.getElementById(WOB_DTS).text();
        dateText.setText(date);
        String description = weather.getElementById(WOB_DC).text();
        descriptionText.setText(description);
        weatherImage.setImageResource(night? nightMap.get(description):
                                      imageMap.get(description));

        String centigrade = weather.getElementById(WOB_TM).text();
        String format = getString(R.string.centigrade);
        centigradeText.setText(String.format(format, centigrade));
        String fahrenheit = weather.getElementById(WOB_TTM).text();
        format = getString(R.string.fahrenheit);
        fahrenheitText.setText(String.format(format, fahrenheit));

        String wind = weather.getElementById(WOB_WS).text();
        windText.setText(wind);

        String precipitation = weather.getElementById(WOB_PP).text();
        format = getString(R.string.precipitation);
        precipitationText.setText(String.format(format, precipitation));

        String humidity = weather.getElementById(WOB_HM).text();
        format = getString(R.string.humidity);
        humidityText.setText(String.format(format, humidity));

        Element daily = weather.getElementById(WOB_DP);
        Elements days = daily.getElementsByClass(WOB_DF);

        int index = 0;
        for (Element day: days)
        {
            String d = day.getElementsByClass(Z1VZSB).first().text();
            String w = day.getElementsByClass(UW5PK).first().attr("alt");
            ViewGroup group = (ViewGroup) dayGroup.getChildAt(index++);

            ImageView image = (ImageView) group.getChildAt(0);
            image.setImageResource(imageMap.get(w));

            ViewGroup g = (ViewGroup) group.getChildAt(1);
            ViewGroup gw = (ViewGroup) g.getChildAt(0);
            ViewGroup gt = (ViewGroup) g.getChildAt(1);
            ViewGroup gc = (ViewGroup) gt.getChildAt(0);
            ViewGroup gf = (ViewGroup) gt.getChildAt(1);
            
            TextView text = (TextView) gw.getChildAt(0);
            text.setText(d);
            text = (TextView) gw.getChildAt(1);
            text.setText(w);
            Elements tt = day.getElementsByClass(WOB_T);
            format = getString(R.string.centigrade);
            text = (TextView) gc.getChildAt(0);
            String td = tt.get(0).text();
            text.setText(String.format(format, td));
            text = (TextView) gc.getChildAt(1);
            String tn = tt.get(2).text();
            text.setText(String.format(format, tn));
            format = getString(R.string.fahrenheit);
            text = (TextView) gf.getChildAt(0);
            td = tt.get(1).text();
            text.setText(String.format(format, td));
            text = (TextView) gf.getChildAt(1);
            tn = tt.get(3).text();
            text.setText(String.format(format, tn));
        }
    }

    // updateWidgets
    @SuppressWarnings("deprecation")
    private void updateWidgets()
    {
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName provider = new
            ComponentName(this, WeatherWidgetProvider.class);
        RemoteViews views = new
            RemoteViews(getPackageName(), R.layout.widget);

        if (preferences.contains(Weather.PREF_DATE))
        {
            views.setTextViewText(R.id.location,
                                  preferences.getString(Weather.PREF_LOCN, ""));
            views.setTextViewText(R.id.date,
                                  preferences.getString(Weather.PREF_DATE, ""));
            views.setTextViewText(R.id.wind,
                                  preferences.getString(Weather.PREF_WIND, ""));
            views.setTextViewText(R.id.humidity,
                                  preferences.getString(Weather.PREF_HUMID, ""));
            views.setTextViewText(R.id.description,
                                  preferences.getString(Weather.PREF_DESC, ""));
            views.setTextViewText(R.id.centigrade,
                                  preferences.getString(Weather.PREF_CENT, ""));
            views.setTextViewText(R.id.fahrenheit,
                                  preferences.getString(Weather.PREF_FAHR, ""));
            views.setTextViewText(R.id.precipitation,
                                  preferences.getString(Weather.PREF_PRECIP, ""));

            Calendar calendar = Calendar.getInstance();
            boolean night = ((calendar.get(Calendar.HOUR_OF_DAY) < 6) ||
                             (calendar.get(Calendar.HOUR_OF_DAY) > 18));

            views.setImageViewResource
                (R.id.weather, night?
                 nightMap.get(preferences.getString(Weather.PREF_DESC, "")):
                 imageMap.get(preferences.getString(Weather.PREF_DESC, "")));

            switch (temperature)
            {
            case CENTIGRADE:
                views.setViewVisibility(R.id.centigrade, View.VISIBLE);
                views.setViewVisibility(R.id.fahrenheit, View.GONE);
                break;

            case FAHRENHEIT:
                views.setViewVisibility(R.id.centigrade, View.GONE);
                views.setViewVisibility(R.id.fahrenheit, View.VISIBLE);
                break;
            }

            manager.updateAppWidget(provider, views);
        }
    }

    // temp
    private void temp(int temp)
    {
        temperature = temp;

        switch (temp)
        {
        case CENTIGRADE:
            centigradeText.setVisibility(View.VISIBLE);
            fahrenheitText.setVisibility(View.GONE);
            break;

        case FAHRENHEIT:
            centigradeText.setVisibility(View.GONE);
            fahrenheitText.setVisibility(View.VISIBLE);
            break;
        }

        for (int i = 0; i < dayGroup.getChildCount(); i++)
        {
            ViewGroup group = (ViewGroup) dayGroup.getChildAt(i);
            ViewGroup g = (ViewGroup) group.getChildAt(1);
            ViewGroup gt = (ViewGroup) g.getChildAt(1);
            ViewGroup gc = (ViewGroup) gt.getChildAt(0);
            ViewGroup gf = (ViewGroup) gt.getChildAt(1);

            switch (temp)
            {
            case CENTIGRADE:
                gc.setVisibility(View.VISIBLE);
                gf.setVisibility(View.GONE);
                break;

            case FAHRENHEIT:
                gc.setVisibility(View.GONE);
                gf.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    // help
    private void help()
    {
        Intent intent = new Intent(this, Help.class);
        startActivity(intent);
    }

    // about
    @SuppressWarnings("deprecation")
    private void about()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.appName);
        builder.setIcon(R.drawable.ic_launcher);

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        SpannableStringBuilder spannable =
            new SpannableStringBuilder(getText(R.string.version));
        Pattern pattern = Pattern.compile("%s");
        Matcher matcher = pattern.matcher(spannable);
        if (matcher.find())
            spannable.replace(matcher.start(), matcher.end(),
                              BuildConfig.VERSION_NAME);
        matcher.reset(spannable);
        if (matcher.find())
            spannable.replace(matcher.start(), matcher.end(),
                              dateFormat.format(BuildConfig.BUILT));
        builder.setMessage(spannable);

        // Add the button
        builder.setPositiveButton(android.R.string.ok, null);

        // Create the AlertDialog
        Dialog dialog = builder.show();

        // Set movement method
        TextView text = dialog.findViewById(android.R.id.message);
        if (text != null)
        {
            text.setTextAppearance(builder.getContext(),
                                   android.R.style.TextAppearance_Small);
            text.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    // showToast
    void showToast(int key)
    {
        String text = getString(key);

        // Cancel the last one
        if (toast != null)
            toast.cancel();

        // Make a new one
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // GoogleTask
    private static class GoogleTask
            extends AsyncTask<String, Void, Document>
    {
        private WeakReference<Weather> weatherWeakReference;

        // GoogleTask
        public GoogleTask(Weather weather)
        {
            weatherWeakReference = new WeakReference<>(weather);
        }

        // doInBackground
        @Override
        protected Document doInBackground(String... params)
        {
            final Weather weather = weatherWeakReference.get();
            if (weather == null)
                return null;

            String url = params[0];
            // Do web search
            try
            {
                Document doc = Jsoup.connect(url).get();
                return doc;
            }

            catch (Exception e)
            {
                weather.runOnUiThread(() -> weather.showToast(R.string.noData));
                e.printStackTrace();
            }

            return null;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(Document doc)
        {
            final Weather weather = weatherWeakReference.get();
            if (weather == null)
                return;

            weather.display(doc);
        }
    }
}
