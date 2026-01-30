# 💰 Enoconomy

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen?style=for-the-badge&logo=minecraft)
![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk)
![Paper](https://img.shields.io/badge/Paper-API-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

**Minecraft sunucunuz için modern, güçlü ve kullanımı kolay ekonomi eklentisi**

[📦 İndir](#kurulum) • [📖 Komutlar](#komutlar) • [🌐 Web Panel](#web-panel) • [⚙️ Ayarlar](#konfigürasyon) • [🔌 API](#api-kullanımı)

</div>

---

## ✨ Özellikler

- 🏦 **Tam Ekonomi Sistemi** - Para transferi, bakiye yönetimi ve daha fazlası
- 🌐 **Modern Web Panel** - Tarayıcıdan sunucu ekonomisini yönetin
- 📊 **Gerçek Zamanlı İstatistikler** - Toplam para, oyuncu sayısı, işlem geçmişi
- 🏆 **Sıralama Sistemi** - En zengin oyuncuları listeleyin
- 🔧 **Kolay Yapılandırma** - Web panelinden veya config.yml'den ayarlayın
- 🔌 **Geliştirici API** - Diğer eklentilerle entegrasyon
- 💾 **SQLite/MySQL Desteği** - Esnek veritabanı seçenekleri
- 🎨 **PlaceholderAPI Desteği** - Scoreboard ve hologram entegrasyonu

---

## 📋 Gereksinimler

| Gereksinim | Versiyon |
|------------|----------|
| Minecraft Server | Paper 1.21.x |
| Java | 17 veya üzeri |
| RAM | Minimum 512MB |

---

## 📦 Kurulum

1. [Releases](https://github.com/EnoBaco021/enoconomy/releases) sayfasından en son JAR dosyasını indirin
2. JAR dosyasını sunucunuzun `plugins` klasörüne koyun
3. Sunucuyu yeniden başlatın
4. `plugins/Enoconomy/config.yml` dosyasından ayarları yapılandırın

---

## 🎮 Komutlar

### Oyuncu Komutları

| Komut | Açıklama | İzin |
|-------|----------|------|
| `/para`, `/bakiye`, `/money` | Bakiyenizi görüntüler | `enoconomy.balance` |
| `/para <oyuncu>` | Başka oyuncunun bakiyesini görüntüler | `enoconomy.balance.others` |
| `/gönder <oyuncu> <miktar>` | Para transfer eder | `enoconomy.pay` |
| `/pay <oyuncu> <miktar>` | Para transfer eder | `enoconomy.pay` |
| `/sıralama`, `/baltop` | En zengin oyuncuları listeler | `enoconomy.baltop` |

### Admin Komutları

| Komut | Açıklama | İzin |
|-------|----------|------|
| `/enoconomy give <oyuncu> <miktar>` | Oyuncuya para verir | `enoconomy.admin` |
| `/enoconomy take <oyuncu> <miktar>` | Oyuncudan para alır | `enoconomy.admin` |
| `/enoconomy set <oyuncu> <miktar>` | Bakiye ayarlar | `enoconomy.admin` |
| `/enoconomy reset <oyuncu>` | Bakiyeyi sıfırlar | `enoconomy.admin` |
| `/enoconomy stats` | Sunucu istatistikleri | `enoconomy.admin` |
| `/enoconomy reload` | Ayarları yeniden yükler | `enoconomy.admin` |
| `/enoconomy webadmin create <user> <pass>` | Web admin oluşturur | `enoconomy.admin` |
| `/enoconomy webpanel` | Web panel bilgisi | `enoconomy.admin` |

---

## 🌐 Web Panel

Enoconomy, tarayıcıdan erişilebilen modern bir web panel ile gelir.

### Web Panel Özellikleri

- 📊 **Dashboard** - Gerçek zamanlı sunucu istatistikleri
- 👥 **Oyuncu Yönetimi** - Oyuncu bakiyelerini görüntüle ve düzenle
- 📜 **İşlem Geçmişi** - Tüm ekonomi işlemlerini izle
- 🏆 **Sıralama** - En zengin oyuncular listesi
- ⚙️ **Ayarlar** - Ekonomi ayarlarını web üzerinden değiştir

### Web Panel Kurulumu

1. Oyun içinde admin oluşturun:
```
/enoconomy webadmin create admin şifre123
```

2. Tarayıcınızda açın:
```
http://localhost:3000
```

3. Oluşturduğunuz kullanıcı adı ve şifre ile giriş yapın

### Ekran Görüntüleri

<details>
<summary>📸 Web Panel Ekran Görüntüleri</summary>

**Dashboard**
- Toplam oyuncu, para ve işlem istatistikleri
- Son işlemler listesi
- En zengin oyuncular

**Oyuncu Yönetimi**
- Oyuncu arama
- Bakiye düzenleme
- İşlem geçmişi görüntüleme

**Ayarlar**
- Para birimi sembolü
- Başlangıç bakiyesi
- Transfer vergisi
- Ve daha fazlası...

</details>

---

## ⚙️ Konfigürasyon

### config.yml

```yaml
# Veritabanı ayarları
database:
  type: sqlite  # sqlite veya mysql
  mysql:
    host: localhost
    port: 3306
    database: enoconomy
    username: root
    password: ""

# Ekonomi ayarları
economy:
  starting-balance: 100.0      # Başlangıç bakiyesi
  max-balance: 1000000000.0    # Maksimum bakiye
  currency-symbol: "$"          # Para birimi sembolü
  currency-name: "Coin"         # Para birimi adı
  transfer-tax: 0.0             # Transfer vergisi (%)
  min-transfer: 1.0             # Minimum transfer miktarı

# Web panel ayarları
web-panel:
  enabled: true
  port: 3000
```

---

## 🔌 API Kullanımı

Diğer eklentilerinizde Enoconomy API'sini kullanabilirsiniz:

```java
// API'yi alın
EconomyAPI api = Enoconomy.getAPI();

// Bakiye işlemleri
double balance = api.getBalance(player);
api.deposit(player, 100.0);
api.withdraw(player, 50.0);
api.setBalance(player, 1000.0);

// Kontroller
boolean hasEnough = api.hasEnough(player, 100.0);
boolean hasAccount = api.hasAccount(player);

// Para formatı
String formatted = api.formatMoney(1234.56); // "$1,234.56"
```

### Maven Dependency

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.EnoBaco021</groupId>
    <artifactId>enoconomy</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

---

## 📊 PlaceholderAPI

| Placeholder | Açıklama |
|-------------|----------|
| `%enoconomy_balance%` | Oyuncunun bakiyesi |
| `%enoconomy_balance_formatted%` | Formatlanmış bakiye |
| `%enoconomy_currency_symbol%` | Para birimi sembolü |
| `%enoconomy_currency_name%` | Para birimi adı |
| `%enoconomy_top_name_<sıra>%` | Sıralamadaki oyuncu adı |
| `%enoconomy_top_balance_<sıra>%` | Sıralamadaki oyuncu bakiyesi |

---

## 🛠️ Derleme

Projeyi kendiniz derlemek için:

```bash
git clone https://github.com/EnoBaco021/enoconomy.git
cd enoconomy
mvn clean package -DskipTests
```

JAR dosyası `target/enoconomy-1.0-SNAPSHOT.jar` konumunda oluşturulur.

---

## 📝 Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

---

## 🤝 Katkıda Bulunma

Katkılarınızı bekliyoruz! Lütfen bir Pull Request göndermeden önce:

1. Projeyi fork edin
2. Feature branch oluşturun (`git checkout -b feature/yeniözellik`)
3. Değişikliklerinizi commit edin (`git commit -m 'Yeni özellik eklendi'`)
4. Branch'inizi push edin (`git push origin feature/yeniözellik`)
5. Pull Request açın

---

## 📞 İletişim & Destek

- **GitHub:** [@EnoBaco021](https://github.com/EnoBaco021)
- **Issues:** [GitHub Issues](https://github.com/EnoBaco021/enoconomy/issues)

---

<div align="center">

**⭐ Bu projeyi beğendiyseniz yıldız vermeyi unutmayın! ⭐**

Made with ❤️ by [EnoBaco021](https://github.com/EnoBaco021)

</div>

