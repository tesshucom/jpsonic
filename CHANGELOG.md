<!--
# CHANGELOG.md
# jpsonic/jpsonic
# -->

## v106.1.1
#### Based on *airsonic 10.5.0-RELEASE*

  * [fix] Update Tomcat to 8.5.50 (CVE-2019-12418, CVE-2019-17563 CVE).

Critical security fix.
The following measures taken.

 - Update Tomcat version to 8.5.50. The only version that addresses threats now.
 - Stop Tomcat precompiler. Because it depends on 8.5.40. As a result, the initial display of the web screen is slightly slower.
 - Jetty will continue to change to a compilable configuration. However,
   since it does not respond to threats, no official distribution will be made.
   It only supports arbitrary compilation.

## v106.1.0
#### Based on *airsonic 10.5.0-RELEASE*

  * [fix] Fixed server startup flow.
	This is fix for potential issue with Airsonic 10.5.0.
	The update to 106.1.0 disables automatic scan on first launch and removes previous search index data.
  * [fix] The UPnP search function has been improved and the previous search function has been removed.
	Performs query analysis according to Service Template Version 1.01 for UPnP Version 1.0.
	From 106.1.0, voice input is also possible from BubbleUPnP for DLNA.
  * [fix] Fixed the bug that UPnP cover art is not processed correctly.
	From 106.1.0, cover art of Artist(file/id3) / Album(file/id3) / Song / Playlist / Podcast can be displayed.
	(In the case of BubbleUPnP for DLNA. It depends on the specifications of the client application)
  * [fix] Fixed security check on cover art.
	Fixed meaningless SecurityException not to be output to the log.
  * [fix] Fixed to display multi genres correctly.
	From 106.1.0, if the genres are separated by semicolons, they will be displayed as different genres in the genre list.
  * [update] Added UPnP display items.
	Genre (song), shuffle (album), shuffle (song), and podcast are newly added.
  * [update] Added sorting option to genre master.
	Added option to display in dictionary order.

## v106.0.0
#### Based on *airsonic 10.5.0-RELEASE*

###### General

  * [fix] Update jackson to 2.10.1(CVE-2019-16943, CVE-2019-17531).
  * [fix] Update cxf to 3.3.4(CVE-2019-12406, CVE-2019-12419).
  * [update] Changed the default value of the setting item.
             The recommended items are now ON by default because so many options have been added.
  * [update] Jpsonic icons have been added to optional items, that include Jpsonic's unique functions and modifications.

###### WEB

  * [update] Modified the order of Home> All to be in the same order regardless of the DB being used.
  * [update] Added an option to include composers in the search, regardless of personal settings.
  * [update] Added an option to output the value entered in the log. Input from Web/Rest/DLNA can be confirmed.
  * [update] Added default user icon for Jpsonic theme 

###### DLNA

  * [update] Improved item deployment speed.
  * [update] Added DLNA display items (index/recently added album).
  * [update] Added option to select DLNA display items.
  * [update] Fixed the title search of DLNA to work correctly.
             DLNA title search can be selected as ID3/FileStructure (default is FileStructure and same search as Web) 
  * [update] Added an option to display the number of items in the genre 

## v105.2.1
#### Based on *airsonic 10.5.0-SNAPSHOT 06e36ff*

  * [fix] Fixed bug that fail when migrating with postgres from v105.1.0 to v105.2.0.

## v105.2.0
#### Based on *airsonic 10.5.0-SNAPSHOT 06e36ff*

> [06e36ff]
> 
> Fixed a bug where the last song in the play queue is repeated.
> MariaDB support etc.

  * [fix] Update jackson-databind to 2.10.0.pr3(CVE-2019-14540, CVE-2019-16335).
  * [fix] Fixed a edge case where artist reading analysis failed.
  * [update] The sorting algorithm and settings shared internally. Most features now work with the same sorting rules.
  * [update] DNLA Japanese language support has started. Provides title translation and complete dictionary sorting.
  * [update] Added an option to strictly sort DNLA/REST-ID3 in the sorting options. Necessary when handling DNLA in Japanese.

## v105.1.0
#### Based on *airsonic 10.5.0-SNAPSHOT eb4c5a0*

> [eb4c5a0]
> 
> Minor screen and player fixes, compatible with tomcat9.

  * [fix] Fixed a case where excessive Japanese translations were done when tags contained uppercase alphabets.
  * [update] Update jackson-databind to 2.9.9.3(CVE-2019-12086).
  * [update] Improved translation of Japanese messages. Fixed mistranslation due to design misread.
  * [update] Supports composer tag scanning and searching. Search is possible when composer is turned on as an option.
  * [update] Added header to song table. Header is possible when composer/genre is turned on as an option.
  * [update] Improved sorting of Play queue. Change to ignore upper/lower case.
  * [update] Add advanced sorting options. (Changing Various artist sorting rules / Sort serial numbers)
  * [update] Add artist-specific stopwords. "CV, feat, with" are ignored when searching the Artist field.

