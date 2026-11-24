package com.klipatolyesi.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.File;
import java.util.*;

public class MainActivity extends Activity implements MediaEngine.Listener {
    private static final int BG=0xff101516,CARD=0xff1b2425,ACCENT=0xffa8f078,TEXT=0xfff3f6f2,MUTED=0xffaab7b3;
    private final MediaEngine engine=MediaEngine.I;
    private LinearLayout root,editor,outputCard,quickSaveCard,stickerRows;
    private EditText url,start,end;
    private TextView status,error,info,previewError;
    private ProgressBar progress;
    private Button download,importButton,saveGif,saveVideo,saveSticker,exportGif,exportVideo,exportSticker,cancel,update,play,open,share;
    private Spinner cropPreset,speed,size,fps,rotate,filter;
    private Switch mute,mirror;
    private VideoView video;
    private GifPreview gif;
    private CropOverlay crop;
    private SeekBar startSeek,endSeek;
    private String loaded="";
    private boolean syncing;
    private String stickerSignature="";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable previewLoop=new Runnable() { public void run() {
        if(video!=null && video.isPlaying()) {
            try { if(video.getCurrentPosition()>=number(end,engine.duration)*1000) video.seekTo((int)(number(start,0)*1000)); } catch(Exception ignored) {}
        }
        handler.postDelayed(this,120);
    }};

    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(18),dp(20),dp(30));
        scroll.addView(root);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets) -> {
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(dp(20),dp(18)+bars.top,dp(20),dp(30)+bars.bottom);
            }
            return insets;
        });
        TextView eyebrow=text("BAĞLANTI → DÜZENLE → KAYDET",11,ACCENT);eyebrow.setLetterSpacing(.12f);root.addView(eyebrow);
        TextView title=text("Klip Atölyesi",32,TEXT);title.setTypeface(null,Typeface.BOLD);root.addView(title);
        root.addView(text("X’ten al. Kendi biçiminde kaydet.",15,MUTED));
        LinearLayout source=card(root);heading(source,"01  Medyanı ekle");
        url=input("X gönderisinin bağlantısı",false);url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);url.setSingleLine(true);source.addView(url);
        LinearLayout sourceRow=row(source);
        Button paste=button("Yapıştır",false);download=button("X’ten indir",true);sourceRow.addView(paste,weight());sourceRow.addView(download,weight());
        paste.setOnClickListener(v -> { ClipboardManager cb=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE); if(cb.hasPrimaryClip()) url.setText(cb.getPrimaryClip().getItemAt(0).coerceToText(this)); });
        download.setOnClickListener(v -> { hideKeyboard();notifications();engine.download(this,url.getText().toString()); });
        importButton=button("＋  Telefondan video / GIF aç",false);source.addView(importButton);
        importButton.setOnClickListener(v -> { Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"video/*","image/gif"});startActivityForResult(i,10); });
        LinearLayout stateCard=card(root);
        status=text("",14,TEXT);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);stateCard.addView(status);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));stateCard.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));
        error=text("",13,0xffffb5a8);error.setTextIsSelectable(true);stateCard.addView(error);
        cancel=button("İşlemi iptal et",false);stateCard.addView(cancel);cancel.setOnClickListener(v -> engine.cancel());

        quickSaveCard=card(root);heading(quickSaveCard,"Nasıl kaydetmek istersin?");
        quickSaveCard.addView(text("Kaynak video veya GIF olabilir. Seçili süre ve düzenlemeler uygulanır.",12,MUTED));
        saveGif=button("GIF olarak kaydet",true);saveGif.setId(R.id.save_gif);quickSaveCard.addView(saveGif);saveGif.setOnClickListener(v -> save(true));
        saveVideo=button("Video (MP4) olarak kaydet",false);saveVideo.setId(R.id.save_video);quickSaveCard.addView(saveVideo);saveVideo.setOnClickListener(v -> save(false));
        saveSticker=button("WhatsApp çıkartması oluştur",false);saveSticker.setId(R.id.save_sticker);quickSaveCard.addView(saveSticker);saveSticker.setOnClickListener(v -> saveSticker());
        quickSaveCard.addView(text("GIF sessiz ve döngülü kaydedilir. En fazla 60 saniye seçebilirsin.",12,MUTED));
        quickSaveCard.addView(text("Hareketli WhatsApp çıkartması: en fazla 10 saniye. Boyut ve sıkıştırma otomatik ayarlanır.",12,MUTED));

        LinearLayout stickerCard=card(root);heading(stickerCard,"WhatsApp çıkartma paketlerim");
        stickerCard.addView(text("GIF veya videodan çıkartma oluştur. Her pakette en az 3, en fazla 30 çıkartma bulunur. Yeni çıkartmalar son pakete eklenir; dolunca yeni paket açılır. Arka plan otomatik silinmez, görüntü şeffaf kenarlarla kareye yerleştirilir.",12,MUTED));
        stickerRows=column();stickerCard.addView(stickerRows);

        editor=card(root);heading(editor,"02  Kes, kırp ve biçimlendir");
        info=text("",12,MUTED);editor.addView(info);
        FrameLayout preview=new FrameLayout(this); preview.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams previewParams=new LinearLayout.LayoutParams(-1,dp(230));previewParams.topMargin=dp(12);editor.addView(preview,previewParams);
        video=new VideoView(this);preview.addView(video,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        gif=new GifPreview(this);preview.addView(gif,new FrameLayout.LayoutParams(-1,-1));
        crop=new CropOverlay(this);preview.addView(crop,new FrameLayout.LayoutParams(-1,-1));
        previewError=text("Bu dosya için önizleme kullanılamıyor. Dönüştürerek kaydetmeyi deneyebilirsiniz.",12,MUTED);previewError.setVisibility(View.GONE);editor.addView(previewError);
        video.setOnPreparedListener(mp -> { previewError.setVisibility(View.GONE);mp.setLooping(false); video.seekTo((int)(number(start,0)*1000)); });
        video.setOnErrorListener((mp,what,extra) -> { if(!engine.busy && !engine.sourceGif)previewError.setVisibility(View.VISIBLE);return true; });
        play=button("▶  Seçili bölümü oynat / duraklat",false);editor.addView(play);
        play.setOnClickListener(v -> { if(video.isPlaying()) video.pause(); else { int pos=video.getCurrentPosition();if(pos<number(start,0)*1000 || pos>=number(end,engine.duration)*1000)video.seekTo((int)(number(start,0)*1000));video.start(); } });
        editor.addView(text("Köşelerden kırpın, ortadan tutup taşıyın. Önizleme kaynak dosyayı gösterir; efektler kaydedilirken uygulanır.",12,MUTED));
        cropPreset=spinner(editor,"Kırpma oranı",new String[]{"Serbest / tamamı","Kare · 1:1","Dikey · 9:16","Yatay · 16:9","Portre · 4:5"});
        cropPreset.setOnItemSelectedListener(selected(position -> { double[] ratios={0,1,9.0/16,16.0/9,4.0/5};crop.ratio(ratios[position]); }));
        LinearLayout times=row(editor);
        LinearLayout a=column(), b=column();times.addView(a,weight());times.addView(b,weight());
        a.addView(text("Başlangıç · saniye",12,MUTED));start=input("0.00",true);a.addView(start);
        b.addView(text("Bitiş · saniye",12,MUTED));end=input("0.00",true);b.addView(end);
        editor.addView(text("Başlangıç noktası",12,MUTED));startSeek=new SeekBar(this);startSeek.setMax(10000);editor.addView(startSeek);
        editor.addView(text("Bitiş noktası",12,MUTED));endSeek=new SeekBar(this);endSeek.setMax(10000);editor.addView(endSeek);
        startSeek.setOnSeekBarChangeListener(seeker(true));endSeek.setOnSeekBarChangeListener(seeker(false));
        start.setOnFocusChangeListener((v,focus) -> {if(!focus) syncSliders();});end.setOnFocusChangeListener((v,focus) -> {if(!focus)syncSliders();});
        rotate=spinner(editor,"Döndürme",new String[]{"Döndürme yok","90° sağa","180°","270° sağa"});
        mirror=new Switch(this);mirror.setText("Yatay aynala");mirror.setTextColor(TEXT);editor.addView(mirror);
        speed=spinner(editor,"Oynatma hızı",new String[]{"0,25×","0,5×","0,75×","1× · normal","1,5×","2×","4×"});speed.setSelection(3);
        filter=spinner(editor,"Görünüm",new String[]{"Doğal","Canlı","Siyah beyaz","Sıcak"});
        heading(editor,"03  Çıkışını seç");
        size=spinner(editor,"En fazla genişlik · büyütme yapılmaz",new String[]{"480 piksel · küçük dosya","720 piksel · dengeli","1080 piksel · yüksek","1920 piksel · en yüksek"});size.setSelection(1);
        fps=spinner(editor,"GIF akıcılığı",new String[]{"10 kare/sn","15 kare/sn","20 kare/sn","24 kare/sn"});fps.setSelection(1);
        mute=new Switch(this);mute.setText("Videonun sesini kaldır");mute.setTextColor(TEXT);editor.addView(mute);
        editor.addView(text("GIF ses içermez. GIF çıktısı en fazla 60 saniye olabilir. Video kaydında ses varsa korunur.",12,MUTED));
        exportGif=button("GIF olarak kaydet",true);editor.addView(exportGif);exportGif.setOnClickListener(v -> save(true));
        exportVideo=button("Video (MP4) olarak kaydet",false);editor.addView(exportVideo);exportVideo.setOnClickListener(v -> save(false));
        exportSticker=button("WhatsApp çıkartması oluştur",false);editor.addView(exportSticker);exportSticker.setOnClickListener(v -> saveSticker());

        outputCard=card(root);heading(outputCard,"Son kaydın hazır");outputCard.addView(text("Videolar: Movies/KlipAtolyesi\nGIF’ler: Pictures/KlipAtolyesi",13,MUTED));
        LinearLayout outputRow=row(outputCard);open=button("Dosyayı aç",false);share=button("Paylaş",true);outputRow.addView(open,weight());outputRow.addView(share,weight());
        open.setOnClickListener(v -> output(false));share.setOnClickListener(v -> output(true));
        LinearLayout footer=card(root);update=button("İndirme motorunu güncelle",false);footer.addView(update);update.setOnClickListener(v -> {notifications();engine.update(this);});
        footer.addView(text("X indirmesi başarısız olursa motoru güncellemeyi deneyin. Gizli, silinmiş ve giriş gerektiren gönderiler desteklenmeyebilir. Çoklu videolu gönderilerde ilk medya alınır.\n\nKlip Atölyesi 1.1.1 · Reklamsız\nDüzenleme ve dönüştürme cihazda yapılır. X veya WhatsApp ile bağlantılı değildir.",12,MUTED));
        Button about=button("Kullanım ve açık kaynak lisansları",false);footer.addView(about);about.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Klip Atölyesi").setMessage("Yalnızca kaydetme ve kullanma hakkınız bulunan içerikleri işleyin.\n\nİndirme: yt-dlp / youtubedl-android\nDönüştürme: FFmpeg\nUygulama ve bağımlılıklar GPL-3.0 kapsamında sunulur. Kaynak kod teslim paketindedir.\n\nhttps://github.com/yausername/youtubedl-android\nhttps://github.com/yt-dlp/yt-dlp\nhttps://ffmpeg.org\n\nUygulama geliştiricisine dosya veya analiz verisi gönderilmez. X indirmeleri X sunucularıyla; motor güncellemesi GitHub ile iletişim kurar.").setPositiveButton("Tamam",null).show());
        engine.listener=this;
        if(state!=null) { url.setText(state.getString("url","")); }
        handleIntent(getIntent());
        if(engine.source==null && !engine.busy) engine.restore(this);
        changed();
    }
    public void changed() {
        if(isFinishing() || isDestroyed()) return;
        status.setText(engine.status);error.setText(engine.error);error.setVisibility(engine.error.isEmpty()?View.GONE:View.VISIBLE);
        progress.setVisibility(engine.busy?View.VISIBLE:View.GONE);progress.setIndeterminate(engine.progress<0);progress.setProgress(Math.max(0,engine.progress));
        cancel.setVisibility(engine.busy?View.VISIBLE:View.GONE);
        download.setEnabled(!engine.busy);importButton.setEnabled(!engine.busy);update.setEnabled(!engine.busy);
        saveGif.setEnabled(!engine.busy);saveVideo.setEnabled(!engine.busy);exportGif.setEnabled(!engine.busy);exportVideo.setEnabled(!engine.busy);
        saveSticker.setEnabled(!engine.busy);exportSticker.setEnabled(!engine.busy);refreshStickerPacks();
        quickSaveCard.setVisibility(engine.source==null?View.GONE:View.VISIBLE);
        editor.setVisibility(engine.source==null?View.GONE:View.VISIBLE);outputCard.setVisibility(engine.result==null?View.GONE:View.VISIBLE);
        crop.setEnabled(!engine.busy);
        gif.setAnimating(!engine.busy);
        if(engine.source!=null && !loaded.equals(engine.source.getAbsolutePath())) {
            loaded=engine.source.getAbsolutePath();video.stopPlayback();gif.clear();previewError.setVisibility(View.GONE);
            crop.media(engine.width,engine.height);cropPreset.setSelection(0);
            start.setText("0.00");end.setText(String.format(Locale.US,"%.3f",engine.duration));syncSliders();
            info.setText(engine.sourceName+" · "+engine.width+" × "+engine.height+" · "+String.format(Locale.getDefault(),"%.2f sn",engine.duration));
            gif.setVisibility(engine.sourceGif?View.VISIBLE:View.GONE);video.setVisibility(engine.sourceGif?View.GONE:View.VISIBLE);play.setVisibility(engine.sourceGif?View.GONE:View.VISIBLE);
            if(engine.sourceGif) gif.load(loaded);else video.setVideoPath(loaded);
        }
    }
    private void save(boolean gifOutput) {
        hideKeyboard();video.pause();notifications();
        engine.export(this,selectedEdit(gifOutput));
    }
    private void saveSticker() {
        hideKeyboard();video.pause();notifications();engine.exportSticker(this,selectedEdit(false));
    }
    private EditSpec selectedEdit(boolean gifOutput) {
        EditSpec s=new EditSpec();s.start=number(start,Double.NaN);s.end=number(end,Double.NaN);s.width=engine.width;s.height=engine.height;
        s.left=crop.left;s.top=crop.top;s.right=crop.right;s.bottom=crop.bottom;
        s.rotation=rotate.getSelectedItemPosition()*90;s.mirror=mirror.isChecked();s.filter=filter.getSelectedItemPosition();
        s.speed=new double[]{.25,.5,.75,1,1.5,2,4}[speed.getSelectedItemPosition()];s.maxWidth=new int[]{480,720,1080,1920}[size.getSelectedItemPosition()];
        s.fps=new int[]{10,15,20,24}[fps.getSelectedItemPosition()];s.gif=gifOutput;s.mute=mute.isChecked();
        return s;
    }
    private void refreshStickerPacks() {
        try {
            List<StickerStore.Pack> packs=StickerStore.list(this);StringBuilder signature=new StringBuilder(Boolean.toString(engine.busy));
            for(StickerStore.Pack p:packs)signature.append(p.id).append(p.version);
            if(signature.toString().equals(stickerSignature))return;stickerSignature=signature.toString();stickerRows.removeAllViews();
            if(packs.isEmpty())stickerRows.addView(text("Henüz çıkartma yok. Bir GIF veya video açıp ‘WhatsApp çıkartması oluştur’a dokun.",13,MUTED));
            for(StickerStore.Pack pack:packs) {
                int count=pack.stickers.size();stickerRows.addView(text(pack.name+" · "+count+"/30 çıkartma",14,TEXT));
                if(count<3)stickerRows.addView(text("WhatsApp’a eklemek için "+(3-count)+" çıkartma daha oluştur.",12,MUTED));
                Button add=button("WhatsApp’a ekle",true);add.setEnabled(count>=3 && !engine.busy);add.setContentDescription(pack.name+" paketini WhatsApp’a ekle");stickerRows.addView(add);add.setOnClickListener(v -> addStickerPack(pack));
            }
        } catch(Exception e) {stickerRows.removeAllViews();stickerRows.addView(text("Çıkartma paketleri okunamadı: "+e.getMessage(),13,0xffffb5a8));}
    }
    private void addStickerPack(StickerStore.Pack pack) {
        List<String> packages=new ArrayList<>(),labels=new ArrayList<>();
        for(String name:new String[]{"com.whatsapp","com.whatsapp.w4b"})try {getPackageManager().getPackageInfo(name,0);packages.add(name);labels.add(name.equals("com.whatsapp")?"WhatsApp":"WhatsApp Business");}catch(PackageManager.NameNotFoundException ignored){}
        if(packages.isEmpty()) {new AlertDialog.Builder(this).setTitle("WhatsApp bulunamadı").setMessage("WhatsApp veya WhatsApp Business yüklü olmalı. Hazırladığın çıkartmalar uygulamada saklanıyor; yükledikten sonra bu düğmeyle ekleyebilirsin.").setPositiveButton("Tamam",null).show();return;}
        if(packages.size()==1)openStickerPack(pack,packages.get(0));
        else new AlertDialog.Builder(this).setTitle("Paketi nereye ekleyelim?").setItems(labels.toArray(new String[0]),(dialog,which)->openStickerPack(pack,packages.get(which))).show();
    }
    private void openStickerPack(StickerStore.Pack pack,String target) {
        try {startActivityForResult(StickerStore.addIntent(pack,target),20);}
        catch(ActivityNotFoundException e) {Toast.makeText(this,"WhatsApp çıkartma ekleme ekranını açamadı. WhatsApp’ı güncellemeyi deneyin.",Toast.LENGTH_LONG).show();}
    }
    private void output(boolean sharing) {
        try {
            Intent i=new Intent(sharing?Intent.ACTION_SEND:Intent.ACTION_VIEW).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if(sharing) { i.setType(engine.resultMime);i.putExtra(Intent.EXTRA_STREAM,engine.result);i.setClipData(ClipData.newRawUri("Klip",engine.result)); }
            else i.setDataAndType(engine.result,engine.resultMime);
            startActivity(Intent.createChooser(i,sharing?"Kaydı paylaş":"Kaydı aç"));
        } catch(ActivityNotFoundException e) { Toast.makeText(this,"Bu dosyayı açabilecek bir uygulama bulunamadı.",Toast.LENGTH_LONG).show(); }
    }
    private void handleIntent(Intent intent) {
        if(intent==null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        if(engine.busy) { Toast.makeText(this,"Önce devam eden işlemin tamamlanmasını bekleyin.",Toast.LENGTH_LONG).show();return; }
        if("text/plain".equals(intent.getType())) {
            String text=intent.getStringExtra(Intent.EXTRA_TEXT);if(text!=null) { url.setText(text); }
        } else {
            Uri uri=intent.getParcelableExtra(Intent.EXTRA_STREAM);if(uri!=null)engine.importFile(this,uri);
        }
        setIntent(new Intent(this,MainActivity.class));
    }
    protected void onNewIntent(Intent i) {super.onNewIntent(i);handleIntent(i);}
    protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==10 && result==RESULT_OK && data!=null && data.getData()!=null) {notifications();engine.importFile(this,data.getData());}
        if(request==20) {
            if(result==RESULT_OK)Toast.makeText(this,"Çıkartma paketi WhatsApp’a eklendi.",Toast.LENGTH_LONG).show();
            else if(data!=null && data.getStringExtra("validation_error")!=null)new AlertDialog.Builder(this).setTitle("WhatsApp paketi eklemedi").setMessage(data.getStringExtra("validation_error")).setPositiveButton("Tamam",null).show();
        }
    }
    protected void onStart() {super.onStart();engine.listener=this;changed();handler.post(previewLoop);}
    protected void onStop() {handler.removeCallbacks(previewLoop);video.pause();if(engine.listener==this)engine.listener=null;super.onStop();}
    protected void onDestroy() {handler.removeCallbacks(previewLoop);video.stopPlayback();gif.clear();if(engine.listener==this)engine.listener=null;super.onDestroy();}
    protected void onSaveInstanceState(Bundle state) {state.putString("url",url.getText().toString());super.onSaveInstanceState(state);}
    void notifications() {if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("asked",false)) {getPreferences(0).edit().putBoolean("asked",true).apply();requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9);}}
    void hideKeyboard() {((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(url.getWindowToken(),0);url.clearFocus();start.clearFocus();end.clearFocus();}
    private void syncSliders() {if(syncing)return;syncing=true;double d=engine.duration;if(d>0) {startSeek.setProgress((int)(10000*number(start,0)/d));endSeek.setProgress((int)(10000*number(end,d)/d));}syncing=false;}
    private SeekBar.OnSeekBarChangeListener seeker(boolean first) {return new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar bar,int p,boolean user) {if(!user || syncing)return;double val=engine.duration*p/10000;
            if(first)val=Math.min(val,Math.max(0,number(end,engine.duration)-.05));else val=Math.max(val,Math.min(engine.duration,number(start,0)+.05));
            (first?start:end).setText(String.format(Locale.US,"%.3f",val));if(first && !engine.sourceGif)video.seekTo((int)(val*1000));syncSliders();}
        public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
    };}
    static double number(EditText field,double fallback) {try{return Double.parseDouble(field.getText().toString().replace(',','.'));}catch(Exception e){return fallback;}}
    interface Selection {void choose(int position);}
    AdapterView.OnItemSelectedListener selected(Selection action) {return new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> a,View v,int p,long id){action.choose(p);}public void onNothingSelected(AdapterView<?> a){}};}
    private Spinner spinner(LinearLayout parent,String label,String[] choices) {
        TextView l=text(label,12,MUTED);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);parent.addView(l,lp);
        Spinner s=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,choices);s.setAdapter(adapter);s.setContentDescription(label);parent.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));return s;
    }
    int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    TextView text(String t,int size,int color){TextView v=new TextView(this);v.setText(t);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(3),0,dp(5));return v;}
    EditText input(String hint,boolean numeric){EditText v=new EditText(this);v.setTextColor(TEXT);v.setTextSize(15);v.setHintTextColor(MUTED);v.setHint(hint);v.setSelectAllOnFocus(numeric);v.setInputType(numeric?InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL:InputType.TYPE_CLASS_TEXT);v.setMinHeight(dp(48));return v;}
    Button button(String t,boolean primary){Button b=new Button(this);b.setText(t);b.setTextSize(13);b.setAllCaps(false);b.setMinHeight(dp(48));b.setTextColor(primary?BG:TEXT);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?ACCENT:0xff30403b));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);b.setLayoutParams(p);return b;}
    LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    LinearLayout card(LinearLayout parent){LinearLayout v=column();v.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawable bg=new GradientDrawable();bg.setColor(CARD);bg.setCornerRadius(dp(20));v.setBackground(bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(18);parent.addView(v,p);return v;}
    void heading(LinearLayout p,String t){TextView v=text(t,17,TEXT);v.setTypeface(null,Typeface.BOLD);p.addView(v);}
    LinearLayout row(LinearLayout p){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);p.addView(r,new LinearLayout.LayoutParams(-1,-2));return r;}
    LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(2),dp(4),dp(2),0);return p;}
}
