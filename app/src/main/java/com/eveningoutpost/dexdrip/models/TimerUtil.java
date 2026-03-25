package com.eveningoutpost.dexdrip.models;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;

import org.joda.time.DateTime;

public class TimerUtil {

    /**
     * Schedule alarm in @seconds
     */
    public static void scheduleReminder(Context context, int seconds, String text) {
        final Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AlarmClock.EXTRA_LENGTH, seconds);
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, text);
        intent.putExtra(AlarmClock.VALUE_RINGTONE_SILENT, true);
        context.startActivity(intent);
    }

    public static void scheduleAlarm(Context context, int seconds, String text) {
        DateTime dt = new DateTime();
        final Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AlarmClock.EXTRA_MINUTES, dt.getMinuteOfHour()+1);
        intent.putExtra(AlarmClock.EXTRA_HOUR, dt.getHourOfDay());
        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, text);
        intent.putExtra(AlarmClock.VALUE_RINGTONE_SILENT, true);
        context.startActivity(intent);
    }
}