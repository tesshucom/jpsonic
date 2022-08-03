#!/bin/bash

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

su-exec "$username":"$groupname" mkdir -p "$JPSONIC_DIR"/data/transcode
rm -f "$JPSONIC_DIR"/data/transcode/ffmpeg
su-exec "$username":"$groupname" ln -fs /usr/bin/ffmpeg "$JPSONIC_DIR"/data/transcode/ffmpeg
rm -f "$JPSONIC_DIR"/data/transcode/ffprobe
su-exec "$username":"$groupname" ln -fs /usr/bin/ffprobe "$JPSONIC_DIR"/data/transcode/ffprobe

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
     -Djpsonic.scan.onboot="$SCAN_ON_BOOT" \
     -Djpsonic.embeddedfont="$EMBEDDED_FONT" \
     -Djpsonic.mime.dsf="$MIME_DSF" \
     -Djpsonic.mime.dff="$MIME_DFF" \
     -DUPNP_PORT="$UPNP_PORT" \
     -Dspring.main.banner-mode="$BANNER_MODE" \
     -Djava.awt.headless=true \
     -Duser.timezone="$TIME_ZONE" \
     "${java_opts_array[@]}" \
     -jar jpsonic.war "$@"
fi

exec "$@"
