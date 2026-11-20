package com.klipatolyesi.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/** Metadata from the bundled decoder, independent of Android vendor codecs. */
public final class MediaInfo {
    public final int width, height;
    public final double duration;

    private MediaInfo(int width, int height, double duration) {
        this.width=width;this.height=height;this.duration=duration;
    }

    public static MediaInfo fromProbe(String json) throws IOException {
        JsonNode root=new ObjectMapper().readTree(json);
        if(root==null) throw new IOException("Medya bilgileri okunamadı.");
        double duration=positive(root.path("format").path("duration"));
        for(JsonNode stream:root.path("streams")) {
            if(!"video".equals(stream.path("codec_type").asText())) continue;
            int w=stream.path("width").asInt(),h=stream.path("height").asInt();
            if(w<2 || h<2 || stream.path("disposition").path("attached_pic").asInt()==1) continue;
            double seconds=positive(stream.path("duration"));
            if(seconds<=0) seconds=duration;
            if(seconds<=0) continue;
            int rotation=stream.path("tags").path("rotate").asInt(0);
            for(JsonNode side:stream.path("side_data_list"))
                if(side.has("rotation")) rotation=(int)Math.round(side.path("rotation").asDouble());
            rotation=((rotation%360)+360)%360;
            if(rotation==90 || rotation==270) {int swap=w;w=h;h=swap;}
            return new MediaInfo(w,h,seconds);
        }
        throw new IOException("Dosyada boyutu ve süresi okunabilen bir video bulunamadı. Başka bir dosya veya gönderi deneyin.");
    }

    private static double positive(JsonNode value) {
        double n=value.asDouble(0);
        return Double.isFinite(n) && n>0?n:0;
    }
}
