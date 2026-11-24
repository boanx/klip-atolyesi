package com.klipatolyesi.app;

import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;
import java.util.*;

/** Read-only WhatsApp sticker protocol. Only indexed sticker assets can be opened. */
public class StickerContentProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    private List<String> path(Uri uri) {if(!StickerStore.AUTHORITY.equals(uri.getAuthority()))throw new IllegalArgumentException("Unknown authority");return uri.getPathSegments();}
    public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
        List<String> p=path(uri);
        try {
            List<StickerStore.Pack> packs=StickerStore.list(getContext());MatrixCursor cursor;
            if((p.size()==1 || p.size()==2) && p.get(0).equals("metadata")) {
                cursor=new MatrixCursor(new String[]{"sticker_pack_identifier","sticker_pack_name","sticker_pack_publisher","sticker_pack_icon","android_play_store_link","ios_app_download_link","sticker_pack_publisher_email","sticker_pack_publisher_website","sticker_pack_privacy_policy_website","sticker_pack_license_agreement_website","image_data_version","whatsapp_will_not_cache_stickers","animated_sticker_pack"});
                for(StickerStore.Pack pack:packs)if(p.size()==1 || p.get(1).equals(pack.id))cursor.addRow(new Object[]{pack.id,pack.name,"Klip Atölyesi","tray.png","","","","https://github.com/boanx/klip-atolyesi","","",pack.version,0,1});
            } else if(p.size()==2 && p.get(0).equals("stickers")) {
                cursor=new MatrixCursor(new String[]{"sticker_file_name","sticker_emoji","sticker_accessibility_text"});
                for(StickerStore.Pack pack:packs)if(pack.id.equals(p.get(1)))for(String name:pack.stickers)cursor.addRow(new Object[]{name,"😊","Animated clip"});
            } else throw new IllegalArgumentException("Unknown sticker query");
            cursor.setNotificationUri(getContext().getContentResolver(),uri);return cursor;
        } catch(IOException e){throw new IllegalStateException("Sticker pack index unavailable",e);}
    }
    public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode))throw new FileNotFoundException("Read only");List<String> p=path(uri);
        if(p.size()!=3 || !p.get(0).equals("stickers_asset"))throw new FileNotFoundException("Unknown sticker path");
        try{return ParcelFileDescriptor.open(StickerStore.asset(getContext(),p.get(1),p.get(2)),ParcelFileDescriptor.MODE_READ_ONLY);}catch(IOException e){throw new FileNotFoundException(e.getMessage());}
    }
    public AssetFileDescriptor openAssetFile(Uri uri,String mode) throws FileNotFoundException {return new AssetFileDescriptor(openFile(uri,mode),0,AssetFileDescriptor.UNKNOWN_LENGTH);}
    public String getType(Uri uri) {
        List<String> p=path(uri);
        if(p.size()==3 && p.get(0).equals("stickers_asset"))return p.get(2).equals("tray.png")?"image/png":"image/webp";
        if(p.size()==1 && p.get(0).equals("metadata"))return "vnd.android.cursor.dir/vnd."+StickerStore.AUTHORITY+".metadata";
        if(p.size()==2 && p.get(0).equals("metadata"))return "vnd.android.cursor.item/vnd."+StickerStore.AUTHORITY+".metadata";
        if(p.size()==2 && p.get(0).equals("stickers"))return "vnd.android.cursor.dir/vnd."+StickerStore.AUTHORITY+".stickers";
        throw new IllegalArgumentException("Unknown sticker URI");
    }
    public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException("Read only");}
    public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException("Read only");}
    public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException("Read only");}
}
