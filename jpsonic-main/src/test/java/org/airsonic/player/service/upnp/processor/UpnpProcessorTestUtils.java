
package org.airsonic.player.service.upnp.processor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.tesshu.jpsonic.domain.JpsonicComparatorsIntegrationTest;
import com.tesshu.jpsonic.domain.JpsonicComparatorsTest;

/**
 * Class that defines the sort order corresponding to test data for Upnp service.
 * 
 * The same has been verified in {@link JpsonicComparatorsIntegrationTest}. This package mainly uses
 * resources/MEDIAS/Sort/Pagination.
 * 
 * UPnP validation mainly focuses on whether pagination is broken by improper implementation of sorting. Strict sorting
 * validation can be done with {@link JpsonicComparatorsTest}.
 * 
 * Some famous UPnP client applications have a default pagination size of 30. (It may be possible depending on the
 * setting, but it may be silent) Therefore, it is desirable that UPnP verification has 30 or more hierarchy elements.
 * By scanning under resources/MEDIAS/Sort/Pagination in the real environment, it is also possible to confirm with the
 * actual client.
 */
class UpnpProcessorTestUtils {

    static final List<String> indexList = Collections.unmodifiableList(Arrays.asList("abcde", "ＢＣＤＥＡ", "ĆḊÉÁḂ", "DEABC",
            "eabcd", "亜伊鵜絵尾", "αβγ", "いうえおあ", "ｴｵｱｲｳ", "オアイウエ", "春夏秋冬", "貼られる", "パラレル", "馬力", "張り切る", "はるなつあきふゆ", "10",
            "20", "30", "40", "50", "60", "70", "80", "90", "98", "99", "ゥェォァィ", "ｪｫｧｨｩ", "ぉぁぃぅぇ", "♂くんつ"));

    static final List<String> jPSonicNaturalList = Collections
            .unmodifiableList(Arrays.asList("10", "20", "30", "40", "50", "60", "70", "80", "90", "98", "99", "abcde",
                    "ＢＣＤＥＡ", "ĆḊÉÁḂ", "DEABC", "eabcd", "亜伊鵜絵尾", "αβγ", "いうえおあ", "ゥェォァィ", "ｴｵｱｲｳ", "ｪｫｧｨｩ", "ぉぁぃぅぇ",
                    "オアイウエ", "春夏秋冬", "貼られる", "パラレル", "馬力", "張り切る", "はるなつあきふゆ", "♂くんつ"));

    static final List<String> childrenList = Collections
            .unmodifiableList(Arrays.asList("empty30", "empty29", "empty28", "empty27", "empty26", "empty25", "empty24",
                    "empty23", "empty22", "empty21", "empty20", "empty19", "empty18", "empty17", "empty16", "empty15",
                    "empty14", "empty13", "empty12", "empty11", "empty10", "empty09", "empty08", "empty07", "empty06",
                    "empty05", "empty04", "empty03", "empty02", "empty01", "empty00"));

    static boolean validateJPSonicNaturalList(List<String> l) {
        return jPSonicNaturalList.equals(l);
    }
}
