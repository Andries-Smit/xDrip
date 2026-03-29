package com.eveningoutpost.dexdrip.utilitymodels;

/**
 * jamorham
 *
 * Vehicle mode abstraction interface
 */

// TODO move elements relating only to vehicle mode from ActivityRecognizedService to here

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.ActivityRecognizedService;
import com.eveningoutpost.dexdrip.services.FloatingViewService;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.utilitymodels.Intents.ACTION_VEHICLE_MODE;
import static com.eveningoutpost.dexdrip.utilitymodels.Intents.EXTRA_VEHICLE_MODE_ENABLED;

public class VehicleMode {

    private static final String TAG = VehicleMode.class.getSimpleName();

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse("vehicle_mode_enabled");
    }

    public static boolean viaCarAudio() {
        return Pref.getBooleanDefaultFalse("vehicle_mode_via_car_audio");
    }

    public static boolean shouldPlaySound() {
        return Pref.getBooleanDefaultFalse("play_sound_in_vehicle_mode");
    }

    public static boolean shouldUseSpeech() {
        return Pref.getBooleanDefaultFalse("speak_readings_in_vehicle_mode");
    }

    public static boolean shouldSpeak() {
        return isEnabled() && shouldUseSpeech() && isVehicleModeActive();
    }

    // TODO extract functionality for this from ActivityRecognizedService
    public static boolean isVehicleModeActive() {
        return ActivityRecognizedService.is_in_vehicle_mode();
    }

    public static void setVehicleModeActive(final boolean active) {
        ActivityRecognizedService.set_vehicle_mode(active);
        if (Pref.getBooleanDefaultFalse("vehicle_mode_floating_widget")) {
            if (active) {
                startFloatingWidget();
            } else {
                stopFloatingWidget();
            }
        }
    }

    public static void onFloatingWidgetPrefChanged(final boolean enabled) {
        if (enabled) {
            if (isVehicleModeActive()) {
                startFloatingWidget();
            }
        } else {
            stopFloatingWidget();
        }
    }

    private static void startFloatingWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(xdrip.getAppContext())) {
            UserError.Log.e(TAG, "Cannot show floating widget: overlay permission not granted");
            return;
        }
        Pref.setBoolean("show_floating_widget", true);
        xdrip.getAppContext().startService(
                new Intent(xdrip.getAppContext(), FloatingViewService.class));
    }

    private static void stopFloatingWidget() {
        Pref.setBoolean("show_floating_widget", false);
        xdrip.getAppContext().stopService(
                new Intent(xdrip.getAppContext(), FloatingViewService.class));
    }

    public static void sendBroadcast() {
        if (SendXdripBroadcast.enabled()) {
            final Bundle bundle = new Bundle();
            bundle.putString(EXTRA_VEHICLE_MODE_ENABLED, isVehicleModeActive() ? "true" : "false");
            SendXdripBroadcast.send(new Intent(ACTION_VEHICLE_MODE), bundle);
        }
    }
}