## v105.0.0
#### Based on *airsonic 10.5.0-SNAPSHOT 3c5735e*

> [3c5735e]
> 
> Minor screen and player fixes, bug fix.

  * Minor screen and player fixes.
  * Fixed a bug that wrong path may be used when searching.
  * Added JSP pre-compilation.
  * [update] Update lucene to 8.2.0. 
  * [update] Refactoring the search function. Japanese processing is expensive, but you can still search faster than Subsonic.
  * [update] Changed random function used when creating random list to use higher entropy function.
    It depends on the platform.
    NativePRNG is tried and SHA1PRNG is used if it is not supported.
    If neither is available, use the same random function as before.

## v104.0.0
#### Based on *airsonic 10.4.0-RELEASE*

  * [update] Theme update. Changed the main theme image to SVG and updated CSS.
  * [update] Temporary workaround for the issue of stopping the scan when the wrong pattern data is read at scan time.

## v103.0.1
#### Based on *airsonic 10.4.0-SNAPSHOT c834bde*

> [c834bde]
> 
> Only player modification and search design changes.

  * fix problems moving to the next song automatically
  * fix Progress bar

## v103.0.0
#### Based on *airsonic 10.4.0-SNAPSHOT 2bfaea2*

> [2bfaea2]
> 
> Security fixes, codebase modernization etc.

  * Security update (spring:CVE-2019-11272&CVE-2019-11272, tomcat:CVE-2019-10072, jackson:CVE-2019-12814)
  * Migrate travis environment from oraclejdk to openjdk.
  * Remove Flash related implementation.
  * Various minor fixes related to javascript.
  * Streaming test enhancements
  etc

## v102.0.0
#### Based on *airsonic 10.3.1-RELEASE*

> [10.3.1-RELEASE]
> 
> Bug fixes, resource saving fixes, security fixes, codebase modernization, docker image update, support for Java 9 and greater etc.

  * Security update (jetty:CVE-2019-10241, CVE-2019-10246)
  * [fix] Fixed a bug that property may be overwritten with values other than firstChild when updating artistSort of AlbumId3.
  * [update] Added processing to delete unnecessary data from lucene index when scanning.
  * [update] Added multi genre support.

## v101.1.0
#### Based on *airsonic 10.3.0-SNAPSHOT c3a1980*

  * Security update (spring:CVE-2019-3795)
  * [update] Compatible with ID3v2.4. For files in ID3v2.4 format, will be load additional readable fields.
  * [update] Analysis improvement of artist reading.
             (1) Change the Tokenize method from Japanase analysis to ID3v2.4 word delimiter. Mis-analysis is reduced.
             (2) Changed not to exclude character types. This means that you can use the reading field with other than Japanese.
  * [update] Improved the process of scan replacement. Fixed to create a complete index in one scan.
  * [update] Added automatic generation change of search index. 
             When updating with definition changes, if the existing index data is old, will be delete it without reading it.
             You can recover only to the normal state by scanning once.

## v101.0.1
#### Based on *airsonic 10.3.0-SNAPSHOT c3a1980*

> [c3a1980]
> 
> A lot of JavaScript improvements, Launch on Jetty. Improving log output when running Jetty etc.

  * [fix] Fixed the problem of duplicate results in random search.
  * [fix] Fixed the problem that double registration occurs when creating search index.
  * [fix] Fixed a bug that DNLA which was occured in v101.0.0 can not be used.

## v101.0.0
#### Based on *airsonic 10.3.0-SNAPSHOT e330eeb*

> [e330eeb]
> 
> Fixes to improve DB reliability, Organize JavaScript, update some libraries, etc.
> 
> Suppress CVE by false positives(spring:CVE-2018-1258)

  * [fix] Fixed to prevent Java errors on the screen if a search is made when there is no search index data.
  * [fix] Fixed double search issue with random search.

## v100.1.0
#### Based on *airsonic 10.2.1-RELEASE*

  * Security update (checkstyle:CVE-2019-9658) There is no impact on already running servers
  * [fix] Fixed a bug that search cannot be performed if Music Folder exist with a specific string pattern.
  * [fix] Fixed a bug that year can not be specified in random search.
  * [update] lucene has been updated to 7.7.1.
  * [update] Adjusted the Boost value at search. 
    The order of the search results is weighted in the following priority order.
    (1) Hiragana input assistance for each Artist / Album / Song / (2) full name assistance for each (3) parsed words.
    (1) and (3) are indexed as necessary to take into account the amount of data in order to eliminate Japanese ambiguity.
  * [update] Fix for speed improvement Index reading cache, deletion of unnecessary copies, etc.
    Covers redundant, time-consuming Japanese processing and performs as fast as Airsonic and Subsonic.

## v100.0.0
#### Based on *airsonic 10.2.1-RELEASE*

  * Security update (stax:CVE-2018-20222) Prevent xxe during parse
  * Based on airsonic 10.2.1-RELEASE.
  * Jpsonic public repository has been created. The version check and release page has been changed to refer public repository.

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
