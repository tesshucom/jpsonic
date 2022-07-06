#!/bin/bash

export LANG="$LANG"
setup-timezone -z "$TIME_ZONE"

groupname=$(getent group "$GROUP_ID" | cut -d":" -f1)
if [ -z "$groupname" ]; then
    groupname=jpsonic
    addgroup -g "$GROUP_ID" -S $groupname
fi

username=$(getent passwd "$USER_ID" | cut -d":" -f1)
if [ -z "$username" ]; then
    username=jpsonic
	adduser \
	    -u "$USER_ID" \
	    --disabled-password \
	    --gecos "" \
	    --home "$JPSONIC_DIR" \
	    --ingroup "$groupname" \
	    jpsonic
fi

addgroup "$username" "$groupname"

if "$SHOW_DATA_DIR"; then
    ls -n "$JPSONIC_DIR"/data
fi

mkdir -p "$JPSONIC_DIR"/data/transcode
if [ ! -e "$JPSONIC_DIR"/data/transcode/ffmpeg ]; then
    ln -fs /usr/bin/ffmpeg "$JPSONIC_DIR"/data/transcode/ffmpeg
fi

if [[ $# -lt 1 ]] || [[ ! "$1" == "java"* ]]; then

    java_opts_array=()
    while IFS= read -r -d '' item; do
        java_opts_array+=( "$item" )
    done < <([[ $JAVA_OPTS ]] && xargs printf '%s\0' <<<"$JAVA_OPTS")

    # su-exec and sudo may obscure exit codes.
    # Use set to enable graceful shutdown.
    set -- su-exec "$username":"$groupname" java \
     -Dserver.host=0.0.0.0 \
     -Dserver.port="$JPSONIC_PORT" \
     -Dserver.contextPath="$CONTEXT_PATH" \
     -Djpsonic.home="$JPSONIC_DIR"/data \
     -Djpsonic.defaultMusicFolder="$JPSONIC_DIR"/music \
     -Djpsonic.defaultPodcastFolder="$JPSONIC_DIR"/podcasts \
     -Djpsonic.defaultPlaylistFolder="$JPSONIC_DIR"/playlists \
     -DUPNP_PORT="$UPNP_PORT" \
     -Dspring.main.banner-mode="$BANNER_MODE" \
     -Djava.awt.headless=true \
     "${java_opts_array[@]}" \
     -jar jpsonic.war "$@"
fi

exec "$@"
