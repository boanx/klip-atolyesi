package com.klipatolyesi.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;

public class StickerSpecTest {
    private EditSpec edit(){EditSpec e=new EditSpec();e.width=640;e.height=360;e.end=4;return e;}
    @Test public void stickerHasTransparent512CanvasWebpAndLoop(){List<String>a=StickerSpec.args(edit(),"in.gif","out.webp",60,10);assertTrue(a.contains("libwebp_anim"));assertTrue(a.contains("-an"));assertTrue(a.stream().anyMatch(s->s.contains("pad=512:512") && s.contains("black@0")));assertTrue(a.contains("-loop"));}
    @Test(expected=IllegalArgumentException.class)public void rejectsLongAnimation(){EditSpec e=edit();e.end=11;StickerSpec.validate(e,20);}
    @Test public void usesPlaybackDuration(){EditSpec e=edit();e.end=20;e.speed=2;StickerSpec.validate(e,20);}
    @Test(expected=IllegalArgumentException.class)public void rejectsSingleShortFrame(){EditSpec e=edit();e.end=.05;StickerSpec.validate(e,20);}
    @Test(expected=IOException.class)public void rejectsTruncatedWebp()throws Exception{WebpInfo.read(new byte[12]);}
    @Test public void acceptsBoundariesRejectsDurationAndSize()throws Exception{WebpInfo w=new WebpInfo();w.width=w.height=512;w.frames=2;w.durationMs=10000;w.minFrameMs=100;w.animation=true;w.loopCount=0;w.validateSticker();w.durationMs=10001;try{w.validateSticker();fail("Too long");}catch(IOException expected){}w.durationMs=9000;w.width=511;try{w.validateSticker();fail("Wrong dimensions");}catch(IOException expected){}}
}
