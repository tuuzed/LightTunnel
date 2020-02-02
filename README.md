# LightTunnel-内网穿透工具

支持TCP、HTTP、HTTPS穿透。

![Java CI](https://github.com/tuuzed/LightTunnel/workflows/Java%20CI/badge.svg)
![LightTunnel Version](https://img.shields.io/badge/LightTunnel-0.5.x-blue.svg)
![Thank Idea](https://img.shields.io/badge/Thank-jetbrains.com-green.svg)

## 0x01 SSL证书生成

- 生成服务端证书
```bash

keytool -genkey \
-alias ltsalias \
-keysize 2048 \
-validity 3650 \
-keyalg RSA \
-dname "CN=LTS" \
-keypass ltspass \
-storepass ltspass \
-keystore lts.jks

keytool -importkeystore \
-srckeystore lts.jks \
-destkeystore lts.jks \
-deststoretype pkcs12

keytool -export \
-alias ltsalias \
-keystore lts.jks \
-storepass ltspass \
-file lts.cer

```
- 生成客户端证书
```bash

keytool -genkey \
-alias ltcalias \
-keysize 2048 \
-validity 3650 \
-keyalg RSA \
-dname "CN=LTC" \
-keypass ltcpass \
-storepass ltcpass \
-keystore ltc.jks

keytool -importkeystore \
-srckeystore ltc.jks \
-destkeystore ltc.jks \
-deststoretype pkcs12

keytool -import -trustcacerts \
-alias ttsalias \
-file lts.cer \
-storepass ltcpass \
-keystore ltc.jks

```
