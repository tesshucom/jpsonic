FROM eclipse-temurin:21-jdk-jammy

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

RUN apt-get update && apt-get install -y --no-install-recommends \
    software-properties-common \
    gosu \
    tini \
    fonts-noto-cjk \
    ffmpeg \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && rm /usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc \
    && rm /usr/share/fonts/opentype/noto/NotoSerifCJK-Bold.ttc \
    && rm /usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc \
    && fc-cache -vf

EXPOSE $JPSONIC_PORT
EXPOSE $UPNP_PORT
EXPOSE 1900/udp
EXPOSE 3333

VOLUME \
    $JPSONIC_DIR/data \
    $JPSONIC_DIR/music \
    $JPSONIC_DIR/playlists \
    $JPSONIC_DIR/podcasts

HEALTHCHECK --interval=30s --timeout=2s CMD export HEALTHCHECK_QUERY=$(echo localhost:"$JPSONIC_PORT""$CONTEXT_PATH"/rest/ping | tr -s /);wget -q http://"$HEALTHCHECK_QUERY" -O /dev/null || exit 1

COPY entry-point.sh /usr/local/bin/entry-point.sh
RUN chmod +x /usr/local/bin/entry-point.sh

COPY target/dependency/jpsonic.war jpsonic.war
RUN java -Djarmode=tools -jar jpsonic.war extract --launcher --destination app \
    && rm jpsonic.war \
    && cd app \
    && java -Dspring.context.exit=onRefresh -XX:ArchiveClassesAtExit=../jpsonic.jsa org.springframework.boot.loader.launch.WarLauncher

ENTRYPOINT ["tini", "-e", "143", "--", "entry-point.sh"]
