package com.panaboost.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class SessionLog {
    public static final class Record {
        public final String game;
        public final long start, end;
        Record(String g,long s,long e){ game=g;start=s;end=e; }
        public String display() {
            long minutes = Math.max(1,(end-start+59999)/60000);
            return game + "  ·  " + minutes + " min  ·  " + DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(start));
        }
    }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("panaboost",0); }
    public static long startedAt(Context c) { return prefs(c).getLong("session_start",0); }
    public static String activeName(Context c) { return prefs(c).getString("session_name", "Juego"); }
    public static void start(Context c, String game) {
        if (startedAt(c) != 0) stop(c);
        prefs(c).edit().putLong("session_start",System.currentTimeMillis()).putString("session_name",game).apply();
    }
    public static void stop(Context c) {
        long started=startedAt(c);
        if (started==0) return;
        JSONArray log;
        try {log=new JSONArray(prefs(c).getString("sessions","[]"));} catch(Exception e){log=new JSONArray();}
        JSONObject record=new JSONObject();
        try {
            record.put("name",activeName(c));record.put("start",started);record.put("end",System.currentTimeMillis());
            JSONArray updated=new JSONArray();updated.put(record);
            for(int i=0;i<Math.min(log.length(),49);i++) updated.put(log.get(i));
            prefs(c).edit().putString("sessions",updated.toString()).remove("session_start").remove("session_name").apply();
        }catch(Exception ignored){}
    }
    public static List<Record> all(Context c) {
        List<Record> data=new ArrayList<>();
        try {
            JSONArray a=new JSONArray(prefs(c).getString("sessions","[]"));
            for(int i=0;i<a.length();i++){
                JSONObject j=a.getJSONObject(i);
                data.add(new Record(j.optString("name","Juego"),j.optLong("start"),j.optLong("end")));
            }
        }catch(Exception ignored){}
        return data;
    }
    public static String exportText(Context c){
        StringBuilder b=new StringBuilder("PanaBoost Pro — historial de sesiones\n\n");
        for(Record r:all(c))b.append(r.display()).append('\n');
        return b.toString();
    }
}
