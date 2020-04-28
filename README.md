# LightTunnel-内网穿透工具

支持TCP、HTTP、HTTPS穿透。

![Java CI](https://github.com/tuuzed/LightTunnel/workflows/Java%20CI/badge.svg)
![LightTunnel LaseVersion](https://img.shields.io/badge/LightTunnel-v0.5.24-blue.svg)
![Thank Idea](https://img.shields.io/badge/Thank-jetbrains.com-green.svg)

## 0x00 发行版下载

[Github下载 LightTunnel-v0.5.24.tar.gz](https://github.com/tuuzed/LightTunnel/releases/download/v0.5.24/LightTunnel-v0.5.24.tar.gz)

[Gitee下载 LightTunnel-v0.5.24.tar.gz](https://gitee.com/tuuzed/LightTunnel/attach_files/383444/download)

## 0x01 文件说明

- 服务器端文件
```
 lts.exe       - 服务器端windows可执行文件
 lts.sh        - 服务器端*nix可执行文件
 lts.jar       - 服务器端jvm可执行文件
 lts.ini       - 服务器端最小配置文件   
 lts_full.ini  - 服务器端完整配置文件   
 lts.jks       - 服务器端SSL证书
```
- 客户端文件
``` 
 ltc.exe       - 客户端windows可执行文件
 ltc.sh        - 客户端*nix可执行文件
 ltc.jar       - 客户端jvm可执行文件
 ltc.ini       - 客户端最小配置文件
 ltc_full.ini  - 客户端完整配置文件   
 ltc.jks       - 客户端SSL证书
```


## 0x02 启动命令

- windows系统
  
  - 服务器端
  ```shell script
  .\lts.exe -c lts.ini
  ```
  
  - 客户端
  ```shell script
  .\ltc.exe -c ltc.ini
  ```

    
- *nix系统

  - 服务器端
  ```shell script
  ./lts.sh -c lts.ini
  ```
  
  - 客户端
  ```shell script
  ./ltc.sh -c ltc.ini
  ```

- jvm

  - 服务器端
  ```shell script
  java -jar ltc.jar -c lts.ini
  ```
  
  - 客户端
  ```shell script
  java -jar ltc.sh -c ltc.ini
  ```
  
## 0x03 SSL证书生成

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
