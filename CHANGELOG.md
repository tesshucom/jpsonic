<!--
# CHANGELOG.md
# jpsonic/jpsonic
# -->

## v111.0.0 (SNAPSHOT)

The binaries of SNAPSHOT are available from the latest artifacts on the [master branch](https://github.com/tesshucom/jpsonic/actions?query=branch%3Amaster) .

#### Enhancement
  * Update libs. Includes fixes for CVE-2021-44228, CVE-2021-45046
  * Bump Spring Boot from 2.5.7 to 2.6.1 ([#1274](https://github.com/tesshucom/jpsonic/issues/1274))
  * Bump HSQLDB from 2.5.0 to 2.6.1 ([#1145](https://github.com/tesshucom/jpsonic/issues/1145))
  * Add build number to UPnP device details　and About page
  * Add support for Windows Media Player ([#381](https://github.com/tesshucom/jpsonic/issues/381))

<details>
<summary>v110.2.0</summary>

#### Enhancement
  * Delete shoutcast ([#1201](https://github.com/tesshucom/jpsonic/issues/1201))
  * Fix to control the format of Stream received by other than Subsonic app ([#1187](https://github.com/tesshucom/jpsonic/issues/1187))
  * Improve transcoding settings page ([#1191](https://github.com/tesshucom/jpsonic/issues/1191))
  * Fix not to show update button by default ([#1223](https://github.com/tesshucom/jpsonic/issues/1223))
  * Change the initial value of Upload permission ([#1224](https://github.com/tesshucom/jpsonic/issues/1224))
  * Romaized Japanese language support ([#319](https://github.com/tesshucom/jpsonic/issues/319))
  * Add resampling transcoding from high-res to CD quality ([#1232](https://github.com/tesshucom/jpsonic/issues/1232))

#### Fixes
  * Fix to reply with the appropriate content type ([#1206](https://github.com/tesshucom/jpsonic/issues/1206))
  * Fix bug that automatic scanning did not start ([#1208](https://github.com/tesshucom/jpsonic/issues/1208))

</details>
<details>
<summary>v110.1.0</summary>

#### Fixes
  * Fix bug that Java 17 build does not run with the correct class version ([#1183](https://github.com/tesshucom/jpsonic/issues/1183))

</details>

<details>
<summary>v110.1.0</summary>

#### Enhancement
  * Java 17 Support
  * Delete Jukebox ([#1107](https://github.com/tesshucom/jpsonic/issues/1107))
  * Delete Sonos ([#1159](https://github.com/tesshucom/jpsonic/issues/1159))
  * Improve setting page to make it easier to switch getNowPlaying ON/OFF ([#1048](https://github.com/tesshucom/jpsonic/issues/1048))
  * Add options to control checking for update dates during scanning ([#1101](https://github.com/tesshucom/jpsonic/issues/1101))
  * Fix to show changed user/player on reload ([#1148](https://github.com/tesshucom/jpsonic/issues/1148), [#1151](https://github.com/tesshucom/jpsonic/issues/1151))
  * Fix to support FLAC playback with MediaMonkey for Windows ([#1157](https://github.com/tesshucom/jpsonic/issues/1157))
  * Fix guest user specifications ([#1160](https://github.com/tesshucom/jpsonic/issues/1160))
  * Improvements regarding bitrate items ([#1171](https://github.com/tesshucom/jpsonic/issues/1171))
  * Fix to show IP address of anonymous user ([#1176](https://github.com/tesshucom/jpsonic/issues/1176))
  * Update libs

#### Fixes
  * Fix bug that change coverart is not working ([#1051](https://github.com/tesshucom/jpsonic/issues/1051))
  * Fix bug that some layouts are broken, in certain languages ([#1103](https://github.com/tesshucom/jpsonic/issues/1103))
  * Fix bug that database cleanup could not be started ([#1109](https://github.com/tesshucom/jpsonic/issues/1109))
  * Fix bug that the reading of the artist (directory) was not updated ([#1110](https://github.com/tesshucom/jpsonic/issues/1110))
  * Fix bug that UPnP did not start even if the setting was enabled ([#1149](https://github.com/tesshucom/jpsonic/issues/1149))

</details>

<details>
<summary>v110.0.0</summary>

#### Updates
  * Streaming improvements. Speeds up transcoding, playback start, and playback position changes.
  * Change transcoding spec for anonymous users
  * Add option to change buffer size of transmitted data
  * Add option to simplify logging
  * Introduce Graceful shutdown
  * Change logo. Tiny CSS fixes
  * Raising JDK requirements. End of Java8 support.
  * Migrating from JUnit 4 to JUnit 5
  * Update libs

#### Fixes
  * Fix degradation that the player type is not displayed correctly
  * Fix bug that playing might be interrupted
  * Fix bug that the Mime type may not be correct on UPnP
  * Fix bug temporary files might not be deleted after transcoding

</details>

<details>
<summary>v109.5.0 Based on airsonic 11.0.0-SNAPSHOT 5c71659</summary>

#### Updates

  * Update libs
  * Minor web page fixes primarily for mobile and Firefox
  * Replace avatar image with new image
  * Add Special Thanks to About page

</details>

<details>
<summary>v109.4.0 Based on airsonic 11.0.0-SNAPSHOT 5c71659</summary>

#### Fixes

  * Update libs. Includes fixes for CVE-2020-13954, CVE-2020-27218 and updates mediaelements.js
  * Fix bug where video meta-analysis was incorrect on Windows
  * Fix browsing feature of video directory
  * Fix share in playqueue

#### Other updates

  * Support JDK15
  * Add feature to change the font/font size of web pages
  * Add voice recognition search in web page
  * Improve video player in web page
  * Add maximization feature to video player
  * Add picture in picture feature to video player
  * Add option to open and close the playqueue with double click/tap
  * Suppress network status page to be available only to administrators by default
  * Delete the frame on the right side of web page
  * Suppress the list of songs currently playing and make them available only to administrators by default
  * Fix to display scan status regardless of settings
  * Add option to display information and links for the song being played
  * Minor fixes for CSS and messages
</details>

<details>
<summary>v109.3.0 Based on airsonic 11.0.0-SNAPSHOT 5c71659</summary>

#### Fixes

  * Various library updates (Includes fix of CVE-2020-5421, CVE-2015-5211 and CVE-2020-11979)

#### Other updates

  * Remove tags that are not recommended in HTML5
  * Remove opening and closing of playqueue by mouse hover
  * Remove old themes all and add new themes
  * Add list view for podcasts and playlists
  * Add index to Home
  * Add "Suppressed legacy features" and "Additional display features" to settings. It suppresses amount of display
  * Add verbose help to setting pages. Redundant help has been added to some setting items and can be turned ON / OFF at once
  * Add a button to reset to the initial value for some setting items
  * Add option to force Bio's display language to English
  * Add an option to allow general users to view logs
  * Fix drawer and playqueue layout
  * Fix layout so that songs with long titles like classical music are not truncate
  * Fix setting pages
  * Fix breadcrumb
</details>

<details>
<summary>v109.2.0 Based on airsonic 11.0.0-SNAPSHOT 5c71659</summary>

#### Fixes

  * Clean up CVE suppression files and remove unnecessary rules
  * Various library updates (ecj, mariadb-java-client, jackson, cxf, pmd, liquibase-core, checker-qual, tomcat, mysql-connector-java, lucene, commons-lang3)
  * Fix a bug that sanitization was insufficient in JSP
  * Fix a bug that cache image may not be generated correctly
  * Fix many potential bugs related to memory leaks

#### Other updates

  * Add compilation profile for Java11 and Java14
  * Built-in Japanese font added
  * Japanese font can be used for chart images and cover art images.
  * Change the design of the chart image
  * Add a theme that can use Japanese fonts to the theme of Web pages
  * Fix some web page for tags and CSS
</details>

<details>
<summary>v109.1.0 Based on airsonic 11.0.0-SNAPSHOT 5c71659</summary>

> Jpsonic will be developed for LTS Java11 from this version.
> Compatibility with Java 11 or later is given priority, and compatibility with Java 10 or earlier is not necessarily guaranteed.

#### Fixes

  * Updated ant to 1.10.8(CVE-2020-1945).
  * Updated spring-boot-dependencies to 2.2.7(CVE-2020-5407).
  * Updated websocket to 2.0.0-M1(CVE-2020-11050).

#### Other updates

  * Numerous library updates ([diff...](https://github.com/tesshucom/jpsonic/compare/0d68d71...ce8633c)).
  * Update hsqldb to 2.5
  * Add new display item to Upnp (MusicFolder/Artist/Album/Song). 
  * Add special processing for searching by Japanese voice input.
    You can search for artists that include a delimiter by typing without the delimiter.
    It has no effect on anything other than Japanese.
  * CSS reorganization using SCSS (Jpsonic theme only).
    Currently the JSP modifications are limited,
    but in the later versions, the keyboard operability and CSS classes etc will be modified.
</details>

<details>
<summary>v109.0.0 based on airsonic 11.0.0-SNAPSHOT 5c71659</summary>

> Includes bug fixes for 10.6.1 and 10.6.2. Does not include updates to HSQLDB.
> 
> [eb4c5a0]
> 
>  - Update Sonos wsdl file
>  - Refactor transcoding/downsampling bitrate limits
>  - Change the default naming convention for podcasts
>  - Update spring-boot to 2.2
>  - Fixed a bug that the play button on the web does not start playing
>  - Fix Last.FM scrobbling on AudioScrobbler API v1
>  - Fix path issue on Windows(internal diagnostics page)
>  - Fix UTF-8 detection on some systems using non-standard locales(internal diagnostics page)
> 
> In addition, library updates etc. 

  * [fix] Updated apache-jsp to 9.4.28.v20200408(CVE-2020-1745). Compiling with the Tomcat profile is not affected.
  * [update] Support for phrase search.
</details>

<details>
<summary>v108.0.0 based on airsonic 10.6.0-RELEASE</summary>

  * [fix] Update jquery to 3.5.
  * [fix] Fixed share psge icon image and link.
  * [update] Removed artist image from biography on Web page.
    Because this is not a proper implementation under Japanese law.
    If a better solution is implemented in the future, it will be modified again to display the image.
</details>

<details>
<summary>v107.2.0 based on airsonic 10.6.0-SNAPSHOT 80ccd82</summary>

  * [fix] Update Jetty to 9.4.27.v20200227 (CVE-2020-1935).
  * [fix] Update Jackson to 2.10.3 (CVE-2020-8840, CVE-2019-20330)
  * [fix] Update commons-configuration2 to 2.7 (CVE-2020-1953)
  * [fix] Update cxf to 3.3.6 (CVE-2020-1954)
  * [fix] Fixed a bug that albums with specific data patterns may not be scanned correctly.
    This is a legacy implementation bug.
    Existing web pages will not be affected, but will affect REST and Jpsonic UPnP implementations.
  * [fix] Fix the bug that only specific pattern queries are skipped in UPnP search.
    Improved song search using artist/composer as key.
  * [update] Change the sort-tag-rearrangement process of after scan.
    Merge processing when there are multiple sort-tags in one name has been changed to stricter processing.
     - In addition to album, artist, albumArtist sort-tags, composer is included in the target.
     - In the case of the sort tag of the person name, it takes precedence in the order of changeDate/albumArtist/artist/composer.
     - In the case of the sort tag of album name, it takes precedence changeDate.
  * [update] Change the conditions under which sort-tags are used for indexing and sorting.
     - Previously, sort-tags were not used if the first string of name was alphabetic.
     - Changed to use sort tag if name and sort tag start with alphabet and if sort tag contains Japanese.
  * [update] Add a column to keep original sort-tag in DB. Currently it does not provide any new features by itself.
       It is intended for future tag-checkers, or to address the need for users to write and check SQL.
  * [update] UPnP display item name changed(En).
       - RecentAlbums & RecentAlbums(ID3) -> Recently added albums & Recently tagged albums.
  * [update] New display items have been added in UPnP.
       - MusicFolder/Artist/RandomSong.
  * [update] Improved the class of container sent by UPnP. Some clients have effects such as improved icon display.
  * [update] The UPnP setting screen has been improved so that the relationship between the selected item and the display name can be easily understood.
  * [update] Changed UPnP to not display artist images obtained from external services.
    (The implementation displaying the tag image of ID3 instead is not deleted.)
    Because this is not a proper implementation under Japanese law.
    In later versions, the same policy will also remove images of artists located except for UPnP.
    If a better solution is implemented in the future, it will be modified again to display the image.
</details>

<details>
<summary>v107.1.0 based on airsonic 10.6.0-SNAPSHOT 80ccd82</summary>

> [80ccd82]
> Numerous library updates, Popup improvements, health check page added etc.

  * [fix] Update Tomcat to 8.5.51(CVE-2020-1935, CVE-2019-17569).
  * [fix] Fix not to perform cleanup during scan.
  * [fix] Sorting fixes and testing enhance.
     - Fix classify English (words starting with the alphabet) and others.
     - Fix for sorting of titles including parentheses and numbers.
     - Fix to sort correctly on Home > All.
</details>

<details>
<summary>v107.0.0 based on airsonic 10.6.0-SNAPSHOT 64fad6a</summary>

> [64fad6a]
> Startup exception suppression, ListenBrainz support, player slider re-implementation, small web screen improvements, etc.

  * [fix] Update cxf to 3.3.5(CVE-2019-17573).
  * [fix] Fixed a bug where some DLNA items could not be used.
  * [update] Change DLNA startup port option.
        From this version, the startup port of UPnP server can be changed by -DUPNP_PORT.
        Airsonic has assigned a default UPnP port to 4041.
        Jpsonic will still make the same automatic assignment.
        If specified port with startup option, will follow it.
  * [update] Add folder access control option to DLNA.
        When this function is turned on, the folders published on DLNA are limited to the music folder specified by the guest user.
  * [update] Add two new items to DLNA (Id3 tag based index and random songs per artist).
  * [update] Add an option to specify the size of the random list used in DLNA.
        You can change the upper sizeof three items related to the random list..
  * [update] DLNA index cache improvements.
        The index cache can be up to 2 minutes, but will be automatically cleared if needed.
        Change to clear the cache automatically after scanning, changing media folder permissions, and changing music folder settings.
</details>

<details>
<summary>v106.1.1 based on airsonic 10.5.0-RELEASE</summary>

  * [fix] Update Tomcat to 8.5.50 (CVE-2019-12418, CVE-2019-17563).

Critical security fix.
The following measures taken.

 - Update Tomcat version to 8.5.50. The only version that addresses threats now.
 - Stop Tomcat precompiler. Because it depends on 8.5.40. As a result, the initial display of the web screen is slightly slower.
 - Jetty will continue to change to a compilable configuration. However,
   since it does not respond to threats, no official distribution will be made.
   It only supports arbitrary compilation.
</details>

<details>
<summary>v106.1.0 based on airsonic 10.5.0-RELEASE</summary>

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
</details>

<details>
<summary>v106.0.0 based on airsonic 10.5.0-RELEASE</summary>

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
</details>

<details>
<summary>v105.2.1 based on airsonic 10.5.0-SNAPSHOT 06e36ff</summary>

  * [fix] Fixed bug that fail when migrating with postgres from v105.1.0 to v105.2.0.
</details>

<details>
<summary>v105.2.0 based on airsonic 10.5.0-SNAPSHOT 06e36ff</summary>

> [06e36ff]
> 
> Fixed a bug where the last song in the play queue is repeated.
> MariaDB support etc.

  * [fix] Update jackson-databind to 2.10.0.pr3(CVE-2019-14540, CVE-2019-16335).
  * [fix] Fixed a edge case where artist reading analysis failed.
  * [update] The sorting algorithm and settings shared internally. Most features now work with the same sorting rules.
  * [update] DNLA Japanese language support has started. Provides title translation and complete dictionary sorting.
  * [update] Added an option to strictly sort DNLA/REST-ID3 in the sorting options. Necessary when handling DNLA in Japanese.
</details>

<details>
<summary>v105.1.0 based on airsonic 10.5.0-SNAPSHOT eb4c5a0</summary>

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
</details>

<details>
<summary>v105.0.0 based on airsonic 10.5.0-SNAPSHOT 3c5735e</summary>

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
</details>

<details>
<summary>v104.0.0 based on airsonic 10.4.0-RELEASE</summary>

  * [update] Theme update. Changed the main theme image to SVG and updated CSS.
  * [update] Temporary workaround for the issue of stopping the scan when the wrong pattern data is read at scan time.
</details>

<details>
<summary>v103.0.1 based on airsonic 10.4.0-c834bde</summary>

> [c834bde]
> 
> Only player modification and search design changes.

  * fix problems moving to the next song automatically
  * fix Progress bar
</details>

<details>
<summary>v103.0.0 based on airsonic 10.4.0-SNAPSHOT 2bfaea2</summary>

> [2bfaea2]
> 
> Security fixes, codebase modernization etc.

  * Security update (spring:CVE-2019-11272&CVE-2019-11272, tomcat:CVE-2019-10072, jackson:CVE-2019-12814)
  * Migrate travis environment from oraclejdk to openjdk.
  * Remove Flash related implementation.
  * Various minor fixes related to javascript.
  * Streaming test enhancements
  etc
</details>

<details>
<summary>v102.0.0 based on airsonic 10.3.1-RELEASE</summary>

> [10.3.1-RELEASE]
> 
> Bug fixes, resource saving fixes, security fixes, codebase modernization, docker image update, support for Java 9 and greater etc.

  * Security update (jetty:CVE-2019-10241, CVE-2019-10246)
  * [fix] Fixed a bug that property may be overwritten with values other than firstChild when updating artistSort of AlbumId3.
  * [update] Added processing to delete unnecessary data from lucene index when scanning.
  * [update] Added multi genre support.
</details>

<details>
<summary>v101.1.0 based on airsonic 10.3.0-SNAPSHOT c3a1980</summary>

  * Security update (spring:CVE-2019-3795)
  * [update] Compatible with ID3v2.4. For files in ID3v2.4 format, will be load additional readable fields.
  * [update] Analysis improvement of artist reading.
             (1) Change the Tokenize method from Japanase analysis to ID3v2.4 word delimiter. Mis-analysis is reduced.
             (2) Changed not to exclude character types. This means that you can use the reading field with other than Japanese.
  * [update] Improved the process of scan replacement. Fixed to create a complete index in one scan.
  * [update] Added automatic generation change of search index. 
             When updating with definition changes, if the existing index data is old, will be delete it without reading it.
             You can recover only to the normal state by scanning once.
</details>

<details>
<summary>v101.0.1 based on airsonic 10.3.0-SNAPSHOT c3a1980</summary>

> [c3a1980]
> 
> A lot of JavaScript improvements, Launch on Jetty. Improving log output when running Jetty etc.

  * [fix] Fixed the problem of duplicate results in random search.
  * [fix] Fixed the problem that double registration occurs when creating search index.
  * [fix] Fixed a bug that DNLA which was occured in v101.0.0 can not be used.
</details>

<details>
<summary>v101.0.0 based on airsonic 10.3.0-SNAPSHOT e330eeb</summary>

> [e330eeb]
> 
> Fixes to improve DB reliability, Organize JavaScript, update some libraries, etc.
> 
> Suppress CVE by false positives(spring:CVE-2018-1258)

  * [fix] Fixed to prevent Java errors on the screen if a search is made when there is no search index data.
  * [fix] Fixed double search issue with random search.
</details>

<details>
<summary>v100.1.0 based on airsonic 10.2.1-RELEASE</summary>

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
</details>

<details>
<summary>v100.0.0 based on airsonic 10.2.1-RELEASE</summary>

  * Security update (stax:CVE-2018-20222) Prevent xxe during parse
  * Based on airsonic 10.2.1-RELEASE.
  * Jpsonic public repository has been created. The version check and release page has been changed to refer public repository.
</details>

<details>
<summary>v2.3.0</summary>

  * Security update (stax:CVE-2018-1000840)
  * Fix for embedded Jetty compilation for evaluation purposes.
  * Based on airsonic e4bb808 (2019-2) Pull translations from transifex.
</details>

<details>
<summary>v2.2.6</summary>

  * Security update (jackson-databind:CVE-2018-19360 - CVE-2018-19362, CVE-2018-14718 - CVE-2018-14721)
  * Based on airsonic adc2241 (2019-1) Fix broken keyboard shortcuts, defrag on HSQLDB, connection pooling for external database etc.
</details>

<details>
<summary>v2.2.5</summary>

  * Security update (guava:CVE-2018-10237)
  * Suppress CVE by false positives(stax:CVE-2017-16224, slf4j:CVE-2018-8088)
  * Localization of version check. Changed Jpsonic update to notify management screen
  * Based on airsonic 77ca475 (2018-12) Screen modification, updating of various libraries, modification of test content, etc.
   - Modification of partial wording accompanying cleanup of overall translation
   - Image replacement related to adding icons for various devices
</details>

<details>
<summary>v2.2.4</summary>

  * Fixed a bug where part of the start argument was not correctly recognized
    (jpsonic.defaultMusicFolder, jpsonic.defaultPodcastFolder, jpsonic.defaultPlaylistFolder)
  * Introduction of Airsonic integration test using Docker
</details>

<details>
<summary>v2.2.3</summary>

  * Security update for cxf(CVE-2018-8039)
  * Based on airsonic 685f4fa (2018-10)
</details>

<details>
<summary>v2.2.2</summary>

  * Improvement of Japanese Song search accuracy.
  * Random search fault correction.
  * Based on airsonic 8ba0bc8 (2018-8)
</details>

<details>
<summary>v2.2.1</summary>

  * Security fix (LDAP authentication without a password).
  * Based upon Airsonic 10.2.0-SNAPSHOT f6905de(2018-8)
  * Start build test with travis.
</details>

<details>
<summary>v2.2</summary>

  * Forward search reinforcement of artist name. Corresponds to full name, hiragana, katakana.
  * Added index rebuilding process after scanning.
  * Based upon Airsonic 10.2.0-SNAPSHOT 8d3c0ec(2018-7)
</details>

<details>
<summary>v2.1</summary>

  * Update of lucene-core(3.0.3 -> 7.4.0).
  * Simple Japanese phrase search.
</details>

<details>
<summary>v2.0</summary>

  * Based upon Airsonic 10.2.0-SNAPSHOT 83ef76a(2018-7)
</details>

<details>
<summary>v1.3</summary>

  * It corresponds to ALBUM_SORT
  * Final release based upon Airsonic 10.1.1-RELEASE
</details>

<details>
<summary>v1.2.2</summary>

  * It corresponds to ARTIST_SORT, ALBUM_ARTIST_SORT
  * Fixed a bug that caused case ignoring excessively. (Alphabet is originally A-Za-z)
</details>

<details>
<summary>v1.2.1</summary>

  * Fixed bug related to sort of id 3
</details>

<details>
<summary>v1.2</summary>

  * Supports sorting using not only morphological analysis but also tag analysis
</details>

<details>
<summary>v1.1</summary>

  * Japanese index / Artist sort (id3)
  * Duplicate records may be included in getAlbunList Fixed a problem
  * Change DLNA icon
</details>

<details>
<summary>v1.0</summary>

  * Japanese index / Artist sort (File structure)
  * Fixed bug in Lang of biography
  * Default Japanese
  * First release as Jpsonic
  * Based upon Airsonic 10.1.1-RELEASE
</details>
