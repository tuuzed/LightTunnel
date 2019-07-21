# Tunnel-内网映射工具

## SSL证书生成
- 生成服务端证书
```bash

keytool -genkey -alias stunnelalias -keysize 2048 -validity 365 -keyalg RSA -dname "CN=tunnel" -keypass stunnelpass -storepass stunnelpass -keystore tunnel-server.jks
keytool -importkeystore -srckeystore tunnel-server.jks -destkeystore tunnel-server.jks -deststoretype pkcs12
keytool -export -alias stunnelalias -keystore tunnel-server.jks -storepass stunnelpass -file tunnel-server.cer

```
- 生成客户端证书
```bash

keytool -genkey -alias ctunnelalias -keysize 2048 -validity 365 -keyalg RSA -dname "CN=tunnel" -keypass ctunnelpass -storepass ctunnelpass -keystore tunnel-client.jks
keytool -importkeystore -srckeystore tunnel-client.jks -destkeystore tunnel-client.jks -deststoretype pkcs12
keytool -import -trustcacerts -alias stunnalalias -file tunnel-server.cer -storepass ctunnelpass -keystore tunnel-client.jks

```
