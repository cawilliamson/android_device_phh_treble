package com.restlessos.partner.wallpapers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Marker receiver discovered by WallpaperPicker2 via the
 * com.android.launcher3.action.PARTNER_CUSTOMIZATION broadcast. its presence
 * makes the picker load this apk's resources as the partner customization
 * package; no logic needed here.
 */
public class PartnerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    }
}
