package com.klipatolyesi.app;

import android.app.*;
import android.content.Intent;
import android.os.*;

public class WorkService extends Service {
    private PowerManager.WakeLock lock;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable() {
        public void run() { if(MediaEngine.I.busy) { getSystemService(NotificationManager.class).notify(7,notification()); handler.postDelayed(this,1500); } else stopSelf(); }
    };
    public void onCreate() {
        super.onCreate();
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("media","Medya işlemleri",NotificationManager.IMPORTANCE_LOW));
        lock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"KlipAtolyesi:medya");
        lock.acquire(60*60*1000L);
    }
    Notification notification() {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent cancel=PendingIntent.getService(this,1,new Intent(this,WorkService.class).setAction("cancel"),PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n=new Notification.Builder(this,"media").setSmallIcon(com.klipatolyesi.app.R.drawable.ic_app).setContentTitle("Klip Atölyesi").setContentText(MediaEngine.I.status).setContentIntent(open).setOngoing(true).addAction(android.R.drawable.ic_menu_close_clear_cancel,"İptal",cancel);
        int p=MediaEngine.I.progress;
        n.setProgress(100,Math.max(0,p),p<0); return n.build();
    }
    public int onStartCommand(Intent i,int flags,int id) {
        startForeground(7,notification());
        if(i!=null && "cancel".equals(i.getAction())) MediaEngine.I.cancel();
        MediaEngine.I.startPending();
        handler.removeCallbacks(refresh); handler.post(refresh);
        return START_NOT_STICKY;
    }
    public void onTimeout(int startId,int fgsType) { MediaEngine.I.cancel(); stopSelf(); }
    public void onDestroy() { handler.removeCallbacks(refresh); if(lock!=null && lock.isHeld()) lock.release(); super.onDestroy(); }
    public IBinder onBind(Intent i) { return null; }
}
