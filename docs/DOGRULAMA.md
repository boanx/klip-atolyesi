# 1.0.0 doğrulaması

20 Kasım 2026 · Android 15 / API 35 x86_64 emülatörü.

Özel release anahtarıyla imzalanan, dağıtımdaki APK test edilmiştir.

- 9 dönüşüm modeli birim testi: başarılı.
- Android Lint release denetimi: 0 hata (uyarılar mevcut).
- Gerçek cihaz ikilileriyle FFmpeg 7.1.1: sesli 4 saniyelik H.264/AAC örneği üretildi.
- Dosya içe aktarma, süre ve boyut okuma: başarılı.
- Kırpma ve 90° döndürme: beklenen 240×160 çıktı.
- 0,5–2,5 saniye aralığını 2× hızla kaydetme: yaklaşık 1 saniye çıktı; ses korundu.
- Video → GIF: 240×240, hareketli ve palet optimizasyonlu çıktı.
- GIF → MP4: başarılı.
- Sessiz çıktı: ses akışı bulunmadığı doğrulandı.
- Android MediaStore galeriye yazma: başarılı.
- Gerçek, herkese açık X gönderisinden 15,557 saniyelik medya indirme: başarılı.
- APK imzası: Android APK Signature Scheme v2 doğrulaması başarılı; 3072 bit RSA.

Fiziksel Android telefonda ve tüm üretici/Android sürümü kombinasyonlarında test edilmemiştir. X erişim davranışı gönderiye, ağa ve platform değişikliklerine göre farklılaşabilir.

## 1.0.1 regresyon denetimleri

- Bildirilen `s == null` hatası 1.0.0'da aynı gönderiyle tekrarlandı.
- Boş Android meta verisi, eksik süre, format/akış süresi yedeği, kapak görseli ve döndürme bilgisi için 6 yeni birim testi eklendi; toplam 15 birim testi başarılı.
- FFprobe ikilisiyle dosya bilgileri okundu; bildirilen gönderi indirme → MP4 → GIF akışıyla doğrulandı.
- Aynı release imzasıyla 1.0.0 üzerine 1.0.1 kuruldu.

## 1.0.2: kaydetme düğmeleri

- GIF ve MP4 için ayrı kaydetme düğmeleri dosya açılınca görünür.
- Arayüzdeki GIF düğmesine basılarak hareketli GIF üretildiği ve galeriye yazıldığı doğrulandı.
- Arayüzdeki MP4 düğmesine basılarak video kaydı doğrulandı.
- Mevcut kesme, kırpma, hız, ses ve dönüştürme testleri başarılı.
- 15 birim testi ve Android Lint denetimi başarılı.

## 1.1.0: hareketli WhatsApp çıkartmaları

24 Kasım 2026 · Android 15 emülatörü.

- 21 birim testi başarılı; Android Lint release denetiminde hata yok.
- Arayüzdeki çıkartma düğmesiyle GIF'ten üç kalıcı çıkartma oluşturuldu.
- Üretilen WebP 512×512, çok kareli, döngülü ve 500 KB sınırının altında.
- Android ImageDecoder, dosyayı AnimatedImageDrawable olarak açtı.
- Şeffaf kenarlar piksel düzeyinde doğrulandı.
- 10 saniyelik örnek otomatik kalite ayarıyla 10.000 ms ve 499.992 bayt olarak üretildi; süre ve 500 KB koşulları birlikte sağlandı.
- WhatsApp metadata, çıkartma listesi ve dosya sağlayıcı protokolleri doğrulandı.
- Paket dışındaki özel dosyaların açılması engellendi; dış istemcinin WhatsApp okuma izni olmadan sorgusu reddedildi.
- WhatsApp ve WhatsApp Business için resmi paket ekleme intent'i kullanılır.
- Emülatörde WhatsApp kurulu değildir; WhatsApp içindeki son ekleme/onay ekranı burada uçtan uca test edilmemiştir.

## 1.1.1: süre karşılaştırması

24 Kasım 2026 · Aynı Android 15 x86_64 emülatörü, aynı üretilmiş örnek dosyalar. İlk dönüştürmeler ölçüldü; önbellek kazancı ayrıca ölçüldü. Eski ayarlar testte 1.1.0 kodundaki altı profil ve sıkıştırma seviyesi 6 kullanılarak yeniden çalıştırıldı. Yeni yol hızlı kodlama ve süreye göre ilk kalite/kare hızı ayarını kullanır; bu kıyas eşit görüntü kalitesi garantisi vermez.

| Örnek | Eski süre | Yeni süre | Eski/yeni deneme | Yeni dosya |
|---|---:|---:|---:|---:|
| 2 saniyelik GIF | 7.193 ms | 202 ms | 1 / 1 | 152.764 bayt |
| 10 saniyelik çıktı | 91.620 ms | 478 ms | 3 / 1 | 461.130 bayt |

- İki çıktı da Android tarafından hareketli WebP olarak açıldı; 512×512, en fazla 10 saniye ve 500 KB doğrulamalarından geçti.
- Aynı kaynak ve ayarlar: 1 ve 3 ms içinde önbellekten bayt bayt aynı dosya alındı.
- Seçilen süre değiştiğinde önbelleğin kullanılmadığı ve yeni dosyanın geçerli olduğu doğrulandı.
- Karşılaştırma komutu: `adb shell am instrument -w -e benchmarkStickers true com.klipatolyesi.app.test/com.klipatolyesi.app.EngineInstrumentation`.
- Bu iki test örneğinin sonuçları tüm dosyalara/telefonlara genellenemez. Daha hızlı profil kalite ve kare hızı bakımından farklı sonuçlar verebilir.
- Son release APK'sında paket kaydı/sağlayıcı, şeffaf kenarlar, MP4/GIF dönüşümü ve 10 saniye çıkartma sınırı regresyonları geçti. 23 birim testi başarılı, Android Lint release denetiminde hata yok.
