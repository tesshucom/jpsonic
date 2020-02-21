package com.tesshu.jpsonic.service;

import com.tesshu.jpsonic.domain.JapaneseReadingUtils;
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
@DependsOn({ "japaneseReadingUtils", "jpsonicComparators" })
public class MediaFileServiceUtils {

    private final JapaneseReadingUtils utils;

    private final JpsonicComparators jpsonicComparator;

    public MediaFileServiceUtils(JapaneseReadingUtils utils, JpsonicComparators jpsonicComparator) {
        super();
        this.utils = utils;
        this.jpsonicComparator = jpsonicComparator;
    }

    /**
     * Compensate for missing properties when initial creation of MediaFile
     * (when performing meta-analysis).
     * 
     * @param m
     */
    public void analyze(MediaFile m) {
        utils.analyze(m);
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
