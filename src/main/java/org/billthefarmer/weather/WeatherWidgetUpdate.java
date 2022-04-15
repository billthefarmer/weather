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
import android.annotation.SuppressLint;
import android.app.Service;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;

import java.lang.ref.WeakReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class WeatherWidgetUpdate extends Service
{
    // onCreate
    @Override
    public void onCreate()
    {
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED)
            stopSelf();

        LocationManager locationManager = (LocationManager)
            getSystemService(LOCATION_SERVICE);

        String provider = locationManager.getBestProvider(new Criteria(), true);
        Location location = locationManager.getLastKnownLocation(provider);

        if (location == null)
        {
            LocationListener listener = new LocationListener()
            {
                @Override
                public void onLocationChanged(Location location)
                {
                    located(location);
                }

                @Override
                public void onStatusChanged(String provider, int status,
                                            Bundle extras) {}
                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {}
            };

            locationManager.requestSingleUpdate(provider, listener, null);
            return START_NOT_STICKY;
        }

        located(location);
        return START_NOT_STICKY;
    }
    
    // located
    private void located(Location location)
    {
        double lat = location.getLatitude();
	double lng = location.getLongitude();

        if (!Geocoder.isPresent())
            stopSelf();

        Geocoder geocoder = new Geocoder(this);

        try
        {
            List<Address> addressList =
                geocoder.getFromLocation(lat, lng, Weather.ADDRESSES);

            if (addressList == null)
                stopSelf();

            String locality = null;
            for (Address address: addressList.toArray(new Address[0]))
            {
                if (address.getLocality() != null)
                {
                    locality = String.format(Weather.ADDR_FORMAT,
                                             address.getLocality(),
                                             address.getSubAdminArea(),
                                             address.getCountryName());
                    break;
                }
            }

            String url = String.format(Weather.GOOGLE_URL, locality);
                Document doc = Jsoup.connect(url).get();
                update(doc);
        }

        catch (Exception e)
        {
            stopSelf();
        }
    }

    // update
    @SuppressLint("InlinedApi")
    private void update(Document doc)
    {
        if (doc == null)
            stopSelf();

        Element weather = doc.getElementById(Weather.WOB_WC);
        if (weather == null)
            stopSelf();

        String location = weather.getElementById(Weather.WOB_LOC).text();
        String date = weather.getElementById(Weather.WOB_DTS).text();
        String description = weather.getElementById(Weather.WOB_DC).text();

        String centigrade = weather.getElementById(Weather.WOB_TM).text();
        String fahrenheit = weather.getElementById(Weather.WOB_TTM).text();
        String wind = weather.getElementById(Weather.WOB_WS).text();

        String precipitation = weather.getElementById(Weather.WOB_PP).text();
        String humidity = weather.getElementById(Weather.WOB_HM).text();

        // Create an Intent to launch Weather
        Intent intent = new Intent(this, Weather.class);
        PendingIntent pendingIntent =
            PendingIntent.getActivity(this, 0, intent,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);

        // Create an Intent to refresh Weather widget
        // Intent refresh = new Intent(this, WeatherWidgetUpdate.class);
        // PendingIntent refreshIntent =
        //     PendingIntent.getActivity(this, 0, refresh,
        //                               PendingIntent.FLAG_UPDATE_CURRENT |
        //                               PendingIntent.FLAG_IMMUTABLE);

        // Get the layout for the widget and attach an on-click
        // listener to the view.
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.widget, pendingIntent);
        // views.setOnClickPendingIntent(R.id.update, refreshIntent);

        views.setTextViewText(R.id.location, location);
        views.setTextViewText(R.id.date, date);
        views.setTextViewText(R.id.description, description);
        views.setTextViewText(R.id.wind, wind);
        String format = getString(R.string.centigrade);
        views.setTextViewText(R.id.centigrade,
                              String.format(format, centigrade));
        format = getString(R.string.fahrenheit);
        views.setTextViewText(R.id.fahrenheit,
                              String.format(format, fahrenheit));
        format = getString(R.string.precipitation);
        views.setTextViewText(R.id.precipitation,
                              String.format(format, precipitation));
        format = getString(R.string.humidity);
        views.setTextViewText(R.id.humidity,
                              String.format(format, humidity));

        Map<CharSequence, Integer> imageMap = new
            HashMap<CharSequence, Integer>();

        Calendar calendar = Calendar.getInstance();
        boolean night = ((calendar.get(Calendar.HOUR_OF_DAY) < 6) ||
                         (calendar.get(Calendar.HOUR_OF_DAY) > 18));

        int index = 0;
        for (String key: Weather.DESCRIPTIONS)
            imageMap.put(key, night? Weather.NIGHT_IMAGES[index++]:
                         Weather.DAY_IMAGES[index++]);

        views.setImageViewResource(R.id.weather, imageMap.get(description));

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        int temperature = preferences.getInt(Weather.PREF_TEMP,
                                             Weather.CENTIGRADE);
        switch (temperature)
        {
        case Weather.CENTIGRADE:
            views.setViewVisibility(R.id.centigrade, View.VISIBLE);
            views.setViewVisibility(R.id.fahrenheit, View.GONE);
            break;

        case Weather.FAHRENHEIT:
            views.setViewVisibility(R.id.centigrade, View.GONE);
            views.setViewVisibility(R.id.fahrenheit, View.VISIBLE);
            break;
        }

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName provider = new
            ComponentName(this, WeatherWidgetProvider.class);

        manager.updateAppWidget(provider, views);
        stopSelf();
    }

    // GoogleTask
    private static class GoogleTask
            extends AsyncTask<String, Void, Document>
    {
        private WeakReference<WeatherWidgetUpdate> weatherWeakReference;

        // GoogleTask
        public GoogleTask(WeatherWidgetUpdate weather)
        {
            weatherWeakReference = new WeakReference<>(weather);
        }

        // doInBackground
        @Override
        protected Document doInBackground(String... params)
        {
            final WeatherWidgetUpdate weather = weatherWeakReference.get();
            if (weather == null)
                return null;

            String url = params[0];
            // Do web search
            try
            {
                Document doc = Jsoup.connect(url).get();
                return doc;
            }

            catch (Exception e) {}

            return null;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(Document doc)
        {
            final WeatherWidgetUpdate weather = weatherWeakReference.get();
            if (weather == null)
                weather.stopSelf();

            weather.update(doc);
        }
    }
}
