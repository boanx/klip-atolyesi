package com.klipatolyesi.app;

import android.content.Context;
import android.graphics.*;
import android.view.*;

public class CropOverlay extends View {
    public float left=0, top=0, right=1, bottom=1;
    private int mediaW=1,mediaH=1, mode;
    private float lastX,lastY;
    private final Paint p=new Paint(3);
    private final RectF media=new RectF(), crop=new RectF();
    public CropOverlay(Context c) { super(c); setContentDescription("Kırpma alanı: köşeleri sürükleyerek boyutlandırın, ortadan sürükleyerek taşıyın."); }
    public void media(int w,int h) { mediaW=w; mediaH=h; reset(); }
    public void reset() { left=top=0; right=bottom=1; invalidate(); }
    public void ratio(double ratio) {
        reset();
        if(ratio>0) {
            double current=(double)mediaW/mediaH;
            if(current>ratio) { float w=(float)(ratio/current); left=(1-w)/2; right=1-left; }
            else { float h=(float)(current/ratio); top=(1-h)/2; bottom=1-top; }
        }
        invalidate();
    }
    void bounds() {
        float s=Math.min((float)getWidth()/mediaW,(float)getHeight()/mediaH);
        float w=s*mediaW,h=s*mediaH;
        media.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);
        crop.set(media.left+left*w,media.top+top*h,media.left+right*w,media.top+bottom*h);
    }
    protected void onDraw(Canvas c) {
        bounds(); p.setStyle(Paint.Style.FILL); p.setColor(0x99000000);
        c.drawRect(media.left,media.top,media.right,crop.top,p); c.drawRect(media.left,crop.bottom,media.right,media.bottom,p);
        c.drawRect(media.left,crop.top,crop.left,crop.bottom,p); c.drawRect(crop.right,crop.top,media.right,crop.bottom,p);
        p.setStyle(Paint.Style.STROKE); p.setColor(0xffa8f078); p.setStrokeWidth(2*getResources().getDisplayMetrics().density);
        c.drawRect(crop,p);
        p.setColor(0x70ffffff); p.setStrokeWidth(1);
        for(int i=1;i<3;i++) { c.drawLine(crop.left+crop.width()*i/3,crop.top,crop.left+crop.width()*i/3,crop.bottom,p); c.drawLine(crop.left,crop.top+crop.height()*i/3,crop.right,crop.top+crop.height()*i/3,p); }
        p.setStyle(Paint.Style.FILL); p.setColor(0xffa8f078); float r=5*getResources().getDisplayMetrics().density;
        c.drawCircle(crop.left,crop.top,r,p); c.drawCircle(crop.right,crop.top,r,p); c.drawCircle(crop.left,crop.bottom,r,p); c.drawCircle(crop.right,crop.bottom,r,p);
    }
    public boolean onTouchEvent(android.view.MotionEvent e) {
        if(!isEnabled()) return false;
        bounds(); float x=e.getX(), y=e.getY(), hit=28*getResources().getDisplayMetrics().density;
        if(e.getAction()==MotionEvent.ACTION_DOWN) {
            mode=0;
            if(Math.abs(x-crop.left)<hit) mode|=1;
            else if(Math.abs(x-crop.right)<hit) mode|=2;
            if(Math.abs(y-crop.top)<hit) mode|=4;
            else if(Math.abs(y-crop.bottom)<hit) mode|=8;
            if(mode==0 && crop.contains(x,y)) mode=16;
            if(mode==0) return false;
            getParent().requestDisallowInterceptTouchEvent(true); lastX=x;lastY=y; return true;
        }
        if(e.getAction()==MotionEvent.ACTION_MOVE) {
            float dx=(x-lastX)/media.width(), dy=(y-lastY)/media.height();
            if(mode==16) { dx=Math.max(-left,Math.min(1-right,dx)); dy=Math.max(-top,Math.min(1-bottom,dy)); left+=dx;right+=dx;top+=dy;bottom+=dy; }
            else {
                if((mode&1)!=0) left=Math.max(0,Math.min(right-.05f,left+dx));
                if((mode&2)!=0) right=Math.min(1,Math.max(left+.05f,right+dx));
                if((mode&4)!=0) top=Math.max(0,Math.min(bottom-.05f,top+dy));
                if((mode&8)!=0) bottom=Math.min(1,Math.max(top+.05f,bottom+dy));
            }
            lastX=x;lastY=y;invalidate();return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL) { getParent().requestDisallowInterceptTouchEvent(false); performClick(); return true; }
        return true;
    }
    public boolean performClick() { super.performClick(); return true; }
}
