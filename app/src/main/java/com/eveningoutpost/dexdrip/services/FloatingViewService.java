package com.eveningoutpost.dexdrip.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.WidgetDisplayHelper;

import java.util.Date;

public class FloatingViewService extends Service {

    private WindowManager mWindowManager;
    private View mFloatingView;
    private WindowManager.LayoutParams params;

    private BroadcastReceiver newDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWidgetData();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();

        mFloatingView = LayoutInflater.from(this).inflate(R.layout.x_drip_widget_float, null);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Pref.getInt("floating_widget_x", 0);
        params.y = Pref.getInt("floating_widget_y", 100);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        try {
            mWindowManager.addView(mFloatingView, params);
        } catch (Exception e) {
            com.eveningoutpost.dexdrip.models.UserError.Log.e("FloatingViewService", "Could not add view to window manager: " + e);
            stopSelf();
            return;
        }

        final View root = mFloatingView.findViewById(R.id.floating_widget_root);
        final ImageView closeButton = mFloatingView.findViewById(R.id.close_floating_view);

        root.setOnTouchListener(new View.OnTouchListener() {
            private static final int CLICK_THRESHOLD_DP = 5;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isDragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        final float dx = event.getRawX() - initialTouchX;
                        final float dy = event.getRawY() - initialTouchY;
                        final float density = v.getContext().getResources().getDisplayMetrics().density;
                        if (!isDragging && Math.hypot(dx, dy) < CLICK_THRESHOLD_DP * density) {
                            return true; // within tap slop — don't move widget yet
                        }
                        isDragging = true;
                        params.x = initialX + (int) dx;
                        params.y = initialY + (int) dy;
                        try {
                            mWindowManager.updateViewLayout(mFloatingView, params);
                        } catch (Exception e) {
                            // ignore
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            Intent intent = new Intent(FloatingViewService.this, Home.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            Pref.setInt("floating_widget_x", params.x);
                            Pref.setInt("floating_widget_y", params.y);
                        }
                        return true;
                }
                return false;
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Pref.setBoolean("show_floating_widget", false);
                stopSelf();
            }
        });

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_NEW_BG_ESTIMATE);
        filter.addAction(Intents.ACTION_NEW_BG_ESTIMATE_NO_DATA);
        filter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(newDataReceiver, filter);
        updateWidgetData();
    }

    private void updateWidgetData() {
        if (mFloatingView == null) return;
        WidgetDisplayHelper.updateCommonWidgetElements(
                mFloatingView, getApplicationContext(), R.id.xDripwidget, 0, 0, null);
        postProcessFloatingWidget(mFloatingView);
    }

    /**
     * Post-processes the floating widget view after common widget update:
     *   - BG text dynamic sizeing
     *   - Reading age shown as "6'" instead of "6 minutes ago", colored by staleness
     *   - Delta shown without unit suffix ("+0.3 mmol" → "+0.3")
     */
    private void postProcessFloatingWidget(View root) {
        final BgReading lastBg = BgReading.lastNoSenssor();
        if (lastBg == null) return;

        final TextView bgView = root.findViewById(R.id.widgetBg);
        if (bgView != null) {
            bgView.setTextSize(bgView.getText().length() > 3 ? 34f : 40f);
        }

        final TextView ageView = root.findViewById(R.id.readingAge);
        if (ageView != null) {
            final int timeAgo = (int) Math.floor(
                    (new Date().getTime() - lastBg.timestamp) / 60000.0);
            ageView.setText(timeAgo + "'");
            ageView.setTextColor(
                    timeAgo > 30 ? Color.parseColor("#ff333d")
                    : timeAgo > 15 ? Color.parseColor("#FFBB33")
                    : Color.WHITE);
        }

        final TextView deltaView = root.findViewById(R.id.widgetDelta);
        if (deltaView != null) {
            final String delta = deltaView.getText().toString();
            final int spaceIdx = delta.indexOf(' ');
            if (spaceIdx > 0) {
                deltaView.setText(delta.substring(0, spaceIdx));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) {
            try {
                mWindowManager.removeView(mFloatingView);
            } catch (Exception e) {
                // ignore
            }
        }
        try {
            unregisterReceiver(newDataReceiver);
        } catch (Exception e) {
            // ignore
        }
    }
}
