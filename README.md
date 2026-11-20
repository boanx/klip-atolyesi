# Klip Atölyesi · Android

X bağlantısından video alma, telefondan video/GIF açma ve kaynak biçiminden bağımsız MP4 ya da hareketli GIF üretme uygulaması. Türkçe, reklamsız; düzenleme cihaz üzerinde çalışır. Android 10 ve üzeri; ARM64, ARMv7 ve x86_64 işlemciler.

**[Android APK'sını indir](https://github.com/boanx/klip-atolyesi/releases/latest/download/Klip-Atolyesi-1.0.1.apk)** · [Sürümler](https://github.com/boanx/klip-atolyesi/releases) · [Kurulum kılavuzu](docs/KURULUM.txt)

APK Android 15 emülatöründe gerçek X indirmesi ve MP4/GIF dönüşümleriyle doğrulanmıştır. Fiziksel telefonda ayrıca test edilmemiştir.

## Kullanım

1. Yukarıdaki bağlantıdan APK dosyasını Android telefonunuza indirin ve açın. Android istediğinde dosyayı açtığınız uygulama için “Bilinmeyen uygulamaları yükle” iznini etkinleştirin.
2. X’te gönderiyi paylaşırken **Klip Atölyesi**ni seçin veya gönderi bağlantısını uygulamaya yapıştırın. **X’ten indir** düğmesine dokunun. Alternatif olarak telefondan video veya GIF açın.
3. Başlangıç ve bitiş zamanını saniye olarak girin ya da sürgülerle seçin. Kırpma çerçevesinin köşelerini sürükleyin. Oran düğmesi merkezden bir oran seçer; serbestçe değiştirebilirsiniz.
4. Döndürme, aynalama, hız, görünüm ve genişlik seçin. Önizleme kaynak dosyadır; görüntü efektleri dışa aktarılırken uygulanır.
5. MP4 veya GIF biçimini seçin ve **Galeriye kaydet** düğmesine dokunun. Son çıktıyı açabilir veya paylaşabilirsiniz.

Videolar `Movies/KlipAtolyesi`, GIF’ler `Pictures/KlipAtolyesi` klasörüne kaydedilir. Bazı galeri uygulamaları GIF’leri ayrı “Animasyonlar” albümünde gösterebilir; Android Dosyalar uygulamasından da erişilebilir.

## Sınırlar

- Yalnızca erişilebilir X/Twitter gönderileri. Gizli, silinmiş, abonelik veya oturum açma gerektiren içerikler desteklenmeyebilir. X tarafındaki değişiklikler indirmeyi etkileyebilir; uygulamadaki **İndirme motorunu güncelle** düğmesini kullanın.
- Çoklu videolu gönderide ilk medya alınır. Profil, canlı yayın ve Spaces bağlantıları desteklenmez.
- GIF çıktısı sessizdir, döngülüdür ve en fazla 60 saniyedir. Kaynak dosya ve tek çıktı için 1 GB sınırı vardır. Çıktı genişliği 480/720/1080/1920 piksel seçenekleriyle sınırlanır, kaynak büyütülmez.
- Kırpma, H.264 uyumluluğu için çift piksele yuvarlanır. Video çıktısı H.264/AAC MP4’tür; GIF çıktısında palet optimizasyonu uygulanır. Yavaşlatılan videoda sesin tonu korunur.
- Son açılan kaynak dosya uygulamanın özel depolamasında tutulur ve başka bir dosya açıldığında değiştirilir. Galeriye kaydedilen çıktılar silinmez. İşlem sırasında uygulama arka plana alınabilir; zorla kapatma veya cihazın uygulamayı sonlandırması işlemi durdurabilir.
- X ile bağlantılı değildir. Yalnızca kaydetme ve kullanma hakkınız olan içerikleri işleyin.

## Gizlilik

Dosyalar düzenleme için bir sunucuya yüklenmez. Analiz veya reklam SDK’sı yoktur. İndirme X ve medya sunucularına, isteğe bağlı motor güncellemesi GitHub’a bağlanır. Galeri kaydı MediaStore, dosya seçimi Android’in sistem dosya seçicisiyle yapılır; genel depolama erişimi istenmez. İşlem bildirimi için bildirim izni isteğe bağlıdır.

## Kaynaktan derleme

Android Studio, JDK 17 veya daha yenisi (bu teslimde JDK 25), Android SDK 36 / Build Tools 36.0.0 ve internet bağlantısı gerekir. Android SDK konumunu kendi `local.properties` dosyanıza `sdk.dir=...` olarak yazın. Gradle wrapper dahildir.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testReleaseUnitTest lintRelease
.\gradlew.bat assembleRelease assembleReleaseAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Release imzası yerel `release-signing.properties` ve `klip-release.jks` dosyalarıyla korunur. Özel anahtar kaynak kod arşivine dahil edilmez. İmza dosyası bulunmazsa yerel release derlemesi geliştirme imzasını kullanır. Teslim APK’sı özel release anahtarıyla imzalıdır. Aynı uygulamayı güncelleyebilmek için yerel imza dosyalarını saklayın.

```properties
storeFile=klip-release.jks
storePassword=YOUR_PASSWORD
keyAlias=klip
keyPassword=YOUR_PASSWORD
```

## Doğrulama

`app/src/test` dönüşüm komutlarının ve zaman sınırlarının birim testlerini içerir. `app/src/androidTest` gerçek FFmpeg ikilisiyle 4 saniyelik sesli test videosu üretir; kırpma/döndürme, kesme/hız, MP4 ses koruma, video→GIF, GIF→MP4, sessiz çıktı ve MediaStore kaydını Android cihaz üzerinde doğrular.

```powershell
adb install -r app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/androidTest/release/app-release-androidTest.apk
adb shell am instrument -w com.klipatolyesi.app.test/com.klipatolyesi.app.EngineInstrumentation
```

GPL-3.0 kapsamında dağıtılır. Bağımlılıklar ve kaynak bağlantıları: `THIRD_PARTY_NOTICES.txt`.

## 1.0.1 düzeltmesi

Android'in bazı MP4 dosyalarında görüntü boyutunu veya süreyi boş döndürmesi nedeniyle görülen `s == null` hatası giderildi. Android bilgileri okuyamazsa uygulama kendi FFprobe ikilisiyle dosyayı analiz eder; eksik bilgiler artık doğrudan sayıya çevrilmez. Bildirilen gönderide indirme, MP4 ve GIF kaydı yeniden test edildi. 1.0.0 üzerine uygulamayı kaldırmadan güncellenebilir.
