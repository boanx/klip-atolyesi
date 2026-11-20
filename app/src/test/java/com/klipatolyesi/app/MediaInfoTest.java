package com.klipatolyesi.app;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;

public class MediaInfoTest {
    @Test public void formatDurationReplacesMissingStreamDuration() throws Exception {
        MediaInfo info=MediaInfo.fromProbe("{\"streams\":[{\"codec_type\":\"audio\"},{\"codec_type\":\"video\",\"width\":1280,\"height\":720}],\"format\":{\"duration\":\"12.34\"}}");
        assertEquals(1280,info.width);assertEquals(720,info.height);assertEquals(12.34,info.duration,.001);
    }
    @Test public void streamDurationReplacesMissingContainerDuration() throws Exception {
        MediaInfo info=MediaInfo.fromProbe("{\"streams\":[{\"codec_type\":\"video\",\"width\":720,\"height\":1280,\"duration\":\"3.5\"}]}");
        assertEquals(3.5,info.duration,.001);
    }
    @Test public void displayMatrixRotationUsesDisplayDimensions() throws Exception {
        MediaInfo info=MediaInfo.fromProbe("{\"streams\":[{\"codec_type\":\"video\",\"width\":1920,\"height\":1080,\"side_data_list\":[{\"rotation\":-90}]}],\"format\":{\"duration\":\"2\"}}");
        assertEquals(1080,info.width);assertEquals(1920,info.height);
    }
    @Test public void skipsCoverArtAndUsesVideoStream() throws Exception {
        MediaInfo info=MediaInfo.fromProbe("{\"streams\":[{\"codec_type\":\"video\",\"width\":300,\"height\":300,\"disposition\":{\"attached_pic\":1}},{\"codec_type\":\"video\",\"width\":640,\"height\":360}],\"format\":{\"duration\":\"5\"}}");
        assertEquals(640,info.width);
    }
    @Test(expected=IOException.class) public void rejectsNoVideo() throws Exception {MediaInfo.fromProbe("{\"streams\":[],\"format\":{\"duration\":\"3\"}}");}
    @Test(expected=IOException.class) public void rejectsMissingAndInvalidDurations() throws Exception {MediaInfo.fromProbe("{\"streams\":[{\"codec_type\":\"video\",\"width\":320,\"height\":240,\"duration\":null}],\"format\":{\"duration\":\"N/A\"}}");}
}
