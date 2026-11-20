package com.klipatolyesi.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class EditSpecTest {
    EditSpec spec() { EditSpec s=new EditSpec();s.width=1920;s.height=1080;s.start=1;s.end=5;return s; }
    @Test public void transformsCropBeforeRotationAndKeepsEvenDimensions() {
        EditSpec s=spec();s.left=.25;s.right=.75;s.rotation=90;s.speed=2;
        assertTrue(s.videoFilter().startsWith("crop=960:1080:480:0,transpose=1,setpts="));
        assertTrue(s.videoFilter().contains("min(720,iw)"));
    }
    @Test public void videoPreservesOptionalAudioAndUsesH264() {
        List<String> a=spec().args("/input with spaces.mp4","/out.mp4");
        assertTrue(a.contains("0:a:0?"));assertTrue(a.contains("libx264"));assertEquals("/out.mp4",a.get(a.size()-1));
    }
    @Test public void gifUsesPaletteAndHasNoAudio() {
        EditSpec s=spec();s.gif=true;List<String>a=s.args("in","out.gif");
        assertTrue(a.contains("-an"));assertTrue(a.stream().anyMatch(x->x.contains("palettegen")));assertFalse(a.contains("libx264"));
    }
    @Test public void slowAudioIsChainedWithinSupportedTempoRange() {EditSpec s=spec();s.speed=.25;assertEquals("asetpts=PTS-STARTPTS,atempo=0.5,atempo=0.500000",s.audioFilter());}
    @Test public void fastAudioIsChainedWithinSupportedTempoRange() {EditSpec s=spec();s.speed=4;assertEquals("asetpts=PTS-STARTPTS,atempo=2.0,atempo=2.000000",s.audioFilter());}
    @Test(expected=IllegalArgumentException.class) public void rejectsReverseTime() {EditSpec s=spec();s.end=.5;s.validate(10);}
    @Test(expected=IllegalArgumentException.class) public void rejectsNonfiniteTime() {EditSpec s=spec();s.start=Double.NaN;s.validate(10);}
    @Test(expected=IllegalArgumentException.class) public void rejectsOversizedGif() {EditSpec s=spec();s.gif=true;s.start=0;s.end=61;s.validate(70);}
    @Test public void gifLimitUsesOutputDuration() {EditSpec s=spec();s.gif=true;s.start=0;s.end=100;s.speed=2;s.validate(110);}
}
