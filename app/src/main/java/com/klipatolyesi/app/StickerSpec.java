package com.klipatolyesi.app;

import java.util.*;

/** WhatsApp animated WebP export; uses the same selected crop/time/effects. */
public final class StickerSpec {
    public static final int MAX_BYTES=500*1024;
    public static void validate(EditSpec edit,double duration) {
        edit.validate(duration);
        double seconds=(edit.end-edit.start)/edit.speed;
        if(seconds>10.00001) throw new IllegalArgumentException("WhatsApp çıkartması için en fazla 10 saniyelik bölüm seçin veya hızı artırın.");
        if(seconds<.2) throw new IllegalArgumentException("Hareketli çıkartma için en az 0,2 saniyelik bölüm seçin.");
    }
    public static List<String> args(EditSpec e,String in,String out,int quality,int fps) {
        String filter=e.videoFilter()+",fps="+fps+",scale=480:480:force_original_aspect_ratio=decrease:force_divisible_by=2:flags=lanczos,format=rgba,pad=512:512:(ow-iw)/2:(oh-ih)/2:color=black@0,format=yuva420p";
        return new ArrayList<>(Arrays.asList("-y","-hide_banner","-nostdin","-filter_threads","1","-ss",EditSpec.n(e.start),"-t",EditSpec.n(e.end-e.start),"-i",in,"-map","0:v:0","-an","-vf",filter,"-fps_mode","passthrough","-c:v","libwebp_anim","-lossless","0","-quality",String.valueOf(quality),"-compression_level","6","-loop","0","-threads","2","-progress","pipe:1","-nostats",out));
    }
}
