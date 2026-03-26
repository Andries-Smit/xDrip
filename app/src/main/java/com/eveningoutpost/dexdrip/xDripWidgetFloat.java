package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.utilitymodels.WidgetDisplayHelper;

import java.util.Date;

/**
 * Display helper for the compact floating widget.
 * Delegates colour/data logic to WidgetDisplayHelper, then post-processes:
 *   - BG text size scaled to 80% of standard widget sizes (55sp→44sp, 45sp→36sp)
 *   - Reading age shown as "6'" instead of "6 minutes ago"
 *   - Delta shown without unit suffix ("+0.3 mmol" → "+0.3")
 */
public class xDripWidgetFloat {

    public static void update(View root, Context context) {
        WidgetDisplayHelper.updateCommonWidgetElements(
                root, context, R.id.xDripwidget, 0, 0, null);
        postProcess(root);
    }

    private static void postProcess(View root) {
        final BgReading lastBg = BgReading.lastNoSenssor();
        if (lastBg == null) return;

        // Scale BG text: WidgetDisplayHelper sets 55sp (short) or 45sp (long); apply 80%
        final TextView bgView = root.findViewById(R.id.widgetBg);
        if (bgView != null) {
            bgView.setTextSize(bgView.getText().length() > 3 ? 36f : 44f);
        }

        // Compact time: "6'" instead of "6 minutes ago"
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

        // Strip unit suffix from delta: "+0.3 mmol" → "+0.3", "+5 mg/dL" → "+5"
        final TextView deltaView = root.findViewById(R.id.widgetDelta);
        if (deltaView != null) {
            final String delta = deltaView.getText().toString();
            final int spaceIdx = delta.indexOf(' ');
            if (spaceIdx > 0) {
                deltaView.setText(delta.substring(0, spaceIdx));
            }
        }
    }
}
