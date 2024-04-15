FROM eclipse-temurin:21-jdk-alpine

LABEL description="Jpsonic is a free, web-based media streamer, providing ubiquitious access to your music." \
      url="https://github.com/jpsonic/jpsonic"

ENV JPSONIC_DIR=/jpsonic \
    CONTEXT_PATH=/ \
    JPSONIC_PORT=4040 \
    UPNP_PORT=4041 \
    UPNP_IDLE_INTERVAL=30 \
    UPNP_MAX_CONNECTIONS=50 \
    UPNP_MAX_IDLE_CONNECTIONS=30 \
    UPNP_DRAIN_AMOUNT=65536 \
    UPNP_MAX_REQ_HEADERS=200 \
    UPNP_MAX_REQ_TIME=500 \
    UPNP_MAX_RSP_TIME=500 \
    UPNP_NODELAY=true \
    SHOW_DATA_DIR=true \
    USER_ID=1000 \
    GROUP_ID=1000 \
    TIME_ZONE=GB \
    BANNER_MODE=OFF \
    LOG_LEVEL=WARN \
    SCAN_ON_BOOT=false \
    EMBEDDED_FONT=false \
    MIME_DSF=audio/x-dsd \
    MIME_DFF=audio/x-dsd \
    JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=500"

WORKDIR $JPSONIC_DIR

COPY local.conf /etc/fonts

RUN apk --no-cache add \
    su-exec \
    bash \
    tini \
    ffmpeg \
    font-noto-cjk \
    && rm -rf /var/cache/apk/* \
    && rm /usr/share/fonts/noto/NotoSansCJK-Bold.ttc \
    && rm /usr/share/fonts/noto/NotoSerifCJK-Bold.ttc \
    && rm /usr/share/fonts/noto/NotoSerifCJK-Regular.ttc \
    && fc-cache -vf \
    && rm -f /usr/lib/librav1e.so.0.6.6

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
