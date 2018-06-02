FROM alpine:3.6

LABEL description="Jpsonic is a free, web-based media streamer, providing ubiquitious access to your music." \
      url="https://github.com/tesshucom/jpsonic"

ENV JPSONIC_PORT=4040 JPSONIC_DIR=/jpsonic CONTEXT_PATH=/

WORKDIR $JPSONIC_DIR

RUN apk --no-cache add \
    ffmpeg \
    lame \
    bash \
    libressl \
    fontconfig \
    ttf-dejavu \
    ca-certificates \
    tini \
    openjdk8-jre

COPY run.sh /usr/local/bin/run.sh

RUN chmod +x /usr/local/bin/run.sh

COPY target/dependency/jpsonic-main.war jpsonic.war

EXPOSE $JPSONIC_PORT

VOLUME $JPSONIC_DIR/data $JPSONIC_DIR/music $JPSONIC_DIR/playlists $JPSONIC_DIR/podcasts

ENTRYPOINT ["tini", "--", "run.sh"]
