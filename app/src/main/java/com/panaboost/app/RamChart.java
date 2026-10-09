package com.panaboost.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.ArrayDeque;

public final class RamChart extends View {
    private final ArrayDeque<Float> samples = new ArrayDeque<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int purple = 0xFFAB7AFF, grid = 0xFF363149;
    private final Path path = new Path();
    public RamChart(Context c) {super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    public void put(double value){if(samples.size()>=32)samples.removeFirst();samples.addLast((float)Math.max(0,Math.min(100,value)));invalidate();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        paint.setColor(grid);paint.setStrokeWidth(1f);paint.setStyle(Paint.Style.STROKE);
        for(int i=1;i<=3;i++){float y=h*i/4f;canvas.drawLine(0,y,w,y,paint);}
        if(samples.size()<2)return;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setColor(purple);
        path.reset();int i=0;for(float v:samples){float x=w*i/(float)(samples.size()-1);float y=h*(1-v/100f);if(i++==0)path.moveTo(x,y);else path.lineTo(x,y);}canvas.drawPath(path,paint);
        paint.setStrokeWidth(1f);paint.setStyle(Paint.Style.FILL);paint.setColor(0x33AB7AFF);
        Path fill=new Path(path);fill.lineTo(w,h);fill.lineTo(0,h);fill.close();canvas.drawPath(fill,paint);
    }
}
