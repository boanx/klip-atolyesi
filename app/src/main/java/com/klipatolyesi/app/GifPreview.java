package com.klipatolyesi.app;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

public class GifPreview extends View {
    private Movie movie;
    private long start;
    private boolean animating=true;
    public GifPreview(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }
    public void load(String path) { movie=Movie.decodeFile(path); start=SystemClock.uptimeMillis(); invalidate(); }
    public void clear() { movie=null; }
    public void setAnimating(boolean value) {if(animating!=value){animating=value;if(value)invalidate();}}
    protected void onDraw(Canvas c) {
        if(movie==null) return;
        int duration=movie.duration(); if(duration<=0) duration=1000;
        movie.setTime((int)((SystemClock.uptimeMillis()-start)%duration));
        float s=Math.min((float)getWidth()/movie.width(),(float)getHeight()/movie.height());
        c.save(); c.translate((getWidth()-movie.width()*s)/2,(getHeight()-movie.height()*s)/2); c.scale(s,s); movie.draw(c,0,0); c.restore();
        if(animating && isShown()) postInvalidateDelayed(33);
    }
}
