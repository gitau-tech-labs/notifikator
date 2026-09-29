package net.kzxiv.notify.client;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.widget.Toast;

import net.kzxiv.notify.client.service.ForwarderService;

public class ConfigurationActivity extends PreferenceActivity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        addPreferencesFromResource(R.xml.preferences);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        if (!isNotificationListenerEnabled()) {
            Toast.makeText(this, R.string.notification_access_required, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }

        // Start the foreground forwarder service (30s retry loop)
        ForwarderService.start(this);

        // Ask user once to disable battery optimization for reliable background delivery
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception ignored) {
            }
        }

        Preference manageDenylistButton = findPreference(getString(R.string.key_manage_denylist));
        manageDenylistButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(ConfigurationActivity.this, AppPickerActivity.class));
                return true;
            }
        });

    }
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
    {
        int NOTIFICATION_ID = 0;
        String CHANNEL_ID = "notifikator";

        Resources res = getResources();
        if (res.getString(R.string.key_send).equals(preference.getKey()))
        {
            NotificationManager mgr = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
            Notification.Builder nb = new Notification.Builder(this);

            nb.setContentTitle(res.getString(R.string.notification_title));
            nb.setContentText(res.getString(R.string.notification_text));
            nb.setSmallIcon(R.drawable.mask);

            BitmapDrawable largeIconDrawable = (BitmapDrawable) res.getDrawable(R.drawable.icon);
            Bitmap largeIconBitmap = largeIconDrawable.getBitmap();
            nb.setLargeIcon(largeIconBitmap);

            // `VERSION_CODES.O` means SDK 26
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel mChannel = null;
                mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
                mChannel.setDescription("");
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.GREEN);
                mChannel.enableVibration(false);
                mgr.createNotificationChannel(mChannel);

                nb.setChannelId(CHANNEL_ID);
            }

            mgr.notify(NOTIFICATION_ID, nb.build());
            return false;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private boolean isNotificationListenerEnabled()
    {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (flat == null)
            return false;
        ComponentName component = new ComponentName(this, NotificationService.class);
        return flat.contains(component.flattenToString());
    }
}
