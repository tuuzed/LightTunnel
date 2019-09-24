# TPS-内网穿透工具

支持TCP、HTTP、HTTPS穿透。

## 0x01 SSL证书生成

- 生成服务端证书
```bash

keytool -genkey \
-alias tpsalias \
-keysize 2048 \
-validity 3650 \
-keyalg RSA \
-dname "CN=TPS" \
-keypass tpspass \
-storepass tpspass \
-keystore tps.jks

keytool -importkeystore \
-srckeystore tps.jks \
-destkeystore tps.jks \
-deststoretype pkcs12

keytool -export \
-alias tpsalias \
-keystore tps.jks \
-storepass tpspass \
-file tps.cer

```
- 生成客户端证书
```bash

keytool -genkey \
-alias tpcalias \
-keysize 2048 \
-validity 3650 \
-keyalg RSA \
-dname "CN=TPC" \
-keypass tpcpass \
-storepass tpcpass \
-keystore tpc.jks

keytool -importkeystore \
-srckeystore tpc.jks \
-destkeystore tpc.jks \
-deststoretype pkcs12

keytool -import -trustcacerts \
-alias tpsalias \
-file tps.cer \
-storepass tpcpass \
-keystore tpc.jks

```
