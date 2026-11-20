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
