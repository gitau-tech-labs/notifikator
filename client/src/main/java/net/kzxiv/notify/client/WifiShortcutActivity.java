package net.kzxiv.notify.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import net.kzxiv.notify.client.service.ForwarderService;
import net.kzxiv.notify.client.service.LogStore;

public class WifiShortcutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Silently start the forwarder service every time the icon is tapped.
        // This guarantees the service is running even if the OEM killed it.
        try {
            ForwarderService.start(this);
            LogStore.append(this, "SHORTCUT tapped — ForwarderService start requested");
        } catch (Exception e) {
            LogStore.append(this, "SHORTCUT error: " + e.getMessage());
        }

        // Immediately hand off to the real Android Wi-Fi settings screen.
        try {
            Intent wifi = new Intent(Settings.ACTION_WIFI_SETTINGS);
            wifi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(wifi);
        } catch (Exception e) {
            // Fallback: open the generic wireless settings if Wi-Fi isn't available
            try {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            } catch (Exception ignored) {}
        }

        // Close ourselves so we don't leave a background activity in the stack.
        finish();
    }
}
