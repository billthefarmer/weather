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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class WeatherWidgetProvider extends AppWidgetProvider
{

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

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(context);

        // Create an Intent to launch Weather
        Intent intent = new Intent(context, Weather.class);
        PendingIntent pendingIntent =
            PendingIntent.getActivity(context, 0, intent,
                                      PendingIntent.FLAG_UPDATE_CURRENT |
                                      PendingIntent.FLAG_IMMUTABLE);

        // Get the layout for the widget and attach an on-click
        // listener to the view.
        RemoteViews views = new
            RemoteViews(context.getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.widget, pendingIntent);

        int temp = preferences.getInt(Weather.PREF_TEMP, Weather.CENTIGRADE);

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

            Map<CharSequence, Integer> imageMap = new
                HashMap<CharSequence, Integer>();

            Calendar today = Calendar.getInstance();
            boolean night = ((today.get(Calendar.HOUR_OF_DAY) < 6) ||
                             (today.get(Calendar.HOUR_OF_DAY) > 18));

            int index = 0;
            for (String key: Weather.DESCRIPTIONS)
                imageMap.put(key, night? Weather.NIGHT_IMAGES[index++]:
                             Weather.DAY_IMAGES[index++]);

            views.setImageViewResource
                (R.id.weather,
                 imageMap.get(preferences.getString(Weather.PREF_DESC, "")));
        }

        // Tell the AppWidgetManager to perform an update on the app
        // widgets.
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }
}
