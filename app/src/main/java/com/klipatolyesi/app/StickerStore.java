package com.klipatolyesi.app;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Private persistent packs; publishing the index atomically exposes only complete files. */
public final class StickerStore {
    public static final String AUTHORITY="com.klipatolyesi.app.stickercontentprovider";
    public static final int MIN_STICKERS=3,MAX_STICKERS=30,MAX_PACKS=10;
    public static final class Pack {
        public final String id,name,version;public final List<String> stickers;
        Pack(String id,String name,String version,List<String> stickers) {this.id=id;this.name=name;this.version=version;this.stickers=Collections.unmodifiableList(stickers);}
    }
    private static File root(Context c) {return new File(c.getFilesDir(),"sticker-packs");}
    public static synchronized List<Pack> list(Context c) throws IOException {
        AtomicFile index=new AtomicFile(new File(root(c),"index.json"));
        if(!index.getBaseFile().exists() && !new File(index.getBaseFile()+".bak").exists())return new ArrayList<>();
        try(InputStream in=index.openRead()) {
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
            JSONArray all=new JSONArray(out.toString(StandardCharsets.UTF_8.name()));List<Pack> result=new ArrayList<>();
            for(int i=0;i<all.length();i++) {JSONObject p=all.getJSONObject(i);JSONArray files=p.getJSONArray("stickers");List<String> names=new ArrayList<>();for(int j=0;j<files.length();j++)names.add(files.getString(j));result.add(new Pack(p.getString("id"),p.getString("name"),p.getString("version"),names));}
            return result;
        } catch(JSONException e) {throw new IOException("Çıkartma paketleri okunamadı.",e);}
    }
    public static synchronized String add(Context c,File webp) throws Exception {
        WebpInfo.read(webp).validateSticker();List<Pack> packs=list(c);Pack pack=packs.isEmpty()?null:packs.get(packs.size()-1);
        if(pack==null || pack.stickers.size()>=MAX_STICKERS) {
            if(packs.size()>=MAX_PACKS)throw new IOException("En fazla 10 paket oluşturulabilir. Paketlerin tamamı dolu.");
            pack=new Pack("klip_"+UUID.randomUUID().toString().replace("-",""),"Klip Atölyesi "+(packs.size()+1),"1",new ArrayList<>());packs.add(pack);
        }
        File folder=new File(root(c),pack.id);if(!folder.isDirectory() && !folder.mkdirs())throw new IOException("Çıkartma klasörü oluşturulamadı.");
        String filename="s_"+UUID.randomUUID().toString().replace("-","")+".webp";File dest=new File(folder,filename);
        try {
            try(InputStream in=new FileInputStream(webp);OutputStream out=new FileOutputStream(dest)) {byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            File tray=new File(folder,"tray.png");if(!tray.isFile())makeTray(c,tray);
            List<String> files=new ArrayList<>(pack.stickers);files.add(filename);
            Pack updated=new Pack(pack.id,pack.name,UUID.randomUUID().toString(),files);packs.set(packs.size()-1,updated);write(c,packs);
            c.getContentResolver().notifyChange(android.net.Uri.parse("content://"+AUTHORITY+"/metadata"),null);
            return pack.id;
        } catch(Exception e) {dest.delete();throw e;}
    }
    private static void makeTray(Context c,File file) throws IOException {
        Bitmap bitmap=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);Drawable icon=c.getDrawable(R.drawable.ic_app);icon.setBounds(0,0,96,96);icon.draw(new Canvas(bitmap));
        try(OutputStream out=new FileOutputStream(file)) {if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Paket simgesi oluşturulamadı.");}finally{bitmap.recycle();}
    }
    private static void write(Context c,List<Pack> packs) throws Exception {
        JSONArray all=new JSONArray();for(Pack pack:packs){JSONObject p=new JSONObject();p.put("id",pack.id);p.put("name",pack.name);p.put("version",pack.version);p.put("stickers",new JSONArray(pack.stickers));all.put(p);}
        AtomicFile index=new AtomicFile(new File(root(c),"index.json"));FileOutputStream out=null;
        try {out=index.startWrite();out.write(all.toString().getBytes(StandardCharsets.UTF_8));index.finishWrite(out);}catch(Exception e){if(out!=null)index.failWrite(out);throw e;}
    }
    public static synchronized File asset(Context c,String id,String name) throws IOException {
        for(Pack p:list(c))if(p.id.equals(id) && (name.equals("tray.png") || p.stickers.contains(name))) {
            File base=new File(root(c),id).getCanonicalFile(),target=new File(base,name).getCanonicalFile();
            if(!target.getParentFile().equals(base) || !target.isFile())break;
            return target;
        }
        throw new FileNotFoundException("Çıkartma dosyası bulunamadı.");
    }
    public static Intent addIntent(Pack pack,String packageName) {
        return new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK").setPackage(packageName)
            .putExtra("sticker_pack_id",pack.id).putExtra("sticker_pack_authority",AUTHORITY).putExtra("sticker_pack_name",pack.name);
    }
}
