FROM eclipse-temurin:17-jdk-jammy

LABEL description="Jpsonic is a free, web-based media streamer, providing ubiquitious access to your music." \
      url="https://github.com/jpsonic/jpsonic"

ENV JPSONIC_DIR=/jpsonic \
    CONTEXT_PATH=/ \
    JPSONIC_PORT=4040 \
    UPNP_PORT=4041 \
    SHOW_DATA_DIR=true \
    USER_ID=1000 \
    GROUP_ID=1000 \
    LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8 \
    TIME_ZONE=GB \
    BANNER_MODE=CONSOLE \
    SCAN_ON_BOOT=false \
    EMBEDDED_FONT=false \
    MIME_DSF=audio/x-dsd \
    MIME_DFF=audio/x-dsd \
    JAVA_OPTS=-Xmx512m

WORKDIR $JPSONIC_DIR

RUN apt-get update && apt-get install -y \
    software-properties-common \
    gosu \
    tini \
    fonts-noto-cjk \
    ffmpeg \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && rm /usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc \
    && rm /usr/share/fonts/opentype/noto/NotoSerifCJK-Bold.ttc \
    && rm /usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc

EXPOSE $JPSONIC_PORT
EXPOSE $UPNP_PORT
EXPOSE 1900/udp
EXPOSE 3333

VOLUME \
    $JPSONIC_DIR/data \
    $JPSONIC_DIR/music \
    $JPSONIC_DIR/playlists \
    $JPSONIC_DIR/podcasts

HEALTHCHECK --interval=30s --timeout=2s CMD wget -q http://localhost:"$JPSONIC_PORT""$CONTEXT_PATH"rest/ping -O /dev/null || exit 1

COPY entry-point.sh /usr/local/bin/entry-point.sh
RUN chmod +x /usr/local/bin/entry-point.sh
COPY target/dependency/jpsonic.war jpsonic.war

ENTRYPOINT ["tini", "-e", "143", "--", "entry-point.sh"]
