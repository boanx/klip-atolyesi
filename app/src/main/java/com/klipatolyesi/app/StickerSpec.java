package com.klipatolyesi.app;

import java.util.*;

/** WhatsApp animated WebP export; uses the same selected crop/time/effects. */
public final class StickerSpec {
    public static final int MAX_BYTES=500*1024;
    public static final int MAX_ATTEMPTS=3;
    public static int initialQuality(double seconds) {return seconds>6?40:seconds>3?50:65;}
    public static int initialFps(double seconds) {return seconds>6?8:10;}
    public static int nextQuality(int quality,long bytes) {
        double ratio=(MAX_BYTES*.85)/Math.max(1,bytes);
        return Math.max(8,Math.min(quality-10,(int)(quality*ratio)));
    }
    public static void validate(EditSpec edit,double duration) {
        edit.validate(duration);
        double seconds=(edit.end-edit.start)/edit.speed;
        if(seconds>10.00001) throw new IllegalArgumentException("WhatsApp çıkartması için en fazla 10 saniyelik bölüm seçin veya hızı artırın.");
        if(seconds<.2) throw new IllegalArgumentException("Hareketli çıkartma için en az 0,2 saniyelik bölüm seçin.");
    }
    public static List<String> args(EditSpec e,String in,String out,int quality,int fps) {
        // Drop unused frames before crop/effects/resize and scale only once.
        String filter="setpts=(PTS-STARTPTS)/"+EditSpec.n(e.speed)+",fps="+fps+","+e.spatialFilter()+",scale=480:480:force_original_aspect_ratio=decrease:force_divisible_by=2:flags=bilinear,format=yuva420p,pad=512:512:(ow-iw)/2:(oh-ih)/2:color=black@0,setsar=1";
        return new ArrayList<>(Arrays.asList("-y","-hide_banner","-nostdin","-filter_threads","1","-ss",EditSpec.n(e.start),"-t",EditSpec.n(e.end-e.start),"-i",in,"-map","0:v:0","-an","-vf",filter,"-fps_mode","passthrough","-c:v","libwebp_anim","-lossless","0","-quality",String.valueOf(quality),"-compression_level","1","-loop","0","-threads","2","-progress","pipe:1","-nostats",out));
    }
}
