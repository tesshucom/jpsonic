FROM alpine:latest

LABEL description="Jpsonic is a free, web-based media streamer, providing ubiquitious access to your music." \
      url="https://github.com/jpsonic/jpsonic"

ENV JPSONIC_DIR=/jpsonic \
    CONTEXT_PATH=/ \
    JPSONIC_PORT=4040 \
    UPNP_PORT=4041 \
    SHOW_DATA_DIR=true \
    USER_ID=1000 \
    GROUP_ID=1000 \
    LANG=C \
    TIME_ZONE=GB \
    BANNER_MODE=on \
    JAVA_OPTS=-Xmx512m

WORKDIR $JPSONIC_DIR

RUN apk --no-cache add \
    su-exec \ 
    alpine-conf \
    tzdata \
    musl-locales \
    musl-locales-lang \
    ffmpeg \
    bash \
    libressl \
    fontconfig \
    ttf-dejavu \
    ca-certificates \
    tini \
    curl \
    openjdk17-jdk

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
