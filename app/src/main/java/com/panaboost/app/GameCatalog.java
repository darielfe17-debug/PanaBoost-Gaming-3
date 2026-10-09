package com.panaboost.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GameCatalog {
    public static class Game {
        public final String name, pkg, activity;
        public final boolean identifiedAsGame;
        public Game(String n, String p, String a, boolean g) { name=n; pkg=p; activity=a; identifiedAsGame=g; }
    }
    public static List<Game> installed(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent q = new Intent(Intent.ACTION_MAIN);
        q.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> all = pm.queryIntentActivities(q, 0);
        List<Game> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo r : all) {
            if (r.activityInfo == null) continue;
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                boolean isGame = Build.VERSION.SDK_INT >= 26 && ai.category == ApplicationInfo.CATEGORY_GAME;
                String name = r.loadLabel(pm).toString();
                result.add(new Game(name, pkg, r.activityInfo.name, isGame));
            } catch (Exception ignored) { }
        }
        Collections.sort(result, (a,b) -> {
            if (a.identifiedAsGame != b.identifiedAsGame) return a.identifiedAsGame ? -1 : 1;
            return a.name.compareToIgnoreCase(b.name);
        });
        return result;
    }
    public static Set<String> favorites(Context c) {
        Set<String> out = new HashSet<>();
        try {
            JSONArray a = new JSONArray(c.getSharedPreferences("panaboost", 0).getString("favorites", "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        } catch (Exception ignored) { }
        return out;
    }
    public static void setFavorite(Context c, String pkg, boolean fav) {
        Set<String> set = favorites(c);
        if (fav) set.add(pkg); else set.remove(pkg);
        JSONArray json = new JSONArray();
        for (String name: set) json.put(name);
        c.getSharedPreferences("panaboost", 0).edit().putString("favorites", json.toString()).apply();
    }
    public static Game find(List<Game> games, String pkg) {
        for (Game g : games) if (g.pkg.equals(pkg)) return g;
        return null;
    }
}
