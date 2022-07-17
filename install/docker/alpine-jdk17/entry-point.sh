#!/bin/bash

set -e

mkdir -p "$JPSONIC_DIR"/data/transcode
ln -fs /usr/bin/ffmpeg "$JPSONIC_DIR"/data/transcode/ffmpeg
ln -fs /usr/bin/lame "$JPSONIC_DIR"/data/transcode/lame

if [[ $# -lt 1 ]] || [[ ! "$1" == "java"* ]]; then

    java_opts_array=()
    while IFS= read -r -d '' item; do
        java_opts_array+=( "$item" )
    done < <([[ $JAVA_OPTS ]] && xargs printf '%s\0' <<<"$JAVA_OPTS")
    exec java -Xmx"${JVM_HEAP}" \
     -Dserver.host=0.0.0.0 \
     -Dserver.port="$JPSONIC_PORT" \
     -Dserver.contextPath="$CONTEXT_PATH" \
     -Djpsonic.home="$JPSONIC_DIR"/data \
     -Djpsonic.defaultMusicFolder="$JPSONIC_DIR"/music \
     -Djpsonic.defaultPodcastFolder="$JPSONIC_DIR"/podcasts \
     -Djpsonic.defaultPlaylistFolder="$JPSONIC_DIR"/playlists \
     -DUPNP_PORT="$UPNP_PORT" \
     -Djava.awt.headless=true \
     "${java_opts_array[@]}" \
     -jar jpsonic.war "$@"
fi

exec "$@"
