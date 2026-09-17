package com.ishan.studyclock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class StudyDb extends SQLiteOpenHelper {
    public static final class Course {
        public final long id;
        public final String code;
        public final String name;
        public final double credits;
        Course(long id, String code, String name, double credits) {
            this.id = id; this.code = code; this.name = name; this.credits = credits;
        }
        @Override public String toString() { return code + " · " + name; }
    }

    public static final class SessionRow {
        public final long id;
        public final String code;
        public final String name;
        public final String topic;
        public final long startTime;
        public final long durationMs;
        SessionRow(long id, String code, String name, String topic, long startTime, long durationMs) {
            this.id=id; this.code=code; this.name=name; this.topic=topic; this.startTime=startTime; this.durationMs=durationMs;
        }
    }

    public static final class CourseTotal {
        public final String code;
        public final String name;
        public final long durationMs;
        CourseTotal(String code, String name, long durationMs) {
            this.code=code; this.name=name; this.durationMs=durationMs;
        }
    }

    public StudyDb(Context c) { super(c, "studyclock.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE courses (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT UNIQUE NOT NULL, name TEXT NOT NULL, credits REAL NOT NULL)");
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, courseId INTEGER, courseCode TEXT NOT NULL, courseName TEXT NOT NULL, topic TEXT, startTime INTEGER NOT NULL, durationMs INTEGER NOT NULL)");
        seed(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    private void seed(SQLiteDatabase db) {
        addCourse(db, "UPH013", "Physics", 4.5);
        addCourse(db, "UES101", "Engineering Drawing", 4.0);
        addCourse(db, "UHU003", "Professional Communication", 3.0);
        addCourse(db, "UES102", "Manufacturing Processes", 3.0);
        addCourse(db, "UMA023", "Differential Equations & Linear Algebra", 3.5);
    }

    private void addCourse(SQLiteDatabase db, String code, String name, double credits) {
        ContentValues v = new ContentValues();
        v.put("code", code); v.put("name", name); v.put("credits", credits);
        db.insertWithOnConflict("courses", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<Course> getCourses() {
        ArrayList<Course> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,code,name,credits FROM courses ORDER BY id", null)) {
            while (c.moveToNext()) out.add(new Course(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3)));
        }
        return out;
    }

    public long insertSession(long courseId, String code, String name, String topic, long startTime, long durationMs) {
        ContentValues v = new ContentValues();
        v.put("courseId", courseId); v.put("courseCode", code); v.put("courseName", name);
        v.put("topic", topic == null ? "" : topic.trim()); v.put("startTime", startTime); v.put("durationMs", durationMs);
        return getWritableDatabase().insert("sessions", null, v);
    }

    public List<SessionRow> getRecentSessions(int limit) {
        ArrayList<SessionRow> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,courseCode,courseName,topic,startTime,durationMs FROM sessions ORDER BY startTime DESC LIMIT ?",
                new String[]{String.valueOf(limit)})) {
            while (c.moveToNext()) out.add(new SessionRow(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getLong(5)));
        }
        return out;
    }

    public long getTotalBetween(long from, long to) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(durationMs),0) FROM sessions WHERE startTime>=? AND startTime<?",
                new String[]{String.valueOf(from), String.valueOf(to)})) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public List<CourseTotal> getCourseTotals() {
        ArrayList<CourseTotal> out = new ArrayList<>();
        String sql = "SELECT c.code,c.name,COALESCE(SUM(s.durationMs),0) total " +
                "FROM courses c LEFT JOIN sessions s ON c.id=s.courseId GROUP BY c.id ORDER BY total DESC,c.id";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) out.add(new CourseTotal(c.getString(0), c.getString(1), c.getLong(2)));
        }
        return out;
    }
}
