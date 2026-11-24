# Klip Atölyesi · Android

X bağlantısından video alma, telefondan video/GIF açma ve kaynak biçiminden bağımsız MP4 ya da hareketli GIF üretme uygulaması. Türkçe, reklamsız; düzenleme cihaz üzerinde çalışır. Android 10 ve üzeri; ARM64, ARMv7 ve x86_64 işlemciler.

**[Android APK'sını indir](https://github.com/boanx/klip-atolyesi/releases/latest/download/Klip-Atolyesi-1.1.1.apk)** · [Sürümler](https://github.com/boanx/klip-atolyesi/releases) · [Kurulum kılavuzu](docs/KURULUM.txt)

APK Android 15 emülatöründe gerçek X indirmesi ve MP4/GIF dönüşümleriyle doğrulanmıştır. Fiziksel telefonda ayrıca test edilmemiştir.

## Kullanım

1. Yukarıdaki bağlantıdan APK dosyasını Android telefonunuza indirin ve açın. Android istediğinde dosyayı açtığınız uygulama için “Bilinmeyen uygulamaları yükle” iznini etkinleştirin.
2. X’te gönderiyi paylaşırken **Klip Atölyesi**ni seçin veya gönderi bağlantısını uygulamaya yapıştırın. **X’ten indir** düğmesine dokunun. Alternatif olarak telefondan video veya GIF açın.
3. Başlangıç ve bitiş zamanını saniye olarak girin ya da sürgülerle seçin. Kırpma çerçevesinin köşelerini sürükleyin. Oran düğmesi merkezden bir oran seçer; serbestçe değiştirebilirsiniz.
4. Döndürme, aynalama, hız, görünüm ve genişlik seçin. Önizleme kaynak dosyadır; görüntü efektleri dışa aktarılırken uygulanır.
5. **GIF olarak kaydet** veya **Video (MP4) olarak kaydet** düğmesine dokunun. Düğmeler dosyayı açtıktan hemen sonra ve düzenleme seçeneklerinin altında görünür. Son çıktıyı açabilir veya paylaşabilirsiniz.

Videolar `Movies/KlipAtolyesi`, GIF’ler `Pictures/KlipAtolyesi` klasörüne kaydedilir. Bazı galeri uygulamaları GIF’leri ayrı “Animasyonlar” albümünde gösterebilir; Android Dosyalar uygulamasından da erişilebilir.

## GIF veya videodan hareketli WhatsApp çıkartması

1. Telefonda bulunan bir GIF/video açın veya X’ten indirin.
2. En fazla 10 saniyelik bölüm seçin. Kesme, kırpma, hız ve görünüm ayarları çıkartmaya uygulanır.
3. **WhatsApp çıkartması oluştur** düğmesine dokunun. Uygulama hareketli, sessiz WebP dosyasını otomatik sıkıştırıp pakete kaydeder.
4. Pakette en az **3 çıkartma** olduğunda **WhatsApp’a ekle** düğmesine dokunun. WhatsApp’ın açtığı ekleme ekranını onaylayın. WhatsApp ve WhatsApp Business birlikte yüklüyse uygulama seçebilirsiniz.

Çıkartmalar 512×512 piksel, en fazla 10 saniye ve 500 KB olur. Şeffaf kenarlar eklenerek görüntü kareye sığdırılır; görüntünün kendi arka planı otomatik kaldırılmaz. Kaynak genişliği/uzunluğu korunur; genel GIF/video genişlik seçeneği çıkartmanın 512×512 ölçüsünü değiştirmez. Karmaşık görüntülerde 500 KB için kalite ve kare hızı otomatik düşürülür. Sınıra sığmayan içerikte daha kısa bölüm seçmeniz istenir.

Çıkartma oluşturma artık hızlı profili kullanır: kısa içerikler 10, uzun içerikler 8 kare/sn ile başlar; gerekirse dosya boyutuna göre kalite/kare hızı düşürülür. Aynı kaynak ve aynı ayarlarla son oluşturulan çıkartma tekrar istendiğinde doğrulanmış önbellek sonucu kullanılır. Farklı kesme/kırpma/hız ayarları yeniden dönüştürülür. Önizleme işlem sırasında duraklatılarak kaynak tüketimi azaltılır.

Paketler uygulamanın özel depolamasında kalıcı olarak saklanır; başka bir GIF açmak önceki çıkartmaları silmez. Her pakette 3–30 çıkartma bulunur. Son paket dolunca yeni paket açılır; en fazla 10 paket desteklenir. WhatsApp yüklenmemişse çıkartma oluşturabilirsiniz; eklemek için WhatsApp veya WhatsApp Business gerekir.

En az üç çıkartma, 512×512, WebP, 500 KB ve 10 saniye koşulları [WhatsApp’ın resmi Android çıkartma kılavuzuna](https://github.com/WhatsApp/stickers/tree/main/Android) göre uygulanır. Paket ekleme, kullanıcı onaylı resmi Android intent’iyle yapılır; mesaj veya sohbet gönderilmez.

## Sınırlar

- Yalnızca erişilebilir X/Twitter gönderileri. Gizli, silinmiş, abonelik veya oturum açma gerektiren içerikler desteklenmeyebilir. X tarafındaki değişiklikler indirmeyi etkileyebilir; uygulamadaki **İndirme motorunu güncelle** düğmesini kullanın.
- Çoklu videolu gönderide ilk medya alınır. Profil, canlı yayın ve Spaces bağlantıları desteklenmez.
- GIF çıktısı sessizdir, döngülüdür ve en fazla 60 saniyedir. Kaynak dosya ve tek çıktı için 1 GB sınırı vardır. Çıktı genişliği 480/720/1080/1920 piksel seçenekleriyle sınırlanır, kaynak büyütülmez.
- Kırpma, H.264 uyumluluğu için çift piksele yuvarlanır. Video çıktısı H.264/AAC MP4’tür; GIF çıktısında palet optimizasyonu uygulanır. Yavaşlatılan videoda sesin tonu korunur.
- Son açılan kaynak dosya uygulamanın özel depolamasında tutulur ve başka bir dosya açıldığında değiştirilir. Galeriye kaydedilen çıktılar silinmez. İşlem sırasında uygulama arka plana alınabilir; zorla kapatma veya cihazın uygulamayı sonlandırması işlemi durdurabilir.
- X ile bağlantılı değildir. Yalnızca kaydetme ve kullanma hakkınız olan içerikleri işleyin.

## Gizlilik

Dosyalar düzenleme için bir sunucuya yüklenmez. Analiz veya reklam SDK’sı yoktur. İndirme X ve medya sunucularına, isteğe bağlı motor güncellemesi GitHub’a bağlanır. Galeri kaydı MediaStore, dosya seçimi Android’in sistem dosya seçicisiyle yapılır; genel depolama erişimi istenmez. İşlem bildirimi için bildirim izni isteğe bağlıdır.

Çıkartma sağlayıcısı sadece paket dizinindeki kayıtlı WebP/PNG dosyalarını WhatsApp’ın `com.whatsapp.sticker.READ` okuma izniyle sunar. Kaynak videoları, diğer özel dosyaları veya imza bilgilerini açmaz. WhatsApp hesabına giriş yapmaz ve sohbetlere erişmez.

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

## 1.0.2: doğrudan GIF kaydetme

Dosya açıldığında görünen **GIF olarak kaydet** ve **Video (MP4) olarak kaydet** düğmeleri eklendi. Biçim menüsünde seçim yapmaya gerek yoktur. Aynı düğmeler düzenleme seçeneklerinin altında da yer alır; seçili kesme, kırpma ve diğer ayarlar uygulanır.

## 1.1.0: WhatsApp çıkartmaları

Hareketli WebP oluşturma, otomatik boyut optimizasyonu, kalıcı çıkartma paketleri ve WhatsApp/WhatsApp Business’a kullanıcı onayıyla paket ekleme desteği eklendi. GIF ve MP4 kaydı korunur.

## 1.1.1: çıkartma oluşturma hızı

Ağır sıkıştırma kaldırıldı, gereksiz ölçekleme ve kare işleme azaltıldı. Dosya boyutuna göre en fazla üç dönüştürme denemesi yapılır. Son çıkartma, aynı kaynak ve ayarlarda tekrar kullanılabilir. Hız için kalite/kare hızı otomatik ayarlanır; 512×512, hareketli WebP, 10 saniye ve 500 KB koşulları korunur.

Aynı emülatör ve kaynaklarla yapılan karşılaştırmada 2 saniyelik GIF 7.193 ms → 202 ms, 10 saniyelik çıktı 91.620 ms → 478 ms oldu. Bunlar test örneklerinin ölçümleridir; dosya karmaşıklığı ve telefona göre süre değişir. Ayrıntılar: [doğrulama kaydı](docs/DOGRULAMA.md).
