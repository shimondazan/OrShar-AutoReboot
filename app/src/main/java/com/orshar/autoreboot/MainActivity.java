package com.orshar.autoreboot;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.os.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView statusText, infoText;
    private Button saveBtn, deviceOwnerBtn;
    private TimePicker timePicker;
    private SharedPreferences prefs;
    private DevicePolicyManager dpm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AutoReboot", MODE_PRIVATE);
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        statusText = findViewById(R.id.statusText);
        infoText = findViewById(R.id.infoText);
        timePicker = findViewById(R.id.timePicker);
        saveBtn = findViewById(R.id.saveBtn);
        deviceOwnerBtn = findViewById(R.id.deviceOwnerBtn);

        timePicker.setIs24HourView(true);
        timePicker.setHour(prefs.getInt("reboot_hour", 3));
        timePicker.setMinute(prefs.getInt("reboot_minute", 0));

        saveBtn.setOnClickListener(v -> {
            prefs.edit()
                .putInt("reboot_hour", timePicker.getHour())
                .putInt("reboot_minute", timePicker.getMinute())
                .apply();
            startService();
            updateStatus();
            Toast.makeText(this, "✅ הגדרות נשמרו והשירות הופעל", Toast.LENGTH_SHORT).show();
        });

        deviceOwnerBtn.setOnClickListener(v -> {
            String cmd = "adb shell dpm set-device-owner com.orshar.autoreboot/.AdminReceiver";
            infoText.setText("פקודת ADB:\n" + cmd + "\n\n(הפעל פעם אחת בלבד מ-PC לאחר Factory Reset ולפני הוספת חשבון Google)");
        });

        startService();
        updateStatus();
    }

    private void startService() {
        Intent intent = new Intent(this, RebootService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void updateStatus() {
        boolean isOwner = dpm.isDeviceOwnerApp(getPackageName());
        boolean serviceRunning = isServiceRunning();
        int h = prefs.getInt("reboot_hour", 3);
        int m = prefs.getInt("reboot_minute", 0);

        String ownerStatus = isOwner ? "✅ Device Owner" : "⚠️ לא Device Owner";
        String svcStatus = serviceRunning ? "✅ שירות פעיל" : "❌ שירות כבוי";
        statusText.setText(ownerStatus + "\n" + svcStatus +
            String.format("\nאתחול מתוזמן: %02d:%02d", h, m));
    }

    private boolean isServiceRunning() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo svc : am.getRunningServices(Integer.MAX_VALUE)) {
            if (RebootService.class.getName().equals(svc.service.getClassName())) return true;
        }
        return false;
    }
}
