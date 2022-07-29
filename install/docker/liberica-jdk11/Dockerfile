FROM bellsoft/liberica-openjdk-alpine:11

LABEL description="Jpsonic is a free, web-based media streamer, providing ubiquitious access to your music." \
      url="https://github.com/jpsonic/jpsonic"

ENV JPSONIC_DIR=/jpsonic \
    CONTEXT_PATH=/ \
    JPSONIC_PORT=4040 \
    UPNP_PORT=4041 \
    SHOW_DATA_DIR=true \
    USER_ID=1000 \
    GROUP_ID=1000 \
    LANG=en_US.UTF-8 \
    TIME_ZONE=GB \
    BANNER_MODE=on \
    JAVA_OPTS=-Xmx256m

WORKDIR $JPSONIC_DIR

RUN apk --no-cache add \
    su-exec \ 
    alpine-conf \
    tzdata \
    ffmpeg \
    bash \
    fontconfig \
    font-noto-cjk \
    tini \
    curl \
    && rm -rf /var/cache/apk/* \
    && rm /usr/share/fonts/noto/NotoSansCJK-Bold.ttc \
    && rm /usr/share/fonts/noto/NotoSerifCJK-Bold.ttc \
    && rm /usr/share/fonts/noto/NotoSerifCJK-Regular.ttc

COPY entry-point.sh /usr/local/bin/entry-point.sh
RUN chmod +x /usr/local/bin/entry-point.sh
COPY target/dependency/jpsonic.war jpsonic.war

EXPOSE $JPSONIC_PORT
EXPOSE $UPNP_PORT
EXPOSE 1900/udp

VOLUME \
    $JPSONIC_DIR/data  \
    $JPSONIC_DIR/music  \
    $JPSONIC_DIR/playlists  \
    $JPSONIC_DIR/podcasts

HEALTHCHECK --interval=30s --timeout=2s CMD wget -q http://localhost:"$JPSONIC_PORT""$CONTEXT_PATH"rest/ping -O /dev/null || exit 1

ENTRYPOINT ["tini", "-e", "143", "--", "entry-point.sh"]
