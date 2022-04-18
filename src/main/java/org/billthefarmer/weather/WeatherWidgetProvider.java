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
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
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

public class WeatherWidgetProvider extends AppWidgetProvider
{
    private Map<CharSequence, Integer> imageMap;

    private String location;
    private String date;
    private String description;

    private String centigrade;
    private String fahrenheit;
    private String wind;

    private String precipitation;
    private String humidity;

    private String format;

    private int temperature;

    // onAppWidgetOptionsChanged
    @Override
    public void onAppWidgetOptionsChanged(Context context,
                                          AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          Bundle newOptions)
    {
        int appWidgetIds[] = {appWidgetId};
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    // onUpdate
    @Override
    @SuppressLint("InlinedApi")
    public void onUpdate(Context context,
                         AppWidgetManager appWidgetManager,
                         int[] appWidgetIds)
    {
        // Create an Intent to launch Weather
        Intent intent = new Intent(context, Weather.class);
        PendingIntent pendingIntent =
            PendingIntent.getActivity(context, 0, intent,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);

        // Create an Intent to update Weather widget
        Intent updateIntent = new Intent(context, WeatherWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                              appWidgetIds);

        PendingIntent pendingUpdate =
            PendingIntent.getBroadcast(context, 0, updateIntent,
                                       PendingIntent.FLAG_UPDATE_CURRENT |
                                       PendingIntent.FLAG_IMMUTABLE);

        // Get the layout for the widget and attach an on-click
        // listener to the view.
        RemoteViews views = new
            RemoteViews(context.getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.widget, pendingIntent);
        views.setOnClickPendingIntent(R.id.update, pendingUpdate);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(context);

        if (preferences.contains(Weather.PREF_DATE))
        {
            views.setTextViewText(R.id.location,
                                  preferences.getString(Weather.PREF_LOCN, ""));
            views.setTextViewText(R.id.date,
                                  preferences.getString(Weather.PREF_DATE, ""));
            views.setTextViewText(R.id.description,
                                  preferences.getString(Weather.PREF_DESC, ""));
            format = context.getString(R.string.centigrade);
            centigrade = preferences.getString(Weather.PREF_CENT, "");
            views.setTextViewText(R.id.centigrade,
                                  String.format(format, centigrade));
            format = context.getString(R.string.fahrenheit);
            fahrenheit = preferences.getString(Weather.PREF_FAHR, "");
            views.setTextViewText(R.id.fahrenheit,
                                  String.format(format, fahrenheit));
            views.setTextViewText(R.id.wind,
                                  preferences.getString(Weather.PREF_WIND, ""));
            format = context.getString(R.string.precipitation);
            precipitation = preferences.getString(Weather.PREF_PRCP, "");
            views.setTextViewText(R.id.precipitation,
                                  String.format(format, precipitation));
            format = context.getString(R.string.humidity);
            humidity = preferences.getString(Weather.PREF_HUMD, "");
            views.setTextViewText(R.id.humidity,
                                  String.format(format, humidity));

            imageMap = new HashMap<CharSequence, Integer>();

            Calendar today = Calendar.getInstance();
            boolean night = ((today.get(Calendar.HOUR_OF_DAY) < 6) ||
                             (today.get(Calendar.HOUR_OF_DAY) > 18));

            int index = 0;
            for (String key: Weather.DESCRIPTIONS)
                imageMap.put(key, night? Weather.NIGHT_IMAGES[index++]:
                             Weather.DAY_IMAGES[index++]);

            Integer id = imageMap.get(
                preferences.getString(Weather.PREF_DESC, ""));
            views.setImageViewResource(R.id.weather, (id == null)? 0: id);

            temperature = preferences.getInt(Weather.PREF_TEMP,
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
        }

        views.setViewVisibility(R.id.progress, View.VISIBLE);
        views.setViewVisibility(R.id.update, View.INVISIBLE);

        // Tell the AppWidgetManager to perform an update on the app
        // widgets.
        appWidgetManager.updateAppWidget(appWidgetIds, views);
        refresh(context);
    }

    // refresh
    private void refresh(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context
            .checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED)
        {
            hideProgress(context);
            return;
        }

        LocationManager locationManager = (LocationManager)
            context.getSystemService(Context.LOCATION_SERVICE);

        String provider = locationManager.getBestProvider(new Criteria(), true);
        Location location = locationManager.getLastKnownLocation(provider);

        if (location == null)
        {
            LocationListener listener = new LocationListener()
            {
                @Override
                public void onLocationChanged(Location location)
                {
                    located(context, location);
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
            return;
        }

        located(context, location);
    }

    // located
    private void located(Context context, Location location)
    {
        double lat = location.getLatitude();
	double lng = location.getLongitude();

        if (!Geocoder.isPresent())
        {
            hideProgress(context);
            return;
        }

        Geocoder geocoder = new Geocoder(context);

        try
        {
            List<Address> addressList =
                geocoder.getFromLocation(lat, lng, Weather.ADDRESSES);

            if (addressList == null)
                return;

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
            GoogleTask task = new GoogleTask(context, this);
            task.execute(url);
        }

        catch (Exception e) {}
    }

    // hideProgress
    private void hideProgress(Context context)
    {
        RemoteViews views = new
            RemoteViews(context.getPackageName(), R.layout.widget);

        views.setViewVisibility(R.id.progress, View.INVISIBLE);
        views.setViewVisibility(R.id.update, View.VISIBLE);

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new
            ComponentName(context, WeatherWidgetProvider.class);

        manager.updateAppWidget(provider, views);
    }

    // update
    private void update(Context context, Document doc)
    {
        if (doc == null)
        {
            hideProgress(context);
            return;
        }

        Element weather = doc.getElementById(Weather.WOB_WC);
        if (weather == null)
        {
            hideProgress(context);
            return;
        }

        // Get the layout for the widget
        RemoteViews views = new
            RemoteViews(context.getPackageName(), R.layout.widget);

        try
        {
            location = weather.getElementById(Weather.WOB_LOC).text();
            date = weather.getElementById(Weather.WOB_DTS).text();
            description = weather.getElementById(Weather.WOB_DC).text();

            centigrade = weather.getElementById(Weather.WOB_TM).text();
            fahrenheit = weather.getElementById(Weather.WOB_TTM).text();
            wind = weather.getElementById(Weather.WOB_WS).text();

            precipitation = weather.getElementById(Weather.WOB_PP).text();
            humidity = weather.getElementById(Weather.WOB_HM).text();

            views.setTextViewText(R.id.location, location);
            views.setTextViewText(R.id.date, date);
            views.setTextViewText(R.id.description, description);
            views.setTextViewText(R.id.wind, wind);
            String format = context.getString(R.string.centigrade);
            views.setTextViewText(R.id.centigrade,
                                  String.format(format, centigrade));
            format = context.getString(R.string.fahrenheit);
            views.setTextViewText(R.id.fahrenheit,
                                  String.format(format, fahrenheit));
            format = context.getString(R.string.precipitation);
            views.setTextViewText(R.id.precipitation,
                                  String.format(format, precipitation));
            format = context.getString(R.string.humidity);
            views.setTextViewText(R.id.humidity,
                                  String.format(format, humidity));

            Integer id = imageMap.get(description);
            views.setImageViewResource(R.id.weather, (id == null)? 0: id);

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
        }

        catch (Exception e) {}

        views.setViewVisibility(R.id.progress, View.INVISIBLE);
        views.setViewVisibility(R.id.update, View.VISIBLE);

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName provider = new
            ComponentName(context, WeatherWidgetProvider.class);

        manager.updateAppWidget(provider, views);
    }

    // GoogleTask
    private static class GoogleTask
            extends AsyncTask<String, Void, Document>
    {
        private WeakReference<WeatherWidgetProvider> weatherWeakReference;
        private WeakReference<Context> contextWeakReference;

        // GoogleTask
        public GoogleTask(Context context, WeatherWidgetProvider weather)
        {
            weatherWeakReference = new WeakReference<>(weather);
            contextWeakReference = new WeakReference<>(context);
        }

        // doInBackground
        @Override
        protected Document doInBackground(String... params)
        {
            final WeatherWidgetProvider weather = weatherWeakReference.get();
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
            final WeatherWidgetProvider weather = weatherWeakReference.get();
            final Context context = contextWeakReference.get();
            if (weather == null || context == null)
               return;

            weather.update(context, doc);
        }
    }
}
