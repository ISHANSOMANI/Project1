package com.ishan.studyclock;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(11,18,32);
    private static final int SURFACE = Color.rgb(17,27,46);
    private static final int CARD = Color.rgb(22,36,59);
    private static final int TEAL = Color.rgb(95,199,193);
    private static final int BLUE = Color.rgb(122,167,255);
    private static final int TEXT = Color.rgb(234,242,255);
    private static final int MUTED = Color.rgb(143,163,189);
    private static final int LINE = Color.rgb(43,61,86);

    private StudyDb db;
    private SharedPreferences timerPrefs;
    private FrameLayout content;
    private Handler uiHandler;
    private TextView timerText;
    private TextView timerState;
    private TextView todayText;
    private Spinner courseSpinner;
    private EditText topicInput;
    private Button startPauseButton;
    private Button stopButton;

    private final Runnable uiTick = new Runnable() {
        @Override public void run() {
            updateTimerUi();
            uiHandler.postDelayed(this, 500);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        db = new StudyDb(this);
        timerPrefs = getSharedPreferences("timer", MODE_PRIVATE);
        uiHandler = new Handler(Looper.getMainLooper());
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
        }
        buildShell();
        showTimerPage();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(12), dp(16), dp(10));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("StudyClock", 24, TEXT, true);
        TextView sub = text("TIET · Pool B · Semester 1", 12, MUTED, false);
        head.addView(title); head.addView(sub);
        root.addView(head, new LinearLayout.LayoutParams(-1, -2));

        content = new FrameLayout(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, 0, 1f);
        cp.topMargin = dp(10);
        root.addView(content, cp);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(8), 0, 0);
        nav.addView(navButton("Clock", v -> showTimerPage()), navLp());
        nav.addView(navButton("Records", v -> showRecordsPage()), navLp());
        nav.addView(navButton("Courses", v -> showCoursesPage()), navLp());
        nav.addView(navButton("Stats", v -> showStatsPage()), navLp());
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(62)));

        setContentView(root);
    }

    private LinearLayout.LayoutParams navLp() { return new LinearLayout.LayoutParams(0, -1, 1f); }

    private Button navButton(String label, View.OnClickListener l) {
        Button b = button(label, false); b.setTextSize(12); b.setOnClickListener(l); return b;
    }

    private void showTimerPage() {
        content.removeAllViews();
        ScrollView sc = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(0, dp(10), 0, dp(24));
        sc.addView(page, new ScrollView.LayoutParams(-1, -2));

        TextView kicker = text("FOCUS CLOCK", 12, TEAL, true);
        page.addView(kicker);

        timerText = text("00:00:00", 54, TEXT, true);
        timerText.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        timerText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(105));
        tp.topMargin = dp(8);
        page.addView(timerText, tp);

        timerState = text("Ready when you are", 14, MUTED, false);
        timerState.setGravity(Gravity.CENTER);
        page.addView(timerState, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout form = card();
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(20);
        page.addView(form, fp);

        form.addView(label("Course"));
        courseSpinner = new Spinner(this);
        List<StudyDb.Course> courses = db.getCourses();
        ArrayAdapter<StudyDb.Course> adapter = new ArrayAdapter<StudyDb.Course>(this, android.R.layout.simple_spinner_dropdown_item, courses) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.rgb(17,27,46)); tv.setTextSize(16); tv.setPadding(dp(8),0,dp(8),0); return tv;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Color.BLACK); tv.setTextSize(16); return tv;
            }
        };
        courseSpinner.setAdapter(adapter);
        courseSpinner.setBackgroundColor(Color.rgb(235,240,246));
        form.addView(courseSpinner, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView tl = label("Topic / chapter (optional)");
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1,-2); tlp.topMargin=dp(14); form.addView(tl, tlp);
        topicInput = new EditText(this);
        topicInput.setTextColor(TEXT); topicInput.setHintTextColor(MUTED); topicInput.setHint("e.g. Wave optics");
        topicInput.setSingleLine(true); topicInput.setTextSize(16); topicInput.setPadding(dp(12),0,dp(12),0);
        topicInput.setBackground(round(CARD, LINE, 12));
        form.addView(topicInput, new LinearLayout.LayoutParams(-1, dp(52)));

        LinearLayout controls = new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, dp(58)); clp.topMargin=dp(18);
        startPauseButton = button("Start studying", true);
        stopButton = button("Stop & save", false);
        controls.addView(startPauseButton, new LinearLayout.LayoutParams(0,-1,1.3f));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0,-1,1f); sp.leftMargin=dp(10); controls.addView(stopButton, sp);
        form.addView(controls, clp);
        startPauseButton.setOnClickListener(v -> handleStartPause());
        stopButton.setOnClickListener(v -> stopTimer());

        LinearLayout today = card();
        LinearLayout.LayoutParams tdp = new LinearLayout.LayoutParams(-1,-2); tdp.topMargin=dp(14); page.addView(today, tdp);
        TextView tdLabel = text("TODAY", 11, MUTED, true); today.addView(tdLabel);
        todayText = text("0h 00m", 28, TEXT, true); today.addView(todayText);
        TextView hint = text("Every stopped session is saved automatically to its course.", 13, MUTED, false); today.addView(hint);

        content.addView(sc);
        uiHandler.removeCallbacks(uiTick); uiHandler.post(uiTick);
    }

    private void handleStartPause() {
        boolean active = timerPrefs.getBoolean("active", false);
        boolean paused = timerPrefs.getBoolean("paused", false);
        Intent i = new Intent(this, StudyTimerService.class);
        if (!active) {
            Object selected = courseSpinner.getSelectedItem();
            if (!(selected instanceof StudyDb.Course)) { Toast.makeText(this, "Choose a course", Toast.LENGTH_SHORT).show(); return; }
            StudyDb.Course c = (StudyDb.Course) selected;
            i.setAction(StudyTimerService.ACTION_START);
            i.putExtra("courseId", c.id); i.putExtra("courseCode", c.code); i.putExtra("courseName", c.name);
            i.putExtra("topic", topicInput.getText().toString().trim());
            startForegroundService(i);
        } else {
            i.setAction(paused ? StudyTimerService.ACTION_RESUME : StudyTimerService.ACTION_PAUSE);
            startService(i);
        }
        uiHandler.postDelayed(this::updateTimerUi, 150);
    }

    private void stopTimer() {
        if (!timerPrefs.getBoolean("active", false)) { Toast.makeText(this, "No active session", Toast.LENGTH_SHORT).show(); return; }
        Intent i = new Intent(this, StudyTimerService.class); i.setAction(StudyTimerService.ACTION_STOP); startService(i);
        Toast.makeText(this, "Session saved", Toast.LENGTH_SHORT).show();
        uiHandler.postDelayed(this::updateTimerUi, 250);
    }

    private void updateTimerUi() {
        if (timerText == null || todayText == null) return;
        boolean active = timerPrefs.getBoolean("active", false), paused = timerPrefs.getBoolean("paused", false);
        long ms = elapsedNow();
        timerText.setText(formatHms(ms));
        if (active) {
            String code = timerPrefs.getString("courseCode", "Study");
            timerState.setText((paused ? "Paused" : "Studying") + " · " + code);
            startPauseButton.setText(paused ? "Resume" : "Pause");
            stopButton.setEnabled(true); stopButton.setAlpha(1f);
            courseSpinner.setEnabled(false); topicInput.setEnabled(false);
        } else {
            timerState.setText("Ready when you are");
            startPauseButton.setText("Start studying");
            stopButton.setEnabled(false); stopButton.setAlpha(.55f);
            courseSpinner.setEnabled(true); topicInput.setEnabled(true);
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY,0); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0);
        long from=cal.getTimeInMillis(), to=from+24L*60*60*1000;
        long total=db.getTotalBetween(from,to) + (active ? ms : 0);
        todayText.setText(formatHuman(total));
    }

    private long elapsedNow() {
        long acc = timerPrefs.getLong("accumulatedMs", 0L);
        if (timerPrefs.getBoolean("active", false) && !timerPrefs.getBoolean("paused", false)) {
            acc += Math.max(0, SystemClock.elapsedRealtime() - timerPrefs.getLong("lastStartElapsed", SystemClock.elapsedRealtime()));
        }
        return acc;
    }

    private void showRecordsPage() {
        uiHandler.removeCallbacks(uiTick); timerText=null; todayText=null;
        content.removeAllViews();
        ScrollView sc = new ScrollView(this); LinearLayout page = columnPage();
        page.addView(sectionTitle("Study records"));
        List<StudyDb.SessionRow> rows = db.getRecentSessions(50);
        if (rows.isEmpty()) page.addView(empty("No sessions yet. Start the clock and your records will appear here."));
        SimpleDateFormat f = new SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault());
        for (StudyDb.SessionRow r : rows) {
            LinearLayout c = card();
            TextView h = text(r.code + " · " + r.name, 15, TEXT, true); c.addView(h);
            if (r.topic != null && !r.topic.trim().isEmpty()) c.addView(text(r.topic, 14, BLUE, false));
            c.addView(text(f.format(new Date(r.startTime)) + "   •   " + formatHuman(r.durationMs), 12, MUTED, false));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(10); page.addView(c,lp);
        }
        sc.addView(page); content.addView(sc);
    }

    private void showCoursesPage() {
        uiHandler.removeCallbacks(uiTick); timerText=null; todayText=null;
        content.removeAllViews();
        ScrollView sc=new ScrollView(this); LinearLayout page=columnPage();
        page.addView(sectionTitle("Pool B courses"));
        page.addView(text("Preloaded from TIET's 2025 first-year scheme · 18 credits",13,MUTED,false));
        List<StudyDb.CourseTotal> totals=db.getCourseTotals();
        List<StudyDb.Course> courses=db.getCourses();
        for(int i=0;i<courses.size();i++){
            StudyDb.Course c=courses.get(i); StudyDb.CourseTotal t=totals.stream().filter(x->x.code.equals(c.code)).findFirst().orElse(null);
            LinearLayout card=card(); card.addView(text(c.code,12,TEAL,true)); card.addView(text(c.name,18,TEXT,true));
            card.addView(text(String.format(Locale.US,"%.1f credits   •   %s studied",c.credits,formatHuman(t==null?0:t.durationMs)),12,MUTED,false));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(10); page.addView(card,lp);
        }
        sc.addView(page); content.addView(sc);
    }

    private void showStatsPage() {
        uiHandler.removeCallbacks(uiTick); timerText=null; todayText=null;
        content.removeAllViews();
        ScrollView sc=new ScrollView(this); LinearLayout page=columnPage(); page.addView(sectionTitle("Analysis"));
        Calendar cal=Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY,0); cal.set(Calendar.MINUTE,0); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0);
        long todayStart=cal.getTimeInMillis(); long today=db.getTotalBetween(todayStart,todayStart+86400000L);
        long week=db.getTotalBetween(todayStart-6L*86400000L,todayStart+86400000L);
        LinearLayout hero=card(); hero.addView(text("THIS WEEK",11,MUTED,true)); hero.addView(text(formatHuman(week),34,TEXT,true)); hero.addView(text("Today  " + formatHuman(today),14,TEAL,true)); page.addView(hero);
        TextView ct=text("By course",16,TEXT,true); LinearLayout.LayoutParams ctp=new LinearLayout.LayoutParams(-1,-2); ctp.topMargin=dp(18); page.addView(ct,ctp);
        List<StudyDb.CourseTotal> totals=db.getCourseTotals(); long max=1; for(StudyDb.CourseTotal x:totals) max=Math.max(max,x.durationMs);
        for(StudyDb.CourseTotal x:totals){
            LinearLayout block=new LinearLayout(this); block.setOrientation(LinearLayout.VERTICAL); block.setPadding(0,dp(8),0,dp(8));
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            TextView name=text(x.code+" · "+x.name,13,TEXT,true); row.addView(name,new LinearLayout.LayoutParams(0,-2,1f)); row.addView(text(formatHuman(x.durationMs),12,MUTED,false)); block.addView(row);
            ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); pb.setMax(1000); pb.setProgress((int)Math.round(1000.0*x.durationMs/max)); pb.getProgressDrawable().setTint(TEAL); pb.getProgressDrawable().setColorFilter(TEAL,android.graphics.PorterDuff.Mode.SRC_IN);
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(8)); pp.topMargin=dp(6); block.addView(pb,pp); page.addView(block);
        }
        sc.addView(page); content.addView(sc);
    }

    private LinearLayout columnPage(){ LinearLayout p=new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setPadding(0,dp(12),0,dp(24)); return p; }
    private TextView sectionTitle(String s){ TextView t=text(s,28,TEXT,true); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(12); t.setLayoutParams(lp); return t; }
    private TextView empty(String s){ TextView t=text(s,15,MUTED,false); t.setPadding(0,dp(30),0,0); return t; }
    private TextView label(String s){ TextView t=text(s,12,MUTED,true); t.setPadding(0,0,0,dp(7)); return t; }
    private LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16),dp(16),dp(16),dp(16)); c.setBackground(round(SURFACE,LINE,18)); return c; }
    private TextView text(String s,float sp,int color,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); t.setLineSpacing(0,1.08f); return t; }
    private Button button(String s,boolean primary){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(15); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setTextColor(primary?BG:TEXT); b.setBackground(round(primary?TEAL:CARD,primary?TEAL:LINE,14)); return b; }
    private GradientDrawable round(int fill,int stroke,int r){ GradientDrawable g=new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(r)); g.setStroke(dp(1),stroke); return g; }
    private int dp(int x){ return Math.round(x*getResources().getDisplayMetrics().density); }
    private static String formatHms(long ms){ long sec=Math.max(0,ms/1000),h=sec/3600,m=(sec%3600)/60,s=sec%60; return String.format(Locale.US,"%02d:%02d:%02d",h,m,s); }
    private static String formatHuman(long ms){ long min=Math.max(0,Math.round(ms/60000f)); long h=min/60,m=min%60; return h>0 ? String.format(Locale.US,"%dh %02dm",h,m) : String.format(Locale.US,"%dm",m); }

    @Override protected void onResume(){ super.onResume(); if(timerText!=null){ uiHandler.removeCallbacks(uiTick); uiHandler.post(uiTick);} }
    @Override protected void onPause(){ super.onPause(); }
    @Override protected void onDestroy(){ uiHandler.removeCallbacks(uiTick); super.onDestroy(); }
}
