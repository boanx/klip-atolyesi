package com.klipatolyesi.app;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.os.SystemClock;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

/** Optional same-device, same-input comparison against the shipped 1.1.0 algorithm. */
final class StickerSpeedCheck {
    static void run(Instrumentation instrumentation,Context c,Consumer<String> emit) throws Exception {
        MediaEngine e=MediaEngine.I;
        File dir=new File(c.getCacheDir(),"speed-"+System.nanoTime());dir.mkdirs();
        try {
            File video=new File(dir,"source.mp4");
            run(e,c,Arrays.asList("-y","-hide_banner","-f","lavfi","-i","testsrc2=size=320x240:rate=24:duration=4","-f","lavfi","-i","sine=frequency=440:duration=4","-c:v","libx264","-pix_fmt","yuv420p","-c:a","aac","-shortest",video.getAbsolutePath()));
            File gif=new File(dir,"source.gif");EditSpec makeGif=new EditSpec();makeGif.width=320;makeGif.height=240;makeGif.end=2;makeGif.left=.125;makeGif.right=.875;makeGif.gif=true;
            run(e,c,makeGif.args(video.getAbsolutePath(),gif.getAbsolutePath()));
            EditSpec shortEdit=new EditSpec();shortEdit.width=shortEdit.height=240;shortEdit.end=2;
            compare(e,c,gif,shortEdit,dir,"2s GIF",emit);
            EditSpec longEdit=new EditSpec();longEdit.width=320;longEdit.height=240;longEdit.end=4;longEdit.speed=.4;
            compare(e,c,video,longEdit,dir,"10s output",emit);
            emit.accept("ALL SPEED AND FORMAT CHECKS PASSED");
        }finally{MediaEngine.deleteTree(dir);}
    }
    private static void compare(MediaEngine e,Context c,File input,EditSpec spec,File dir,String label,Consumer<String> emit) throws Exception {
        File old=new File(dir,"old.webp"),fast=new File(dir,"fast.webp"),cache=new File(dir,"cache.webp");
        emit.accept("START: "+label+" old 1.1.0");
        long oldStart=SystemClock.elapsedRealtime();int oldAttempts=0;
        for(int[] profile:new int[][]{{80,10},{60,10},{40,10},{25,10},{20,8},{15,5}}) {
            oldAttempts++;run(e,c,oldArgs(spec,input.getAbsolutePath(),old.getAbsolutePath(),profile[0],profile[1]));
            if(old.length()<=StickerSpec.MAX_BYTES){WebpInfo.read(old).validateSticker();break;}
        }
        long oldMs=SystemClock.elapsedRealtime()-oldStart;WebpInfo.read(old).validateSticker();
        emit.accept("OLD: "+label+" "+oldMs+" ms, "+oldAttempts+" attempts, "+old.length()+" bytes");
        MediaEngine.StickerEncoding encoded=e.encodeSticker(c,input,spec,fast);WebpInfo.read(fast).validateSticker();
        if(!(ImageDecoder.decodeDrawable(ImageDecoder.createSource(fast)) instanceof AnimatedImageDrawable))throw new IOException("Fast output is not animated WebP");
        if(encoded.cached)throw new IOException("First encode unexpectedly used cache");
        emit.accept("NEW: "+label+" "+encoded.elapsedMs+" ms, "+encoded.attempts+" attempts, "+fast.length()+" bytes, speedup="+String.format(Locale.US,"%.2fx",(double)oldMs/Math.max(1,encoded.elapsedMs)));
        if(encoded.elapsedMs>=oldMs)throw new IOException("Measured fast path was not faster for "+label);
        MediaEngine.StickerEncoding cached=e.encodeSticker(c,input,spec,cache);
        if(!cached.cached || cached.attempts!=0 || !Arrays.equals(read(fast),read(cache)))throw new IOException("Identical sticker was not reused safely");
        emit.accept("CACHE: "+label+" "+cached.elapsedMs+" ms, byte-identical");
        // Changing the selected time must invalidate the reusable result.
        double originalEnd=spec.end;spec.end-=.2;
        MediaEngine.StickerEncoding changed=e.encodeSticker(c,input,spec,cache);
        if(changed.cached)throw new IOException("Changed edit reused the wrong sticker");
        WebpInfo.read(cache).validateSticker();spec.end=originalEnd;
        emit.accept("PASS: "+label+" edited duration invalidates cache; dimensions/size/animation remain valid");
    }
    private static byte[] read(File file)throws IOException{try(InputStream in=new FileInputStream(file)){return in.readAllBytes();}}
    private static void run(MediaEngine e,Context c,List<String> args)throws Exception {
        Process process=e.ffmpeg(c,args);String log=new String(process.getInputStream().readAllBytes());if(process.waitFor()!=0)throw new IOException(log);
    }
    private static List<String> oldArgs(EditSpec e,String input,String out,int quality,int fps) {
        String filter=e.videoFilter()+",fps="+fps+",scale=480:480:force_original_aspect_ratio=decrease:force_divisible_by=2:flags=lanczos,format=rgba,pad=512:512:(ow-iw)/2:(oh-ih)/2:color=black@0,format=yuva420p";
        return Arrays.asList("-y","-hide_banner","-nostdin","-filter_threads","1","-ss",EditSpec.n(e.start),"-t",EditSpec.n(e.end-e.start),"-i",input,"-map","0:v:0","-an","-vf",filter,"-fps_mode","passthrough","-c:v","libwebp_anim","-lossless","0","-quality",String.valueOf(quality),"-compression_level","6","-loop","0","-threads","2","-progress","pipe:1","-nostats",out);
    }
}
