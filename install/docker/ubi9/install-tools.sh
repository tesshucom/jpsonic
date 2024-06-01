#!/bin/bash
# @param TARGET_ARCH amd64 or arm64

TARGET_ARCH="$@"

# Install noto-cjk
mkdir -p /opt/noto
cd /opt/noto

wget https://github.com/googlefonts/noto-cjk/releases/download/Sans2.004/03_NotoSansCJK-OTC.zip
unzip 03_NotoSansCJK-OTC.zip
rm NotoSansCJK-Black.ttc
rm NotoSansCJK-Bold.ttc
rm NotoSansCJK-DemiLight.ttc
rm NotoSansCJK-Light.ttc
rm NotoSansCJK-Medium.ttc
mkdir -p /usr/share/fonts/noto
ln -sf "${PWD}/NotoSansCJK-Regular.ttc" /usr/share/fonts/noto/NotoSansCJK-Regular.ttc
rm NotoSansCJK-Thin.ttc
rm 03_NotoSansCJK-OTC.zip
fc-cache -vf

# Install tini
mkdir -p /opt/tini
cd /opt/tini

wget "https://github.com/krallin/tini/releases/download/v0.19.0/tini-${TARGET_ARCH}"
wget "https://github.com/krallin/tini/releases/download/v0.19.0/tini-${TARGET_ARCH}.sha1sum"
wget https://raw.githubusercontent.com/krallin/tini/master/LICENSE
sha1sum --check tini-${TARGET_ARCH}.sha1sum
chmod +x tini-${TARGET_ARCH}
ln -sf "${PWD}/tini-${TARGET_ARCH}" /sbin/tini
rm -f tini-${TARGET_ARCH}.sha1sum

# Install ffmpeg
mkdir -p /opt/ffmpeg
cd /opt/ffmpeg

wget "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-${TARGET_ARCH}-static.tar.xz"
wget "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-${TARGET_ARCH}-static.tar.xz.md5"
md5sum -c "ffmpeg-release-${TARGET_ARCH}-static.tar.xz.md5"
tar xvf "ffmpeg-release-${TARGET_ARCH}-static.tar.xz"
cd ffmpeg-*-static
ln -sf "${PWD}/ffmpeg" /usr/bin/ffmpeg
ln -sf "${PWD}/ffprobe" /usr/bin/ffprobe
cd ../
rm -f ffmpeg-release-${TARGET_ARCH}-static.tar.xz ffmpeg-release-${TARGET_ARCH}-static.tar.xz.md5
