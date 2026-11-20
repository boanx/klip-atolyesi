package com.klipatolyesi.app;

import android.content.*;
import android.graphics.Movie;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import java.io.*;
import java.lang.Process;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public final class MediaEngine {
    public interface Listener { void changed(); }
    public static final MediaEngine I = new MediaEngine();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    public volatile boolean busy, cancelled;
    public volatile String status = "Bir X bağlantısı ekleyin veya telefonunuzdan dosya açın.", error = "";
    public volatile int progress = -1;
    public volatile File source;
    public volatile String sourceName = "";
    public volatile double duration;
    public volatile int width, height;
    public volatile boolean sourceGif;
    public volatile Uri result;
    public volatile String resultMime;
    public volatile Listener listener;
    private volatile Process process;
    private Runnable pending;
    private boolean initialized;

    void notifyUi() { main.post(() -> { if (listener != null) listener.changed(); }); }
    void state(String text, int percent) { status = text; progress = percent; notifyUi(); }

    public synchronized void submit(Context context, String title, Work work) {
        if (busy) return;
        Context c = context.getApplicationContext();
        busy = true; cancelled = false; error = ""; state(title, -1);
        pending = () -> {
            try { work.run(c); }
            catch (Exception e) {
                android.util.Log.e("KlipAtolyesi", "Medya işlemi tamamlanamadı", e);
                if (cancelled) state("İşlem iptal edildi.", -1);
                else { error = friendly(e); state("İşlem tamamlanamadı.", -1); }
            } finally {
                process = null; busy = false;
                c.stopService(new Intent(c, WorkService.class));
                notifyUi();
            }
        };
        try { c.startForegroundService(new Intent(c, WorkService.class)); }
        catch (Exception e) { pending=null; busy = false; error = "İşlem başlatılamadı: "+e.getMessage(); notifyUi(); }
    }
    /** Work begins only after Android has registered the foreground notification. */
    synchronized void startPending() { if(pending!=null) { Runnable job=pending;pending=null;worker.submit(job); } }
    public interface Work { void run(Context c) throws Exception; }
    void checkCancel() throws IOException { if (cancelled) throw new IOException("İptal edildi"); }
    public void cancel() {
        cancelled = true;
        Process p = process; if (p != null) p.destroy();
        new Thread(() -> { try { YoutubeDL.getInstance().destroyProcessById("x-download"); } catch (Exception ignored) {} }).start();
        state("İşlem iptal ediliyor…", -1);
    }
    private void init(Context c) throws Exception {
        if (initialized) return;
        state("Medya motoru hazırlanıyor…", -1);
        FFmpeg.getInstance().init(c);
        YoutubeDL.getInstance().init(c);
        initialized = true;
    }
    public static String xUrl(String text) {
        Matcher m = Pattern.compile("https?://(?:www\\.|mobile\\.)?(?:x\\.com|twitter\\.com)/[^\\s<>]+", Pattern.CASE_INSENSITIVE).matcher(text);
        if (!m.find()) throw new IllegalArgumentException("Geçerli bir x.com veya twitter.com gönderi bağlantısı girin.");
        Uri uri = Uri.parse(m.group());
        String path = uri.getPath();
        if (path == null || !path.matches("/(?:[^/]+/status|i/status)/[0-9]+(?:/.*)?"))
            throw new IllegalArgumentException("Profil yerine video içeren gönderinin bağlantısını kullanın.");
        return "https://x.com"+path;
    }
    public void download(Context c, String text) {
        final String url;
        try { url = xUrl(text); } catch (Exception e) { error=e.getMessage(); notifyUi(); return; }
        submit(c, "X bağlantısı açılıyor…", ctx -> {
            init(ctx); checkCancel();
            File dir = new File(ctx.getCacheDir(), "download-"+System.nanoTime()); dir.mkdirs();
            try {
                YoutubeDLRequest r = new YoutubeDLRequest(url);
                r.addOption("--no-playlist"); r.addOption("--playlist-items", "1");
                r.addOption("--socket-timeout", "25"); r.addOption("--retries", "2");
                r.addOption("--max-filesize", "1G");
                r.addOption("-f", "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/best");
                r.addOption("--merge-output-format", "mp4");
                r.addOption("-o", new File(dir, "media.%(ext)s").getAbsolutePath());
                YoutubeDL.getInstance().execute(r, "x-download", (p, eta, line) -> {
                    state("X’ten indiriliyor…", Math.max(0, Math.min(99, (int)(float)p))); return kotlin.Unit.INSTANCE;
                });
                checkCancel();
                File[] files = dir.listFiles((d,n) -> !n.endsWith(".part") && !n.endsWith(".ytdl") && !n.endsWith(".json"));
                if (files == null || files.length == 0) throw new IOException("Gönderide indirilebilir medya bulunamadı.");
                File file = new File(ctx.getFilesDir(), "source-"+System.nanoTime()+"."+extension(files[0].getName()));
                try { copy(new FileInputStream(files[0]), new FileOutputStream(file)); checkCancel(); load(ctx,file,"X gönderisi"); }
                catch (Exception e) { file.delete(); throw e; }
            } finally { deleteTree(dir); }
        });
    }
    public void importFile(Context c, Uri uri) {
        submit(c, "Dosya hazırlanıyor…", ctx -> {
            File file = new File(ctx.getFilesDir(), "source-"+System.nanoTime()+".media");
            try {
                InputStream input = ctx.getContentResolver().openInputStream(uri);
                if (input == null) throw new IOException("Dosya açılamadı.");
                copy(input, new FileOutputStream(file)); checkCancel();
                load(ctx,file,"Telefondan açılan dosya");
            } catch (Exception e) { file.delete(); throw e; }
        });
    }
    void copy(InputStream in, OutputStream out) throws IOException {
        try (in; out) {
            byte[] buf = new byte[128*1024]; int count; long total=0;
            while ((count=in.read(buf)) != -1) { checkCancel(); total+=count;
                if (total > 1024L*1024*1024) throw new IOException("Dosya 1 GB sınırını aşıyor.");
                out.write(buf,0,count);
            }
        }
    }
    private void load(Context c, File f, String name) throws Exception {
        byte[] header = new byte[6];
        try (InputStream in = new FileInputStream(f)) { in.read(header); }
        boolean gif = new String(header, java.nio.charset.StandardCharsets.US_ASCII).startsWith("GIF8");
        int w=0,h=0; double seconds=0;
        if (gif) {
            Movie movie = Movie.decodeFile(f.getAbsolutePath());
            if (movie != null) { w=movie.width(); h=movie.height(); seconds=movie.duration()/1000.0; }
        } else {
            try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
                retriever.setDataSource(f.getAbsolutePath());
                w=(int)metadataNumber(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                h=(int)metadataNumber(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                seconds=metadataNumber(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))/1000.0;
                String rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                if ("90".equals(rot) || "270".equals(rot)) { int swap=w; w=h; h=swap; }
            } catch (Exception ignored) {
                // Android may not expose metadata for codecs/containers FFmpeg supports.
                w=h=0;seconds=0;
            }
        }
        if (w<2 || h<2 || seconds<=0) {
            state("Video bilgileri alternatif yöntemle okunuyor…",-1);
            MediaInfo fallback=probe(c,f);
            w=fallback.width;h=fallback.height;seconds=fallback.duration;
        }
        File old = source;
        width=w; height=h; duration=seconds; sourceGif=gif; sourceName=name; source=f;
        c.getSharedPreferences("media",0).edit().putString("source",f.getAbsolutePath()).putString("name",name).apply();
        if (old != null && !old.equals(f)) old.delete();
        state("Düzenlemeye hazır. Çıkış biçimini seçip kaydedin.",100);
    }
    public void restore(Context c) {
        if (source != null || busy) return;
        String path=c.getSharedPreferences("media",0).getString("source",null);
        if (path!=null && new File(path).isFile()) submit(c,"Son dosya açılıyor…",ctx -> load(ctx,new File(path),ctx.getSharedPreferences("media",0).getString("name","Son dosya")));
    }
    public void update(Context c) {
        submit(c,"İndirme motoru güncelleniyor…",ctx -> {
            init(ctx); checkCancel();
            YoutubeDL.getInstance().updateYoutubeDL(ctx, YoutubeDL.UpdateChannel._STABLE);
            checkCancel(); state("İndirme motoru güncel.",100);
        });
    }
    Process ffmpeg(Context c, List<String> args) throws Exception {
        return nativeProcess(c,"libffmpeg.so",args);
    }
    Process nativeProcess(Context c, String binary, List<String> args) throws Exception {
        init(c); checkCancel();
        List<String> command=new ArrayList<>(); command.add(c.getApplicationInfo().nativeLibraryDir+"/"+binary); command.addAll(args);
        ProcessBuilder b=new ProcessBuilder(command).redirectErrorStream(true);
        String packages=new File(c.getNoBackupFilesDir(),"youtubedl-android/packages").getAbsolutePath();
        b.environment().put("LD_LIBRARY_PATH",packages+"/ffmpeg/usr/lib:"+packages+"/python/usr/lib");
        b.environment().put("TMPDIR",c.getCacheDir().getAbsolutePath());
        process=b.start(); return process;
    }
    MediaInfo probe(Context c, File f) throws Exception {
        Process p=nativeProcess(c,"libffprobe.so",Arrays.asList("-v","quiet","-show_entries","stream=codec_type,width,height,duration:stream_disposition=attached_pic:stream_tags=rotate:stream_side_data=rotation:format=duration","-of","json",f.getAbsolutePath()));
        StringBuilder json=new StringBuilder();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream(),java.nio.charset.StandardCharsets.UTF_8))) {
            char[] buffer=new char[4096];int n;
            while((n=reader.read(buffer))!=-1) { checkCancel();json.append(buffer,0,n);if(json.length()>1024*1024) {p.destroy();throw new IOException("Medya bilgileri beklenenden büyük.");} }
        }
        int code=p.waitFor();checkCancel();
        if(code!=0) throw new IOException("Dosyanın video bilgileri okunamadı. İçerik eksik veya desteklenmeyen bir biçimde olabilir.");
        return MediaInfo.fromProbe(json.toString());
    }
    private static double metadataNumber(String value) {
        if(value==null) return 0;
        try { double n=Double.parseDouble(value);return Double.isFinite(n)&&n>0?n:0; }
        catch(NumberFormatException ignored) {return 0;}
    }
    public void export(Context c, EditSpec spec) {
        if (source == null) return;
        try { spec.validate(duration); } catch (Exception e) { error=e.getMessage(); notifyUi(); return; }
        File input=source;
        submit(c,"Çıktı hazırlanıyor…",ctx -> {
            File out=new File(ctx.getCacheDir(),"export-"+System.nanoTime()+(spec.gif?".gif":".mp4"));
            Uri saved=null;
            try {
                Process p=ffmpeg(ctx,spec.args(input.getAbsolutePath(),out.getAbsolutePath()));
                StringBuilder tail=new StringBuilder();
                try (BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while((line=reader.readLine())!=null) {
                        if(line.startsWith("out_time_us=")) {
                            try { double t=Double.parseDouble(line.substring(12))/1e6;
                                state((spec.gif?"GIF":"Video")+" oluşturuluyor…",Math.max(0,Math.min(99,(int)(100*t/((spec.end-spec.start)/spec.speed)))));
                            } catch (NumberFormatException ignored) {}
                        }
                        tail.append(line).append('\n'); if(tail.length()>8000) tail.delete(0,4000);
                    }
                }
                int code=p.waitFor(); checkCancel();
                if(code!=0 || out.length()==0) throw new IOException("Dönüştürme başarısız.\n"+tail);
                state("Galeriye kaydediliyor…",99);
                ContentResolver resolver=ctx.getContentResolver();
                String mime=spec.gif?"image/gif":"video/mp4";
                ContentValues v=new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME,"Klip_"+new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(new Date())+(spec.gif?".gif":".mp4"));
                v.put(MediaStore.MediaColumns.MIME_TYPE,mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH,(spec.gif?Environment.DIRECTORY_PICTURES:Environment.DIRECTORY_MOVIES)+"/KlipAtolyesi");
                v.put(MediaStore.MediaColumns.IS_PENDING,1);
                saved=resolver.insert(spec.gif?MediaStore.Images.Media.EXTERNAL_CONTENT_URI:MediaStore.Video.Media.EXTERNAL_CONTENT_URI,v);
                if(saved==null) throw new IOException("Galeri kaydı oluşturulamadı.");
                OutputStream stream=resolver.openOutputStream(saved);
                if(stream==null) throw new IOException("Galeriye yazılamadı.");
                copy(new FileInputStream(out),stream); checkCancel();
                v.clear(); v.put(MediaStore.MediaColumns.IS_PENDING,0); resolver.update(saved,v,null,null);
                result=saved; resultMime=mime; saved=null;
                state((spec.gif?"GIF":"Video")+" galeriye kaydedildi. Açabilir veya paylaşabilirsiniz.",100);
            } finally { out.delete(); if(saved!=null) ctx.getContentResolver().delete(saved,null,null); }
        });
    }
    private String friendly(Exception e) {
        String msg=e.getMessage()==null?e.toString():e.getMessage();
        if (msg.contains("403") || msg.contains("401") || msg.contains("login") || msg.contains("private"))
            return "X bu gönderiye erişim vermedi. Gizli, silinmiş veya giriş gerektiren içerikler indirilemeyebilir.\n\n"+msg.substring(0,Math.min(msg.length(),1000));
        return msg.substring(0,Math.min(msg.length(),2200));
    }
    static String extension(String name) { int i=name.lastIndexOf('.'); return i<0?"mp4":name.substring(i+1); }
    static void deleteTree(File file) { File[] children=file.listFiles(); if(children!=null) for(File child:children) deleteTree(child); file.delete(); }
}
