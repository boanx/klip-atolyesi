package com.klipatolyesi.app;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Read RIFF metadata without relying on platform codec support. */
public final class WebpInfo {
    public int width,height,frames,durationMs,minFrameMs=Integer.MAX_VALUE,loopCount=-1;
    public boolean animation;
    public static WebpInfo read(File file) throws IOException {
        if(file.length()>StickerSpec.MAX_BYTES || file.length()<12) throw new IOException("Çıkartma dosyası 500 KB sınırına uygun değil.");
        try(InputStream in=new FileInputStream(file)) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1) bytes.write(buffer,0,n);
            return read(bytes.toByteArray());
        }
    }
    public static WebpInfo read(byte[] b) throws IOException {
        if(b.length<12 || !tag(b,0,"RIFF") || !tag(b,8,"WEBP") || u32(b,4)+8!=b.length) throw new IOException("Geçerli WebP dosyası oluşturulamadı.");
        WebpInfo info=new WebpInfo();
        for(int pos=12;pos<b.length;) {
            if(pos+8>b.length) throw new IOException("Eksik WebP parçası.");
            long size=u32(b,pos+4);long next=pos+8L+size+(size&1);if(next>b.length)throw new IOException("Eksik WebP verisi.");
            int data=pos+8;
            if(tag(b,pos,"VP8X") && size>=10) {info.animation=(b[data]&2)!=0;info.width=u24(b,data+4)+1;info.height=u24(b,data+7)+1;}
            if(tag(b,pos,"ANIM") && size>=6) info.loopCount=(b[data+4]&255)|((b[data+5]&255)<<8);
            if(tag(b,pos,"ANMF") && size>=16) {int duration=u24(b,data+12);info.frames++;info.durationMs+=duration;info.minFrameMs=Math.min(info.minFrameMs,duration);}
            pos=(int)next;
        }
        return info;
    }
    public void validateSticker() throws IOException {
        if(width!=512 || height!=512 || !animation || frames<2 || durationMs>10000 || durationMs<=0 || minFrameMs<8 || loopCount!=0)
            throw new IOException("Çıkartma hareket, boyut veya süre koşullarına uymadı. Daha kısa bir bölüm deneyin.");
    }
    private static boolean tag(byte[] b,int offset,String s) {return offset+4<=b.length && s.equals(new String(b,offset,4,StandardCharsets.US_ASCII));}
    private static int u24(byte[] b,int p) {return (b[p]&255)|((b[p+1]&255)<<8)|((b[p+2]&255)<<16);}
    private static long u32(byte[] b,int p) {return (long)u24(b,p)|((long)(b[p+3]&255)<<24);}
}
