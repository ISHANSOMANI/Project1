package com.ishan.studyclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import java.util.Locale;

public class StudyTimerService extends Service {
    public static final String ACTION_START = "com.ishan.studyclock.START";
    public static final String ACTION_PAUSE = "com.ishan.studyclock.PAUSE";
    public static final String ACTION_RESUME = "com.ishan.studyclock.RESUME";
    public static final String ACTION_STOP = "com.ishan.studyclock.STOP";
    private static final int NOTIF_ID = 73;
    private static final String CHANNEL = "study_timer";
    private SharedPreferences p;
    private Handler handler;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (p.getBoolean("active", false)) {
                getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        p = getSharedPreferences("timer", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Study timer", NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String a = intent.getAction();
        if (ACTION_START.equals(a)) startNew(intent);
        else if (ACTION_PAUSE.equals(a)) pause();
        else if (ACTION_RESUME.equals(a)) resume();
        else if (ACTION_STOP.equals(a)) stopAndSave();
        return START_STICKY;
    }

    private void startNew(Intent i) {
        long nowElapsed = SystemClock.elapsedRealtime();
        long nowWall = System.currentTimeMillis();
        p.edit()
                .putBoolean("active", true).putBoolean("paused", false)
                .putLong("courseId", i.getLongExtra("courseId", -1))
                .putString("courseCode", i.getStringExtra("courseCode"))
                .putString("courseName", i.getStringExtra("courseName"))
                .putString("topic", i.getStringExtra("topic"))
                .putLong("sessionStartWall", nowWall)
                .putLong("lastStartElapsed", nowElapsed)
                .putLong("accumulatedMs", 0L)
                .apply();
        startForeground(NOTIF_ID, buildNotification());
        handler.removeCallbacks(ticker); handler.post(ticker);
    }

    private void pause() {
        if (!p.getBoolean("active", false) || p.getBoolean("paused", false)) return;
        long acc = currentElapsed();
        p.edit().putBoolean("paused", true).putLong("accumulatedMs", acc).apply();
        getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
    }

    private void resume() {
        if (!p.getBoolean("active", false) || !p.getBoolean("paused", false)) return;
        p.edit().putBoolean("paused", false).putLong("lastStartElapsed", SystemClock.elapsedRealtime()).apply();
        getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
    }

    private void stopAndSave() {
        if (!p.getBoolean("active", false)) { stopSelf(); return; }
        long ms = currentElapsed();
        if (ms >= 1000) {
            new StudyDb(this).insertSession(
                    p.getLong("courseId", -1),
                    safe(p.getString("courseCode", "")),
                    safe(p.getString("courseName", "Study")),
                    safe(p.getString("topic", "")),
                    p.getLong("sessionStartWall", System.currentTimeMillis()),
                    ms);
        }
        p.edit().clear().apply();
        handler.removeCallbacks(ticker);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private long currentElapsed() {
        long acc = p.getLong("accumulatedMs", 0L);
        if (p.getBoolean("active", false) && !p.getBoolean("paused", false)) {
            acc += Math.max(0, SystemClock.elapsedRealtime() - p.getLong("lastStartElapsed", SystemClock.elapsedRealtime()));
        }
        return acc;
    }

    private Notification buildNotification() {
        String course = safe(p.getString("courseCode", "Study"));
        String topic = safe(p.getString("topic", ""));
        String state = p.getBoolean("paused", false) ? "Paused" : "Studying";
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 1, launch, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(state + " · " + course)
                .setContentText(format(currentElapsed()) + (topic.isEmpty() ? "" : "  ·  " + topic))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pi)
                .build();
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String format(long ms) {
        long sec = Math.max(0, ms / 1000), h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { handler.removeCallbacks(ticker); super.onDestroy(); }
}
