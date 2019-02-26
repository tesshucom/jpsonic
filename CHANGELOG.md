<!--
# CHANGELOG.md
# jpsonic/jpsonic
# -->

## v2.3.0

  * Security update (stax:CVE-2018-1000840)
  * Fix for embedded Jetty compilation for evaluation purposes.
  * Based on airsonic e4bb808 (2019-2) Pull translations from transifex.

## v2.2.6

  * Security update (jackson-databind:CVE-2018-19360 - CVE-2018-19362, CVE-2018-14718 - CVE-2018-14721)
  * Based on airsonic adc2241 (2019-1) Fix broken keyboard shortcuts, defrag on HSQLDB, connection pooling for external database etc.

## v2.2.5

  * Security update (guava:CVE-2018-10237)
  * Suppress CVE by false positives(stax:CVE-2017-16224, slf4j:CVE-2018-8088)
  * Localization of version check. Changed Jpsonic update to notify management screen
  * Based on airsonic 77ca475 (2018-12) Screen modification, updating of various libraries, modification of test content, etc.
   - Modification of partial wording accompanying cleanup of overall translation
   - Image replacement related to adding icons for various devices

## v2.2.4

  * Fixed a bug where part of the start argument was not correctly recognized
	(jpsonic.defaultMusicFolder, jpsonic.defaultPodcastFolder, jpsonic.defaultPlaylistFolder)
  * Introduction of Airsonic integration test using Docker

## v2.2.3

  * Security update for cxf(CVE-2018-8039)
  * Based on airsonic 685f4fa (2018-10)

## v2.2.2

  * Improvement of Japanese Song search accuracy.
  * Random search fault correction.
  * Based on airsonic 8ba0bc8 (2018-8)

## v2.2.1

  * Security fix (LDAP authentication without a password).
  * Based upon Airsonic 10.2.0-SNAPSHOT f6905de(2018-8)
  * Start build test with travis.

## v2.2

  * Forward search reinforcement of artist name. Corresponds to full name, hiragana, katakana.
  * Added index rebuilding process after scanning.
  * Based upon Airsonic 10.2.0-SNAPSHOT 8d3c0ec(2018-7)

## v2.1

  * Update of lucene-core(3.0.3 -> 7.4.0).
  * Simple Japanese phrase search.

## v2.0

  * Based upon Airsonic 10.2.0-SNAPSHOT 83ef76a(2018-7)

## v2.0

  * Based upon Airsonic 10.2.0-SNAPSHOT 83ef76a(2018-7)

## v1.3

  * It corresponds to ALBUM_SORT
  * Final release based upon Airsonic 10.1.1-RELEASE

## v1.2.2

  * It corresponds to ARTIST_SORT, ALBUM_ARTIST_SORT
  * Fixed a bug that caused case ignoring excessively. (Alphabet is originally A-Za-z)

## v1.2.1

  * Fixed bug related to sort of id 3

## v1.2

  * Supports sorting using not only morphological analysis but also tag analysis

## v1.1

  * Japanese index / Artist sort (id3)
  * Duplicate records may be included in getAlbunList Fixed a problem
  * Change DLNA icon

## v1.0

  * Japanese index / Artist sort (File structure)
  * Fixed bug in Lang of biography
  * Default Japanese
  * First release as Jpsonic
  * Based upon Airsonic 10.1.1-RELEASE
