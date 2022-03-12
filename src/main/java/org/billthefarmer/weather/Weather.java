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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
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
/*
    public static final String REGION = "region";
    public static final String CURRENT_CONDITIONS = "currentConditions";
    public static final String DAY_HOUR = "dayhour";
    public static final String TEMP = "temp";
    public static final String C = "c";
    public static final String F = "f";
    public static final String PRECIP = "precip";
    public static final String HUMIDITY = "humidity";
    public static final String WIND = "wind";
    public static final String KM = "km";
    public static final String MILE = "mile";
    public static final String ICON_URL = "iconURL";
    public static final String COMMENT = "comment";
    public static final String NEXT_DAYS = "next_days";
    public static final String DAY = "day";
    public static final String MAX_TEMP = "max_temp";
    public static final String MIN_TEMP = "min_temp";
    public static final String MAX_C = "max_c";
    public static final String MAX_F = "max_f";
    public static final String MIN_C = "min_c";
    public static final String MIN_F = "min_f";
*/
    public static final String WOB_WC = "wob_wc";
    public static final String WOB_DC = "wob_dc";
    public static final String WOB_TM = "wob_tm";
    public static final String WOB_WS = "wob_ws";
    public static final String WOB_DP = "wob_dp";
    public static final String WOB_DF = "wob_df";
    public static final String WOB_PP = "wob_pp";
    public static final String WOB_HM = "wob_hm";

    public static final String WOB_LOC = "wob_loc";
    public static final String WOB_DTS = "wob_dts";
    public static final String WOB_TTM = "wob_ttm";

    public static final String Z1VZSB = "Z1VzSb";
    public static final String UW5PK = "uW5pk";
    public static final String WOB_T = "wob_t";

    public static final int REQUEST_PERMS = 1;

    public static final int DARK  = 1;
    public static final int LIGHT = 2;

    private Map<String, String> currentMap;
    private List<Map<String, String>> forecastList;

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

        currentMap = new HashMap<String, String>();
        forecastList = new ArrayList<Map<String, String>>();
    }

    // onResume
    @Override
    protected void onResume()
    {
        super.onResume();
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

            String locality = "";
            for (Address address: addressList.toArray(new Address[0]))
            {
                locality = address.getAddressLine(0);
                if (address.getLocality() != null &&
                    locality.startsWith(address.getLocality()))
                   break;
            }

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
    private void display(Document doc)
    {
        Element weather = doc.getElementById(WOB_WC);
        String location = weather.getElementById(WOB_LOC).text();
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
            if (weather == null)
                return;

            if (doc == null)
                return;

            weather.display(doc);
        }
    }
/*
    // LoadTask
    private static class LoadTask extends AsyncTask<String, Void, String>
    {
        private WeakReference<Weather> weatherWeakReference;

        // LoadTask
        public LoadTask(Weather weather)
        {
            weatherWeakReference = new WeakReference<>(weather);
        }

        // doInBackground
        @Override
        protected String doInBackground(String... urls)
        {
            final Weather weather = weatherWeakReference.get();
            if (weather == null)
                return null;

            StringBuilder content = new StringBuilder();
            try
            {
                URL url = new URL(urls[0]);
                try (BufferedReader reader = new BufferedReader
                     (new InputStreamReader(url.openStream())))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        content.append(line);
                        content.append(System.getProperty("line.separator"));
                    }
                }
            }

            catch (Exception e) {}

            return content.toString();
        }

        // onPostExecute
        @Override
        protected void onPostExecute(String result)
        {
            final Weather weather = weatherWeakReference.get();
            if (weather == null)
                return;

            try
            {
                JSONObject json = new JSONObject(result);
                String region = json.getString(REGION);
                JSONObject currentJSON = json.getJSONObject(CURRENT_CONDITIONS);
                String dateTime = currentJSON.getString(DAY_HOUR);
                JSONObject tempJSON = currentJSON.getJSONObject(TEMP);
                String cent = tempJSON.getString(C);
                String fahr = tempJSON.getString(F);
                String precip = currentJSON.getString(PRECIP);
                String humidity = currentJSON.getString(HUMIDITY);
                JSONObject windJSON = currentJSON.getJSONObject(WIND);
                String km = windJSON.getString(KM);
                String mile = windJSON.getString(MILE);
                String icon = currentJSON.getString(ICON_URL);
                String comment = currentJSON.getString(COMMENT);

                weather.currentMap.put(REGION, region);
                weather.currentMap.put(DAY_HOUR, dateTime);
                weather.currentMap.put(C, cent);
                weather.currentMap.put(F, fahr);
                weather.currentMap.put(PRECIP, precip);
                weather.currentMap.put(HUMIDITY, humidity);
                weather.currentMap.put(KM, km);
                weather.currentMap.put(MILE, mile);
                weather.currentMap.put(ICON_URL, icon);
                weather.currentMap.put(COMMENT, comment);

                JSONArray forecastJSON = json.getJSONArray(NEXT_DAYS);
                for (int i = 0; i < forecastJSON.length(); i++)
                {
                    Map<String, String> dayMap = new HashMap<String, String>();
                    JSONObject dayJSON = forecastJSON.getJSONObject(i);

                    String day = dayJSON.getString(DAY);
                    String dayComment = dayJSON.getString(COMMENT);
                    JSONObject maxJSON = dayJSON.getJSONObject(MAX_TEMP);
                    String maxC = tempJSON.getString(C);
                    String maxF = tempJSON.getString(F);
                    JSONObject minJSON = dayJSON.getJSONObject(MIN_TEMP);
                    String minC = tempJSON.getString(C);
                    String minF = tempJSON.getString(F);
                    String dayIcon = dayJSON.getString(ICON_URL);

                    dayMap.put(DAY, day);
                    dayMap.put(COMMENT, dayComment);
                    dayMap.put(MAX_C, maxC);
                    dayMap.put(MAX_F, maxF);
                    dayMap.put(MIN_C, minC);
                    dayMap.put(MIN_F, minF);
                    dayMap.put(ICON_URL, dayIcon);

                    weather.forecastList.add(dayMap);
                }
            }

            catch (Exception e) {}
        }
    }
*/
}
