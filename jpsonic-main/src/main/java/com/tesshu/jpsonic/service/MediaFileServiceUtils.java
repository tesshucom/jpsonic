package com.tesshu.jpsonic.service;

import com.tesshu.jpsonic.domain.JpsonicComparators;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MediaFileComparator;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Utility class for injecting into legacy MediaFileService.
 * Supplement processing that is lacking in legacy services.
 */
@Component
@DependsOn({ "mediaFileJPSupport", "jpsonicComparators" })
public class MediaFileServiceUtils {

    private final MediaFileJPSupport mediaFileJPSupport;

    private final JpsonicComparators jpsonicComparator;

    public MediaFileServiceUtils(MediaFileJPSupport mediaFileJPSupport, JpsonicComparators jpsonicComparator) {
        super();
        this.mediaFileJPSupport = mediaFileJPSupport;
        this.jpsonicComparator = jpsonicComparator;
    }

    /**
     * Compensate for missing properties when initial creation of MediaFile
     * (when performing meta-analysis).
     * 
     * @param m
     */
    public void analyze(MediaFile m) {
        mediaFileJPSupport.analyze(m);
    }

    /**
     * Returns the sorting rules for the child elements of the specified MediaFile.
     * MediaFile's sorting rules depend on the hierarchy to which it belongs.
     *
     * @param parent
     * @return
     */
    public MediaFileComparator mediaFileOrder(MediaFile parent) {
        return jpsonicComparator.mediaFileOrder(parent);
    }

}
