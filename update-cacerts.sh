#!/bin/bash -ex
curl --etag-compare cacert.etag --etag-save cacert.etag --remote-name https://curl.se/ca/cacert.pem
java tools/SplitCACerts.java
rm src/main/resources/diy/y2k/unsup/assets/cacerts.zip.br
brotli -Zj src/main/resources/diy/y2k/unsup/assets/cacerts.zip
