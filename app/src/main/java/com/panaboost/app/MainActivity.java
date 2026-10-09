package com.panaboost.app;

import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MainActivity extends AppCompatActivity {
    private static final int BG=0xFF090B15, SURFACE=0xFF161727, CARD2=0xFF1E2035, WHITE=0xFFF7F5FF,
        MUTED=0xFFA8A8C0, GREEN=0xFF5CE5BE, GOLD=0xFFFFCA80, RED=0xFFFF859B;
    private static final int[] COLORS={0xFFA779FF,0xFF55C9FF,0xFF55E7B8,0xFFFFA363,0xFFFF719E};
    private static final String[] COLOR_NAMES={"Nebulosa violeta","Azul eléctrico","Esmeralda","Ámbar","Magenta"};
    private int accent=COLORS[0], tab=0;
    private LinearLayout root,page,nav;
    private TextView homeRam,homeBattery,homeStorage,homeThermal,sessionText,networkResult;
    private RamChart chart;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private DeviceMonitor.Sample sample;
    private SharedPreferences pref;
    private List<GameCatalog.Game> installed=new ArrayList<>();
    private boolean inForeground=false;
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!inForeground)return;
        sample=DeviceMonitor.read(MainActivity.this);
        updateLiveWidgets();
        handler.postDelayed(this,5000);
    }};

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        pref=getSharedPreferences("panaboost",MODE_PRIVATE);
        int idx=Math.max(0,Math.min(COLORS.length-1,pref.getInt("accent",0)));
        accent=COLORS[idx];
        sample=DeviceMonitor.read(this);
        installed=GameCatalog.installed(this);
        Window window=getWindow();window.setStatusBarColor(BG);window.setNavigationBarColor(BG);
        window.getDecorView().setSystemUiVisibility(0);
        buildShell();render();
    }
    @Override protected void onResume(){super.onResume();inForeground=true;handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){inForeground=false;handler.removeCallbacks(tick);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacks(tick);super.onDestroy();}

    private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
    private int mix(int a,int b,float bWeight){
        float aw=1f-bWeight;
        return Color.rgb((int)(Color.red(a)*aw+Color.red(b)*bWeight),
            (int)(Color.green(a)*aw+Color.green(b)*bWeight),
            (int)(Color.blue(a)*aw+Color.blue(b)*bWeight));
    }
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable gradient(int a,int b,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(dp(radius));return g;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void add(LinearLayout l,View v){l.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    private void gap(LinearLayout l,int height){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setIncludeFontPadding(true);return t;}
    private TextView center(String s,int size,int color,boolean bold){TextView t=txt(s,size,color,bold);t.setGravity(Gravity.CENTER);return t;}
    private void pad(View v,int h,int ver){v.setPadding(dp(h),dp(ver),dp(h),dp(ver));}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private MaterialCardView card(){
        MaterialCardView c=new MaterialCardView(this);
        c.setCardBackgroundColor(SURFACE);c.setRadius(dp(22));
        c.setStrokeColor(0xFF292B41);c.setStrokeWidth(dp(1));
        c.setCardElevation(0);c.setUseCompatPadding(false);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);page.addView(c,p);
        return c;
    }
    private LinearLayout body(MaterialCardView c){LinearLayout b=vertical();pad(b,18,18);c.addView(b);return b;}
    private void title(LinearLayout b,String heading,String description){
        add(b,txt(heading,18,WHITE,true));
        if(description!=null&&!description.isEmpty()){gap(b,5);add(b,txt(description,12,MUTED,false));}
        gap(b,16);
    }
    private MaterialButton button(String s,boolean primary,Runnable cb){
        MaterialButton b=new MaterialButton(this);
        b.setText(s);b.setTextSize(13);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setCornerRadius(dp(15));b.setMinHeight(dp(46));
        b.setInsetTop(0);b.setInsetBottom(0);
        b.setBackgroundTintList(ColorStateList.valueOf(primary?accent:CARD2));
        b.setTextColor(WHITE);
        b.setPadding(dp(12),dp(8),dp(12),dp(8));
        b.setOnClickListener(v->cb.run());return b;
    }
    private void fullButton(LinearLayout p,String s,boolean primary,Runnable cb){
        MaterialButton b=button(s,primary,cb);add(p,b);gap(p,8);
    }
    private void line(LinearLayout p){View v=new View(this);v.setBackgroundColor(0xFF2D2B41);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(10);lp.bottomMargin=dp(12);p.addView(v,lp);}
    private TextView pill(String s,int color){TextView t=txt(s,11,color,true);t.setBackground(shape(0xFF24233A,14));t.setPadding(dp(11),dp(7),dp(11),dp(7));return t;}
    private void settingRow(LinearLayout p,String s,String desc,Runnable action){
        LinearLayout row=horizontal();LinearLayout labels=vertical();add(labels,txt(s,14,WHITE,true));gap(labels,3);add(labels,txt(desc,11,MUTED,false));
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=txt("›",29,accent,true);arrow.setPadding(dp(8),0,0,0);row.addView(arrow);
        row.setPadding(0,dp(8),0,dp(8));add(p,row);row.setOnClickListener(v->action.run());line(p);
    }
    private void switchRow(LinearLayout p,String s,String desc,String key,boolean def){
        LinearLayout row=horizontal();LinearLayout labels=vertical();add(labels,txt(s,14,WHITE,true));gap(labels,4);add(labels,txt(desc,11,MUTED,false));
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        SwitchMaterial sw=new SwitchMaterial(this);sw.setChecked(pref.getBoolean(key,def));
        sw.setButtonTintList(null);
        sw.setOnCheckedChangeListener((v,checked)->pref.edit().putBoolean(key,checked).apply());
        row.addView(sw);add(p,row);line(p);
    }
    private void buildShell(){
        root=vertical();root.setBackgroundColor(mix(accent,BG,0.94f));
        if(Build.VERSION.SDK_INT>=35){
            root.setOnApplyWindowInsetsListener((v,ins)->{
                android.graphics.Insets top=ins.getInsets(WindowInsets.Type.statusBars());
                android.graphics.Insets bottom=ins.getInsets(WindowInsets.Type.navigationBars());
                root.setPadding(0,top.top,0,bottom.bottom);
                return ins;
            });
        }
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        page=vertical();page.setPadding(dp(17),dp(15),dp(17),dp(26));scroll.addView(page);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=horizontal();nav.setBackground(shape(0xFF121321,0));nav.setPadding(dp(8),dp(8),dp(8),dp(8));add(root,nav);
        setContentView(root);
    }
    private void drawNavigation(){
        nav.removeAllViews();String[] icons={"⌂","▦","◷","⚙"};String[] labels={"Inicio","Juegos","Análisis","Ajustes"};
        for(int i=0;i<4;i++){
            final int j=i;
            LinearLayout n=vertical();n.setGravity(Gravity.CENTER);n.setPadding(dp(5),dp(7),dp(5),dp(7));
            boolean selected=tab==i;n.setBackground(shape(selected?mix(accent,0xFF201B30,0.88f):0xFF121321,15));
            add(n,center(icons[i],23,selected?accent:MUTED,true));add(n,center(labels[i],10,selected?WHITE:MUTED,selected));
            nav.addView(n,new LinearLayout.LayoutParams(0,dp(61),1));n.setOnClickListener(v->{tab=j;render();});
        }
    }
    private void render(){
        if(page==null)return;
        page.removeAllViews();homeRam=null;homeBattery=null;homeStorage=null;homeThermal=null;sessionText=null;chart=null;networkResult=null;
        drawNavigation();
        LinearLayout header=horizontal();
        LinearLayout brand=vertical();add(brand,txt("ϟ  PANABOOST",27,WHITE,true));add(brand,txt("GAMING CONTROL CENTER  •  PRO 3.0",10,accent,true));
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));header.addView(pill("● EN LÍNEA",GREEN));
        add(page,header);gap(page,20);
        switch(tab){case 0:home();break;case 1:games();break;case 2:analysis();break;default:settings();}
    }
    private void subtitle(String h,String small){add(page,txt(h,23,WHITE,true));gap(page,4);add(page,txt(small,12,MUTED,false));gap(page,16);}
    private void item(LinearLayout b,String k,String val){
        LinearLayout l=horizontal();l.addView(txt(k,13,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));l.addView(txt(val,13,WHITE,true));add(b,l);gap(b,12);
    }
    private TextView metric(LinearLayout b,String label,String val){
        LinearLayout box=vertical();box.setBackground(shape(CARD2,17));pad(box,13,12);
        add(box,txt(label,11,MUTED,false));gap(box,5);TextView value=txt(val,17,WHITE,true);add(box,value);
        b.addView(box,new LinearLayout.LayoutParams(0,-2,1));return value;
    }
    private void home(){
        LinearLayout hero=vertical();hero.setBackground(gradient(mix(accent,0xFF242047,0.50f),0xFF1A2044,26));pad(hero,20,22);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.bottomMargin=dp(15);page.addView(hero,hp);
        add(hero,txt("CENTRO DE JUEGO",11,0xFFE4CAFF,true));gap(hero,10);
        add(hero,txt("Tu partida, bajo control.",24,WHITE,true));gap(hero,5);
        add(hero,txt("Prepara tu sesión, revisa el dispositivo y abre tus juegos.",13,0xFFE2D6F8,false));gap(hero,14);
        long sessionStart=SessionLog.startedAt(this);
        sessionText=txt(sessionStart==0?"● Listo para jugar":"● Sesión activa · "+SessionLog.activeName(this),13,sessionStart==0?GREEN:GOLD,true);
        add(hero,sessionText);gap(hero,14);
        String chosen=pref.getString("selected_pkg","");GameCatalog.Game g=GameCatalog.find(installed,chosen);
        if(g!=null){add(hero,txt("Juego seleccionado: "+g.name,13,0xFFE2D6F8,false));gap(hero,8);}
        fullButton(hero,sessionStart==0?"ϟ  INICIAR SESIÓN GAMING":"■  FINALIZAR SESIÓN",true,()->{
            if(SessionLog.startedAt(this)!=0){stopSession();render();}
            else launchPicker();
        });
        if(g!=null)fullButton(hero,"▶  ABRIR "+g.name.toUpperCase(Locale.getDefault()),false,()->startGame(g));
        fullButton(hero,"▦  ELEGIR OTRO JUEGO",false,()->launchPicker());

        subtitle("Dispositivo en vivo","Datos del sistema, actualizados cada 5 segundos");
        MaterialCardView info=card();LinearLayout inner=body(info);
        LinearLayout r1=horizontal();homeRam=metric(r1,"RAM disponible","—");gapSide(r1,8);homeBattery=metric(r1,"Batería","—");add(inner,r1);gap(inner,8);
        LinearLayout r2=horizontal();homeStorage=metric(r2,"Almacenamiento libre","—");gapSide(r2,8);homeThermal=metric(r2,"Estado térmico","—");add(inner,r2);
        gap(inner,16);add(inner,txt("Uso de memoria RAM  ·  últimas mediciones",12,MUTED,false));gap(inner,8);
        chart=new RamChart(this);inner.addView(chart,new LinearLayout.LayoutParams(-1,dp(88)));
        gap(inner,10);add(inner,txt("La gráfica muestra RAM del sistema, no FPS ni GPU.",11,MUTED,false));
        updateLiveWidgets();

        subtitle("Accesos rápidos","Acciones reales autorizadas por Android");
        MaterialCardView quick=card();LinearLayout qb=body(quick);
        fullButton(qb,"☾  CONFIGURAR NO MOLESTAR",false,()->openDnd());
        fullButton(qb,"◉  COMPROBAR RED",false,()->{tab=2;render();probe();});
        fullButton(qb,"✓  REVISIÓN ANTES DE JUGAR",false,()->preflight());
        fullButton(qb,"▣  ACTUALIZAR DIAGNÓSTICO",false,()->{sample=DeviceMonitor.read(this);updateLiveWidgets();toast("Diagnóstico actualizado");});
        add(qb,txt("PanaBoost no fuerza la CPU/GPU ni promete FPS imposibles. Protegemos la estabilidad del dispositivo.",12,MUTED,false));
    }
    private void gapSide(LinearLayout l,int width){View g=new View(this);l.addView(g,new LinearLayout.LayoutParams(dp(width),1));}
    private void preflight(){
        DeviceMonitor.Sample st=DeviceMonitor.read(this);
        StringBuilder sb=new StringBuilder();
        if(st.batteryLevel>=0&&st.batteryLevel<20)sb.append("• Batería baja ("+st.batteryLevel+"%). Considera cargar el teléfono.\n");
        else sb.append("• Batería: "+(st.batteryLevel>=0?st.batteryLevel+"%":"no disponible")+".\n");
        if(st.thermalStatus>=android.os.PowerManager.THERMAL_STATUS_SEVERE)
            sb.append("• Temperatura crítica del sistema: pausa y deja enfriar el dispositivo.\n");
        else sb.append("• Estado térmico: "+DeviceMonitor.thermal(st.thermalStatus)+".\n");
        if(st.totalRam>0&&st.freeRam<st.totalRam/10)
            sb.append("• RAM libre muy baja. Cierra manualmente apps pesadas si las estás usando.\n");
        else sb.append("• RAM libre: "+DeviceMonitor.size(st.freeRam)+".\n");
        sb.append("• Red: "+st.network+".\n\nEste chequeo no altera el rendimiento de la CPU/GPU ni los FPS.");
        new AlertDialog.Builder(this).setTitle("Revisión prepartida").setMessage(sb.toString())
            .setPositiveButton("Entendido",null).show();
    }
    private void updateLiveWidgets(){
        if(sample==null)return;
        if(homeRam!=null)homeRam.setText(DeviceMonitor.size(sample.freeRam));
        if(homeBattery!=null)homeBattery.setText(sample.batteryLevel>=0?sample.batteryLevel+"%":"—");
        if(homeStorage!=null)homeStorage.setText(DeviceMonitor.size(sample.freeStorage));
        if(homeThermal!=null)homeThermal.setText(DeviceMonitor.thermal(sample.thermalStatus));
        if(chart!=null)chart.put(sample.ramUsedPercent());
    }
    private void launchPicker(){
        List<GameCatalog.Game> picks=new ArrayList<>();
        Set<String> favorites=GameCatalog.favorites(this);
        for(GameCatalog.Game g:installed)if(g.identifiedAsGame||favorites.contains(g.pkg))picks.add(g);
        if(picks.isEmpty())picks.addAll(installed);
        if(picks.isEmpty()){toast("No se encontraron aplicaciones lanzables");return;}
        String[] labels=new String[picks.size()];for(int i=0;i<picks.size();i++)labels[i]=picks.get(i).name;
        new AlertDialog.Builder(this).setTitle("Elegir juego o aplicación").setItems(labels,(d,i)->{
            GameCatalog.Game g=picks.get(i);
            pref.edit().putString("selected_pkg",g.pkg).apply();
            new AlertDialog.Builder(this).setTitle("¿Iniciar sesión gaming?")
                .setMessage("Seleccionaste: "+g.name+"\n\nLa sesión registrará el tiempo que permanezca activa hasta que regreses a PanaBoost y la finalices.")
                .setPositiveButton("Iniciar y abrir",(x,y)->startGame(g))
                .setNegativeButton("Solo seleccionar",(x,y)->render()).show();
        }).setNeutralButton("Ver todas",(d,w)->{tab=1;render();}).show();
    }
    private void startGame(GameCatalog.Game g){
        pref.edit().putString("selected_pkg",g.pkg).apply();
        if(SessionLog.startedAt(this)==0){SessionLog.start(this,g.name);activateSessionDnd(g.pkg);}
        launch(g);
    }
    private void launch(GameCatalog.Game g){
        try {
            Intent intent=new Intent(Intent.ACTION_MAIN);intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName(g.pkg,g.activity);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }catch(Exception e){
            try{
                Intent i=getPackageManager().getLaunchIntentForPackage(g.pkg);
                if(i==null)throw new IllegalStateException();startActivity(i);
            }catch(Exception ignored){toast("No se pudo iniciar "+g.name+". Selecciona otra aplicación.");}
        }
    }
    private void stopSession(){SessionLog.stop(this);restoreSessionDnd();toast("Sesión finalizada y registrada");}
    private void activateSessionDnd(String pkg){
        if(!pref.getBoolean("profile_dnd_"+pkg,pref.getBoolean("auto_dnd",false)))return;
        try{
            NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if(!n.isNotificationPolicyAccessGranted()){toast("Concede acceso a No molestar en Ajustes");return;}
            pref.edit().putInt("prior_dnd",n.getCurrentInterruptionFilter()).apply();
            n.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
        }catch(Exception e){toast("El sistema bloqueó el cambio de No molestar");}
    }
    private void restoreSessionDnd(){
        int old=pref.getInt("prior_dnd",-1);if(old==-1)return;
        try{
            NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if(n.isNotificationPolicyAccessGranted())n.setInterruptionFilter(old);
        }catch(Exception ignored){}
        pref.edit().remove("prior_dnd").apply();
    }
    private void openDnd(){open(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);}

    private void games(){
        subtitle("Mi biblioteca gaming","Explora, destaca y ejecuta tus aplicaciones instaladas.");
        MaterialCardView top=card();LinearLayout b=body(top);
        title(b,"Juegos instalados","Marca favoritos, abre tus juegos y mantén pulsado su nombre para configurar un perfil por juego.");
        LinearLayout choose=horizontal();
        TextView count=txt(installed.size()+" aplicaciones detectadas",13,GREEN,true);choose.addView(count,new LinearLayout.LayoutParams(0,-2,1));
        add(choose,pill(GameCatalog.favorites(this).size()+" favoritos",accent));add(b,choose);gap(b,12);
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(WHITE);search.setHintTextColor(MUTED);search.setHint("Buscar juego o aplicación...");
        search.setTextSize(14);search.setPadding(dp(13),dp(12),dp(13),dp(12));search.setBackground(shape(CARD2,13));add(b,search);gap(b,12);
        SwitchMaterial showAll=new SwitchMaterial(this);showAll.setText("Mostrar todas las aplicaciones");showAll.setTextColor(MUTED);showAll.setTextSize(12);
        showAll.setChecked(pref.getBoolean("show_all",false));add(b,showAll);
        LinearLayout results=vertical();add(page,results);
        Runnable populate=()->fillGames(results,search.getText().toString(),showAll.isChecked());
        showAll.setOnCheckedChangeListener((x,checked)->{pref.edit().putBoolean("show_all",checked).apply();populate.run();});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int before,int count){populate.run();}public void afterTextChanged(Editable e){}});
        populate.run();
        MaterialCardView hint=card();LinearLayout hb=body(hint);
        add(hb,txt("¿No aparece tu juego?",16,WHITE,true));gap(hb,6);
        add(hb,txt("Activa «Mostrar todas las aplicaciones». Algunas marcas no etiquetan los juegos correctamente. Solo se listan aplicaciones que Android permite consultar.",12,MUTED,false));gap(hb,10);
        fullButton(hb,"↻  VOLVER A DETECTAR APPS",false,()->{installed=GameCatalog.installed(this);render();});
    }
    private void fillGames(LinearLayout list,String query,boolean showAll){
        list.removeAllViews();
        Set<String> fav=GameCatalog.favorites(this);
        List<GameCatalog.Game> candidates=new ArrayList<>();
        for(GameCatalog.Game g:installed){
            if(!showAll&&!g.identifiedAsGame&&!fav.contains(g.pkg))continue;
            if(!g.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))&&!g.pkg.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))continue;
            candidates.add(g);
        }
        candidates.sort((a,b)->{boolean fa=fav.contains(a.pkg),fb=fav.contains(b.pkg);if(fa!=fb)return fa?-1:1;if(a.identifiedAsGame!=b.identifiedAsGame)return a.identifiedAsGame?-1:1;return a.name.compareToIgnoreCase(b.name);});
        if(candidates.isEmpty()){
            TextView empty=txt(showAll?"Sin resultados. Prueba otro nombre.":"No se detectaron juegos automáticamente. Activa «Mostrar todas las aplicaciones».",13,MUTED,false);
            pad(empty,12,20);add(list,empty);return;
        }
        for(GameCatalog.Game g:candidates){
            MaterialCardView c=new MaterialCardView(this);c.setCardBackgroundColor(SURFACE);c.setRadius(dp(17));c.setStrokeColor(0xFF292B41);c.setStrokeWidth(dp(1));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(9);list.addView(c,cp);
            LinearLayout row=horizontal();pad(row,13,12);c.addView(row);
            TextView icon=center(g.identifiedAsGame?"✦":"▣",23,accent,true);icon.setBackground(shape(CARD2,14));
            row.addView(icon,new LinearLayout.LayoutParams(dp(45),dp(45)));gapSide(row,10);
            LinearLayout details=vertical();
            TextView name=txt(g.name,14,WHITE,true);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);add(details,name);
            add(details,txt(fav.contains(g.pkg)?"★ Favorito":(g.identifiedAsGame?"Juego detectado":"Aplicación"),11,MUTED,false));
            row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            TextView star=center(fav.contains(g.pkg)?"★":"☆",23,fav.contains(g.pkg)?GOLD:MUTED,true);
            row.addView(star,new LinearLayout.LayoutParams(dp(37),dp(43)));star.setOnClickListener(v->{GameCatalog.setFavorite(this,g.pkg,!GameCatalog.favorites(this).contains(g.pkg));fillGames(list,query,showAll);});
            MaterialButton play=button("JUGAR",true,()->startGame(g));play.setTextSize(11);play.setMinWidth(0);play.setPadding(dp(7),0,dp(7),0);
            row.addView(play,new LinearLayout.LayoutParams(dp(80),dp(42)));
            details.setOnClickListener(v->{pref.edit().putString("selected_pkg",g.pkg).apply();toast(g.name+" seleccionado");});
            details.setOnLongClickListener(v->{profileDialog(g);return true;});
        }
    }

    private void profileDialog(GameCatalog.Game g){
        boolean auto=pref.getBoolean("profile_dnd_"+g.pkg,pref.getBoolean("auto_dnd",false));
        new AlertDialog.Builder(this).setTitle("Perfil: "+g.name)
            .setMessage("El modo No molestar necesita autorización de Android. La sesión guarda el tiempo de juego hasta finalizarla.")
            .setSingleChoiceItems(new String[]{"No molestar desactivado", "No molestar en esta sesión"},auto?1:0,(d,index)->{
                pref.edit().putBoolean("profile_dnd_"+g.pkg,index==1).apply();d.dismiss();toast("Perfil guardado para "+g.name);
            }).setNegativeButton("Cancelar",null).show();
    }
    private void analysis(){
        subtitle("Analíticas reales","El estado del teléfono, red y registro de sesiones.");
        MaterialCardView dev=card();LinearLayout d=body(dev);title(d,"Monitor del dispositivo","Mediciones Android del sistema, no estimaciones inventadas.");
        if(sample==null)sample=DeviceMonitor.read(this);
        item(d,"Modelo",Build.MANUFACTURER+" "+Build.MODEL);
        item(d,"Android",Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")");
        item(d,"Procesadores disponibles",String.valueOf(Runtime.getRuntime().availableProcessors()));
        item(d,"RAM disponible / total",DeviceMonitor.size(sample.freeRam)+" / "+DeviceMonitor.size(sample.totalRam));
        item(d,"RAM usada",String.format(Locale.getDefault(),"%.1f%%",sample.ramUsedPercent()));
        item(d,"Almacenamiento libre / total",DeviceMonitor.size(sample.freeStorage)+" / "+DeviceMonitor.size(sample.totalStorage));
        item(d,"Batería",sample.batteryLevel<0?"No disponible":sample.batteryLevel+"%");
        item(d,"Temperatura de la batería",DeviceMonitor.temp(sample.batteryTempTenths));
        item(d,"Estado térmico Android",DeviceMonitor.thermal(sample.thermalStatus));
        item(d,"Ahorro de batería",sample.batterySaver?"Activado":"Desactivado");
        fullButton(d,"↻  VOLVER A MEDIR",true,()->{sample=DeviceMonitor.read(this);render();});
        MaterialCardView net=card();LinearLayout n=body(net);title(n,"Diagnóstico de conexión","La prueba conecta con un servicio de Google, no con los servidores del juego.");
        item(n,"Tipo de red",sample.network);
        networkResult=txt("Pulsa para comprobar el tiempo de respuesta.",13,MUTED,false);add(n,networkResult);gap(n,12);
        fullButton(n,"◉  PROBAR CONECTIVIDAD",false,()->probe());
        MaterialCardView usage=card();LinearLayout u=body(usage);title(u,"Uso de juegos hoy","Estadísticas aproximadas de Android, solo con autorización.");
        if(!usageGranted()){
            add(u,txt("Sin permiso. No se accede a tu historial de uso sin tu autorización.",12,MUTED,false));gap(u,10);
            fullButton(u,"CONCEDER ACCESO DE USO",true,()->open(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }else{
            List<Map.Entry<String,Long>> times=gameUsageToday();
            if(times.isEmpty())add(u,txt("No hay estadísticas de juegos disponibles hoy.",12,MUTED,false));
            int count=0;
            for(Map.Entry<String,Long> e:times){
                GameCatalog.Game g=GameCatalog.find(installed,e.getKey());if(g==null)continue;
                item(u,g.name,Math.max(1,e.getValue()/60000)+" min");if(++count==8)break;
            }
            fullButton(u,"ADMINISTRAR PERMISO",false,()->open(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
        MaterialCardView history=card();LinearLayout h=body(history);title(h,"Historial de sesiones","Sesiones registradas cuando las finalizas en PanaBoost.");
        List<SessionLog.Record> records=SessionLog.all(this);
        if(SessionLog.startedAt(this)!=0){add(h,txt("● Sesión activa: "+SessionLog.activeName(this),13,GREEN,true));gap(h,9);}
        if(records.isEmpty())add(h,txt("Aún no hay sesiones terminadas.",12,MUTED,false));
        else for(int i=0;i<Math.min(8,records.size());i++){
            add(h,txt("•  "+records.get(i).display(),12,WHITE,false));gap(h,11);
        }
        gap(h,10);
        fullButton(h,"COMPARTIR HISTORIAL",false,()->shareHistory());
    }
    private boolean usageGranted(){
        try{
            AppOpsManager ops=(AppOpsManager)getSystemService(APP_OPS_SERVICE);
            int mode=ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),getPackageName());
            return mode==AppOpsManager.MODE_ALLOWED;
        }catch(Exception e){return false;}
    }
    private List<Map.Entry<String,Long>> gameUsageToday(){
        Map<String,Long> map=new HashMap<>();
        try{
            UsageStatsManager manager=(UsageStatsManager)getSystemService(Context.USAGE_STATS_SERVICE);
            Calendar calendar=Calendar.getInstance();calendar.set(Calendar.HOUR_OF_DAY,0);calendar.set(Calendar.MINUTE,0);calendar.set(Calendar.SECOND,0);calendar.set(Calendar.MILLISECOND,0);
            List<UsageStats> all=manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,calendar.getTimeInMillis(),System.currentTimeMillis());
            if(all!=null)for(UsageStats st:all){
                if(st.getTotalTimeInForeground()>0)map.put(st.getPackageName(),map.getOrDefault(st.getPackageName(),0L)+st.getTotalTimeInForeground());
            }
        }catch(Exception ignored){}
        List<Map.Entry<String,Long>> entries=new ArrayList<>(map.entrySet());
        Set<String> saved=GameCatalog.favorites(this);
        entries.removeIf(e->{GameCatalog.Game g=GameCatalog.find(installed,e.getKey());return g==null||(!g.identifiedAsGame&&!saved.contains(g.pkg));});
        entries.sort((a,b)->Long.compare(b.getValue(),a.getValue()));return entries;
    }
    private void probe(){
        if(networkResult==null){tab=2;render();}
        TextView output=networkResult;
        if(output==null)return;output.setText("Conectando de forma segura...");
        new Thread(()->{
            long t=System.nanoTime();String result;
            HttpURLConnection con=null;
            try{
                URL url=new URL("https://www.google.com/generate_204");
                con=(HttpURLConnection)url.openConnection();
                con.setRequestMethod("HEAD");con.setConnectTimeout(4000);con.setReadTimeout(4000);
                int code=con.getResponseCode();
                long ms=Math.round((System.nanoTime()-t)/1000000.0);
                result="Respuesta HTTP "+code+" · "+ms+" ms al servicio de prueba (NO es ping del juego)";
            }catch(Exception e){result="Sin respuesta: "+e.getClass().getSimpleName();}
            finally{if(con!=null)con.disconnect();}
            String done=result;runOnUiThread(()->{if(networkResult==output)output.setText(done);});
        }).start();
    }
    private void shareHistory(){
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,SessionLog.exportText(this));
        startActivity(Intent.createChooser(i,"Compartir historial"));
    }

    private void settings(){
        subtitle("Personaliza tu centro","Opciones que cambian el comportamiento de PanaBoost.");
        MaterialCardView style=card();LinearLayout sb=body(style);title(sb,"Apariencia","Personaliza el acento de toda la interfaz.");
        settingRow(sb,"Color principal","Tema: "+COLOR_NAMES[pref.getInt("accent",0)],()->colorPicker());
        MaterialCardView controls=card();LinearLayout cb=body(controls);title(cb,"Sesiones gaming","Configuración para la preparación de tus partidas.");
        switchRow(cb,"No molestar automático","Activa prioridad durante las sesiones si Android da permiso.","auto_dnd",false);
        settingRow(cb,"Permiso No molestar","Abre la autorización del sistema",()->openDnd());
        switchRow(cb,"Mostrar todas las apps","Incluye apps sin categoría de juego en la biblioteca.","show_all",false);
        MaterialCardView space=card();LinearLayout cl=body(space);title(cl,"Mantenimiento seguro","PanaBoost solo puede borrar sus propios archivos temporales.");
        fullButton(cl,"◫  BORRAR CACHÉ DE PANABOOST",false,()->{
            long bytes=wipeCache(getCacheDir());File ext=getExternalCacheDir();if(ext!=null)bytes+=wipeCache(ext);
            toast("Caché propia limpiada: "+bytes/1024+" KB");
        });
        settingRow(cl,"Ajustes de batería","Controlar ahorro de energía desde Android",()->open(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        settingRow(cl,"Ajustes de almacenamiento","Revisar almacenamiento desde Android",()->open(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        settingRow(cl,"Información de la aplicación","Permisos, batería y notificaciones",()->{
            Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()));startActivity(i);
        });
        MaterialCardView about=card();LinearLayout a=body(about);title(a,"PanaBoost Pro 3.0","Optimización responsable · datos medidos · sin root");
        add(a,txt("✓ Biblioteca y favoritos de juegos\n✓ Lanzador con sesiones y registro\n✓ RAM, batería, temperatura y estado térmico\n✓ Gráfica de uso real de RAM\n✓ Prueba de conectividad HTTPS\n✓ Tiempo de uso de juegos (con permiso)\n✓ Perfil visual y controles de No molestar\n✓ Caché propia y ajustes de Android",13,GREEN,false));
        gap(a,14);
        add(a,txt("Límites de Android: no es posible desbloquear FPS, overclockear CPU/GPU, limpiar cachés de otras aplicaciones ni acelerar la red mediante una app convencional. El tamaño del APK no indica la calidad de la optimización.",12,MUTED,false));
        gap(a,8);add(a,txt("ID independiente: com.panaboost.gamingpro. Se instala junto a versiones anteriores y evita conflictos de firma entre paquetes.",11,MUTED,false));
    }
    private void colorPicker(){
        new AlertDialog.Builder(this).setTitle("Elige el color principal").setSingleChoiceItems(COLOR_NAMES,pref.getInt("accent",0),(d,w)->{
            pref.edit().putInt("accent",w).apply();accent=COLORS[w];d.dismiss();render();
        }).setNegativeButton("Cancelar",null).show();
    }
    private long wipeCache(File f){
        if(f==null)return 0;long total=0;File[] children=f.listFiles();if(children==null)return 0;
        for(File sub:children){if(sub.isDirectory())total+=wipeCache(sub);else total+=sub.length();sub.delete();}return total;
    }
    private void open(String action){try{startActivity(new Intent(action));}catch(Exception e){toast("Este ajuste no está disponible en tu Android");}}
}
