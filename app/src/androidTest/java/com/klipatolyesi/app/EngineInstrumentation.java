package com.klipatolyesi.app;

import android.app.*;
import android.content.*;
import android.graphics.Movie;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.lang.Process;
import java.util.*;

/** End-to-end device checks using the exact bundled native binaries. */
public class EngineInstrumentation extends Instrumentation {
    private StringBuilder report=new StringBuilder();
    private final ArrayList<Uri> outputs=new ArrayList<>();
    private void check(boolean yes,String message) throws Exception {if(!yes)throw new Exception(message);report.append("PASS: ").append(message).append('\n');}
    private String networkUrl;
    private boolean skipUpdate;
    public void onCreate(Bundle args) {super.onCreate(args);networkUrl=args.getString("networkUrl");skipUpdate=Boolean.parseBoolean(args.getString("skipUpdate","false"));start();}
    private void waitJob() throws Exception {
        long deadline=System.currentTimeMillis()+180000;
        while(MediaEngine.I.busy && System.currentTimeMillis()<deadline)Thread.sleep(100);
        check(!MediaEngine.I.busy,"İşlem zamanında tamamlandı");
        check(MediaEngine.I.error.isEmpty(),"Motor hatasız: "+MediaEngine.I.error);
    }
    public void onStart() {
        Bundle result=new Bundle();
        try {
            Context c=getTargetContext();MediaEngine e=MediaEngine.I;
            Activity activity=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Thread.sleep(1000);if(e.busy)waitJob();
            if(networkUrl!=null) {
                if(!skipUpdate) {runOnMainSync(()->e.update(c));waitJob();}
                runOnMainSync(()->e.download(c,networkUrl));waitJob();
                check(e.source!=null && e.duration>0,"Gerçek X gönderisi indirildi: "+networkUrl+" · "+e.duration+" sn");
                check(e.width>0 && e.height>0,"İndirilen kaynağın boyutu okundu: "+e.width+"×"+e.height);
                Process diagnostic=e.nativeProcess(c,"libffprobe.so",List.of("-v","quiet","-show_entries","stream=codec_name,width,height,duration:format=format_name,duration","-of","json",e.source.getAbsolutePath()));
                report.append("PROBE: ").append(new String(diagnostic.getInputStream().readAllBytes())).append('\n');diagnostic.waitFor();
                EditSpec downloaded=new EditSpec();downloaded.width=e.width;downloaded.height=e.height;downloaded.start=0;downloaded.end=Math.min(2,e.duration);downloaded.maxWidth=480;
                runOnMainSync(()->e.export(c,downloaded));waitJob();
                check(e.result!=null && "video/mp4".equals(e.resultMime),"X içeriği MP4 olarak galeriye kaydedildi");
                try(MediaMetadataRetriever r=new MediaMetadataRetriever()) {r.setDataSource(c,e.result);check(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!=null,"MP4 çıktısı Android tarafından okunabiliyor");}
                downloaded.gif=true;
                runOnMainSync(()->e.export(c,downloaded));waitJob();
                try(InputStream in=c.getContentResolver().openInputStream(e.result)) {Movie movie=Movie.decodeStream(in);check(movie!=null && movie.width()>0 && movie.duration()>0,"X içeriği hareketli GIF olarak galeriye kaydedildi");}
                result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);return;
            }
            File input=new File(c.getCacheDir(),"fixture.mp4");
            Process p=e.ffmpeg(c,List.of("-y","-hide_banner","-f","lavfi","-i","testsrc2=size=320x240:rate=24:duration=4","-f","lavfi","-i","sine=frequency=440:duration=4","-c:v","libx264","-pix_fmt","yuv420p","-c:a","aac","-shortest",input.getAbsolutePath()));
            String log=new String(p.getInputStream().readAllBytes());
            check(p.waitFor()==0,"FFmpeg test videosu ve ses üretti: "+(input.length()>0));
            check(MediaEngine.xUrl("Gönderi https://twitter.com/Interior/status/463440424141459456?s=20").equals("https://x.com/Interior/status/463440424141459456"),"X bağlantısı normalize edildi");
            try { MediaEngine.xUrl("https://x.com.evil.example/u/status/123");throw new Exception("Sahte alan adı kabul edildi"); } catch(IllegalArgumentException expected) {report.append("PASS: Sahte alan adı reddedildi\n");}
            runOnMainSync(()->e.importFile(c,Uri.fromFile(input)));waitJob();
            check(e.width==320 && e.height==240 && e.duration>3.9,"Video boyutu ve süresi okundu");
            waitForIdleSync();
            check(activity.findViewById(R.id.save_gif).isShown(),"GIF olarak kaydet seçeneği medya açılınca görünür");
            runOnMainSync(()->activity.findViewById(R.id.save_gif).performClick());waitJob();
            check("image/gif".equals(e.resultMime),"GIF düğmesi GIF biçimini doğrudan seçiyor");
            try(InputStream in=c.getContentResolver().openInputStream(e.result)) {Movie movie=Movie.decodeStream(in);check(movie!=null && movie.duration()>3000,"GIF düğmesi gerçek hareketli GIF kaydediyor");}
            runOnMainSync(()->activity.findViewById(R.id.save_video).performClick());waitJob();
            check("video/mp4".equals(e.resultMime),"Video düğmesi doğrudan MP4 kaydediyor");
            EditSpec s=new EditSpec();s.width=e.width;s.height=e.height;s.start=.5;s.end=2.5;s.left=.25;s.right=.75;s.rotation=90;s.speed=2;
            runOnMainSync(()->e.export(c,s));waitJob();Uri video=e.result;outputs.add(video);
            check("video/mp4".equals(e.resultMime),"MP4 galeriye kaydedildi");
            try(MediaMetadataRetriever r=new MediaMetadataRetriever()) {
                r.setDataSource(c,video);
                check("240".equals(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)) && "160".equals(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)),"Kırpma ve döndürme 240×160 çıktı verdi");
                long dur=Long.parseLong(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                check(dur>=900 && dur<=1300,"Kesme ve 2× hız yaklaşık 1 saniye çıktı verdi: "+dur);
                check("yes".equals(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)),"MP4 sesi korundu");
            }
            s.gif=true;s.start=0;s.end=2;s.speed=1;s.rotation=0;s.left=.125;s.right=.875;
            runOnMainSync(()->e.export(c,s));waitJob();Uri gif=e.result;outputs.add(gif);
            try(InputStream in=c.getContentResolver().openInputStream(gif)) {
                Movie movie=Movie.decodeStream(in);
                check(movie!=null && movie.width()==240 && movie.height()==240,"Video → GIF paletli, 240×240 üretildi");
                check(movie.duration()>1500,"GIF birden fazla kareyle hareketli üretildi");
            }
            runOnMainSync(()->e.importFile(c,gif));waitJob();
            check(e.sourceGif && e.width==240 && e.height==240,"GIF kaynak olarak açıldı");
            EditSpec back=new EditSpec();back.width=e.width;back.height=e.height;back.start=0;back.end=Math.min(1.5,e.duration);back.filter=2;back.mirror=true;back.mute=true;
            runOnMainSync(()->e.export(c,back));waitJob();Uri converted=e.result;outputs.add(converted);
            try(MediaMetadataRetriever r=new MediaMetadataRetriever()) {r.setDataSource(c,converted);check("240".equals(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)),"GIF → MP4 dönüştürüldü");check(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)==null,"Sessiz çıktı ses içermiyor");}
            runOnMainSync(()->e.importFile(c,Uri.fromFile(input)));waitJob();
            Thread.sleep(700);
            android.graphics.Bitmap screenshot=getUiAutomation().takeScreenshot();
            if(screenshot!=null)try(OutputStream screenshotFile=new FileOutputStream(new File(c.getExternalFilesDir(null),"test-preview.png"))) {screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,screenshotFile);screenshot.recycle();}
            Process version=e.ffmpeg(c,List.of("-version"));String versionText=new String(version.getInputStream().readAllBytes());version.waitFor();
            try(OutputStream versionFile=new FileOutputStream(new File(c.getExternalFilesDir(null),"ffmpeg-version.txt"))) {versionFile.write(versionText.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            report.append("ALL DEVICE TESTS PASSED\n");
            result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
        } catch(Throwable t) {result.putString("stream",report+"\nFAIL: "+android.util.Log.getStackTraceString(t));finish(Activity.RESULT_CANCELED,result);}
    }
}
