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
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.lang.ref.WeakReference;

import java.net.URL;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
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

    public static final String PREF_THEME = "pref_theme";
    public static final String PREF_DARK = "pref_dark";
    public static final String PREF_LIGHT = "pref_light";

    public static final String WEATHER_URL =
        "https://weatherdbi.herokuapp.com/data/weather/%s";

    public static final String GOOGLE_URL =
        "https://www.google.com/search?hl=en&q=weather %s";

    public static final String YAHOO_URL =
        "https://search.yahoo.com/search?p=weather %s";

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

    public static final String DESCRIPTIONS[] =
    {
        "Sunny", "Mostly sunny", "Partly cloudy", "Mostly cloudy",
        "Cloudy", "Scattered showers", "Showers", "Light rain",
        "Rain", "Heavy rain", "Snow showers", "Snow", "Clear",
        "Clear with periodic clouds"
    };

    public static final int IMAGES[] =
    {
        R.drawable.ic_sunny, R.drawable.ic_mostly_sunny,
        R.drawable.ic_partly_cloudy, R.drawable.ic_mostly_cloudy,
        R.drawable.ic_cloudy, R.drawable.ic_showers,
        R.drawable.ic_showers, R.drawable.ic_light_rain,
        R.drawable.ic_rain, R.drawable.ic_heavy_rain,
        R.drawable.ic_snow_showers, R.drawable.ic_snow,
        R.drawable.ic_clear, R.drawable.ic_clear_with_periodic_clouds
    };

    public static final int REQUEST_PERMS = 1;

    public static final int DARK  = 1;
    public static final int LIGHT = 2;

    // private ImageView locationImage;
    private ImageView weatherImage;

    private TextView dateText;
    private TextView windText;
    // private TextView locationText;
    private TextView humidityText;
    private TextView descriptionText;
    private TextView temperatureText;
    private TextView precipitationText;

    private ViewGroup dayGroup;

    private ProgressBar progress;

    private int theme;

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        theme = preferences.getInt(PREF_THEME, DARK);

        setContentView(R.layout.main);

        weatherImage = findViewById(R.id.weather);

        dateText = findViewById(R.id.date);
        windText = findViewById(R.id.wind);
        humidityText = findViewById(R.id.humidity);
        descriptionText = findViewById(R.id.description);
        temperatureText = findViewById(R.id.temperature);
        precipitationText = findViewById(R.id.precipitation);

        dayGroup = findViewById(R.id.days);

        progress = findViewById(R.id.progress);
    }

    // onResume
    @Override
    protected void onResume()
    {
        super.onResume();
        getActionBar().setIcon(R.drawable.ic_action_location_searching);
        refresh();
    }

    // onPause
    @Override
    public void onPause()
    {
        super.onPause();

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt(PREF_THEME, theme);
        editor.apply();
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

        if (location == null)
            return;

	double lat = location.getLatitude();
	double lng = location.getLongitude();

        if (!Geocoder.isPresent())
            return;

        Geocoder geocoder = new Geocoder(this);

        try
        {
            List<Address> addressList = geocoder.getFromLocation(lat, lng, 10);

            if (addressList == null)
                return;

            String locality = null;
            for (Address address: addressList.toArray(new Address[0]))
            {
                if (address.getLocality() != null)
                {
                    locality = String.format("%s, %s, %s",
                                             address.getLocality(),
                                             address.getSubAdminArea(),
                                             address.getCountryName());
                    break;
                }
            }

            String url = String.format(GOOGLE_URL, locality);

            GoogleTask task = new GoogleTask(this);
            task.execute(url);
            progress.setVisibility(View.VISIBLE);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // display
    private void display(Document doc)
    {
        progress.setVisibility(View.GONE);

        Element weather = doc.getElementById(WOB_WC);
        String location = weather.getElementById(WOB_LOC).text();

        getActionBar().setIcon(R.drawable.ic_action_location_found);
        setTitle(location);

        String date = weather.getElementById(WOB_DTS).text();
        dateText.setText(date);
        String description = weather.getElementById(WOB_DC).text();
        descriptionText.setText(description);
        for (int i = 0; i < DESCRIPTIONS.length; i++)
        {
            if (DESCRIPTIONS[i].contentEquals(description))
            {
                weatherImage.setImageResource(IMAGES[i]);
                break;
            }
        }

        String temperature = weather.getElementById(WOB_TM).text();
        String format = getString(R.string.centigrade);
        temperatureText.setText(String.format(format, temperature));

        String wind = weather.getElementById(WOB_WS).text();
        windText.setText(wind);

        String precipitation = weather.getElementById(WOB_PP).text();
        format = getString(R.string.precipitation);
        precipitationText.setText(String.format(format, precipitation));

        String humidity = weather.getElementById(WOB_HM).text();
        format = getString(R.string.humidity);
        humidityText.setText(String.format(format, humidity));

        Element daily = doc.getElementById(WOB_DP);
        Elements days = daily.getElementsByClass(WOB_DF);

        int index = 0;
        for (Element day: days)
        {
            String d = day.getElementsByClass(Z1VZSB).first().text();
            String w = day.getElementsByClass(UW5PK).first().attr("alt");
            ViewGroup group = (ViewGroup) dayGroup.getChildAt(index++);
            ImageView image = (ImageView) group.getChildAt(0);
            for (int i = 0; i < DESCRIPTIONS.length; i++)
            {
                if (DESCRIPTIONS[i].contentEquals(w))
                {
                    image.setImageResource(IMAGES[i]);
                    break;
                }
            }
            ViewGroup g = (ViewGroup) group.getChildAt(1);
            ViewGroup gw = (ViewGroup) g.getChildAt(0);
            ViewGroup gt = (ViewGroup) g.getChildAt(1);
            
            TextView text = (TextView) gw.getChildAt(0);
            text.setText(d);
            text = (TextView) gw.getChildAt(1);
            text.setText(w);
            Elements tt = day.getElementsByClass(WOB_T);
            format = getString(R.string.centigrade);
            text = (TextView) gt.getChildAt(0);
            String td = tt.get(0).text();
            text.setText(String.format(format, td));
            text = (TextView) gt.getChildAt(1);
            String tn = tt.get(2).text();
            text.setText(String.format(format, tn));
        }
    }

    // theme
    private void theme(int t)
    {
        theme = t;
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.M)
            recreate();
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
                weather.runOnUiThread(() ->
                {
                    // weather.textView.append("Exception " + e.toString());
                });

                e.printStackTrace();
            }

            return null;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(Document doc)
        {
            final Weather weather = weatherWeakReference.get();
            if (weather == null || doc == null)
                return;

            
            weather.display(doc);
        }
    }
}
