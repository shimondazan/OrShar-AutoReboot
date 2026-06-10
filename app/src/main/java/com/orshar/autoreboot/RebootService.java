package com.orshar.autoreboot;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import java.util.concurrent.*;

public class RebootService extends Service {
    private static final String TAG = "RebootService";
    private static final String CHANNEL_ID = "RebootChannel";
    private static final int NOTIF_ID = 1;

    // שרתי NTP פנימיים של OrShar
    private static final String[] NTP_SERVERS = {"100.1.1.101", "100.1.1.102"};

    private ScheduledExecutorService scheduler;
    private SharedPreferences prefs;
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("AutoReboot", MODE_PRIVATE);
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());

        syncTimeFromNtp();   // סנכרון בעת עליית השירות
        startScheduler();
    }

    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // בדיקת שעת אתחול כל דקה
        scheduler.scheduleAtFixedRate(() -> {
            Calendar now = Calendar.getInstance();
            int h = now.get(Calendar.HOUR_OF_DAY);
            int m = now.get(Calendar.MINUTE);
            int th = prefs.getInt("reboot_hour", 3);
            int tm = prefs.getInt("reboot_minute", 0);
            if (h == th && m == tm) {
                doReboot();
            }
        }, 0, 1, TimeUnit.MINUTES);

        // סנכרון NTP כל 6 שעות
        scheduler.scheduleAtFixedRate(this::syncTimeFromNtp, 6, 6, TimeUnit.HOURS);
    }

    private void syncTimeFromNtp() {
        new Thread(() -> {
            if (!dpm.isDeviceOwnerApp(getPackageName())) {
                Log.w(TAG, "Not device owner — NTP sync skipped");
                return;
            }
            SntpClient client = new SntpClient();
            for (String server : NTP_SERVERS) {
                if (client.requestTime(server, 5000)) {
                    long ntpTime = client.getNtpTime();
                    try {
                        // כיבוי זמן אוטומטי כדי ש-setTime יעבוד
                        dpm.setGlobalSetting(adminComponent, Settings.Global.AUTO_TIME, "0");
                        dpm.setTime(adminComponent, ntpTime);
                        Log.i(TAG, "Time synced from " + server);
                    } catch (Exception e) {
                        Log.e(TAG, "setTime failed", e);
                    }
                    return; // הצליח — לא ממשיכים לשרת הבא
                } else {
                    Log.w(TAG, "NTP server unreachable: " + server);
                }
            }
            Log.e(TAG, "All NTP servers failed");
        }).start();
    }

    private void doReboot() {
        Log.i(TAG, "Attempting reboot via Device Owner API");
        try {
            if (dpm.isDeviceOwnerApp(getPackageName())) {
                dpm.reboot(adminComponent);
            } else {
                showError("לא מוגדר כ-Device Owner");
            }
        } catch (Exception e) {
            Log.e(TAG, "Reboot failed", e);
        }
    }

    private void showError(String msg) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoReboot — שגיאה")
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build();
        nm.notify(2, n);
    }

    private Notification buildNotification() {
        int h = prefs.getInt("reboot_hour", 3);
        int m = prefs.getInt("reboot_minute", 0);
        String time = String.format("%02d:%02d", h, m);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OrShar AutoReboot פעיל")
            .setContentText("אתחול ב-" + time + " | סנכרון NTP פעיל")
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "AutoReboot Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() {
        if (scheduler != null) scheduler.shutdown();
        super.onDestroy();
    }
}