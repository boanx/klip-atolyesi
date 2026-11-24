package com.klipatolyesi.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;

/** Pure transformation model; all shell arguments are passed separately. */
public final class EditSpec {
    public double start, end, speed = 1;
    public double left, top, right = 1, bottom = 1;
    public int width, height, rotation, maxWidth = 720, fps = 15, filter;
    public boolean gif, mute, mirror;

    static String n(double value) { return String.format(Locale.US, "%.6f", value); }

    public void validate(double duration) {
        if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end <= start || end > duration + .1)
            throw new IllegalArgumentException("Başlangıç ve bitişi kontrol edin. Bitiş, başlangıçtan büyük olmalı.");
        if (speed < .25 || speed > 4 || !Double.isFinite(speed)) throw new IllegalArgumentException("Geçersiz hız.");
        if (width < 2 || height < 2 || left < 0 || top < 0 || right > 1 || bottom > 1 || right-left < .03 || bottom-top < .03)
            throw new IllegalArgumentException("Kırpma alanı çok küçük.");
        if (gif && (end-start)/speed > 60) throw new IllegalArgumentException("GIF için en fazla 60 saniyelik bir bölüm seçin veya hızı artırın.");
    }

    public String spatialFilter() {
        List<String> f = new ArrayList<>();
        int x = (int)(width*left)/2*2, y = (int)(height*top)/2*2;
        int w = Math.max(2, (int)(width*(right-left))/2*2);
        int h = Math.max(2, (int)(height*(bottom-top))/2*2);
        f.add("crop="+w+":"+h+":"+x+":"+y);
        if (rotation == 90) f.add("transpose=1");
        if (rotation == 180) { f.add("hflip"); f.add("vflip"); }
        if (rotation == 270) f.add("transpose=2");
        if (mirror) f.add("hflip");
        if (filter == 1) f.add("eq=contrast=1.08:saturation=1.25");
        if (filter == 2) f.add("hue=s=0");
        if (filter == 3) f.add("colorbalance=rs=.08:bs=-.06");
        return String.join(",", f);
    }

    public String videoFilter() {
        List<String> f=new ArrayList<>();
        f.add(spatialFilter());
        f.add("setpts=(PTS-STARTPTS)/"+n(speed));
        f.add("scale=w='min("+maxWidth+",iw)':h=-2:flags=lanczos");
        f.add("setsar=1");
        return String.join(",", f);
    }

    public String audioFilter() {
        double s = speed;
        List<String> f = new ArrayList<>();
        f.add("asetpts=PTS-STARTPTS");
        while (s > 2.0) { f.add("atempo=2.0"); s /= 2; }
        while (s < .5) { f.add("atempo=0.5"); s *= 2; }
        f.add("atempo="+n(s));
        return String.join(",", f);
    }

    public List<String> args(String input, String output) {
        List<String> a = new ArrayList<>(Arrays.asList("-y", "-hide_banner", "-nostdin", "-filter_complex_threads", "1", "-ss", n(start), "-t", n(end-start), "-i", input));
        if (gif) {
            a.addAll(Arrays.asList("-filter_complex", videoFilter()+",fps="+fps+",split[a][b];[a]palettegen=max_colors=192:stats_mode=diff[p];[b][p]paletteuse=dither=sierra2_4a", "-an", "-loop", "0"));
        } else {
            a.addAll(Arrays.asList("-map", "0:v:0", "-vf", videoFilter(), "-c:v", "libx264", "-preset", "ultrafast", "-crf", "21", "-pix_fmt", "yuv420p"));
            if (mute) a.add("-an");
            else a.addAll(Arrays.asList("-map", "0:a:0?", "-af", audioFilter(), "-c:a", "aac", "-b:a", "128k"));
            a.addAll(Arrays.asList("-movflags", "+faststart"));
        }
        a.addAll(Arrays.asList("-threads", "2", "-progress", "pipe:1", "-nostats", output));
        return a;
    }
}
