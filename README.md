<!--
# README.md
# jpsonic/jpsonic
-->

[![CI](https://github.com/tesshucom/jpsonic/workflows/CI/badge.svg)](https://github.com/tesshucom/jpsonic/actions?query=workflow%3ACI)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/tesshucom/jpsonic.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/tesshucom/jpsonic/alerts/)
[![Language grade: JavaScript](https://img.shields.io/lgtm/grade/javascript/g/tesshucom/jpsonic.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/tesshucom/jpsonic/context:javascript) 
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/tesshucom/jpsonic.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/tesshucom/jpsonic/context:java)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/740778cca284442080c319e5469eaa33)](https://www.codacy.com/gh/tesshucom/jpsonic/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=tesshucom/jpsonic&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/740778cca284442080c319e5469eaa33)](https://www.codacy.com/gh/tesshucom/jpsonic/dashboard?utm_source=github.com&utm_medium=referral&utm_content=tesshucom/jpsonic&utm_campaign=Badge_Coverage)

Jpsonic
========

What is Jpsonic?
-----------------

Jpsonic is a free, web-based media streamer, an [Airsonic](https://github.com/airsonic/airsonic) Clone.

<table>
<tr>
<td>
<img src="contrib/assets/screenshot1.png" width="200">
</td>
<td>
<img src="contrib/assets/screenshot2.png" width="200">
</td>
<td>
<img src="contrib/assets/screenshot3.png" width="200">
</td>
<td>
<img src="contrib/assets/screenshot4.png" width="200">
</td>
</tr>
</table>

Features
-----------------

<details>
<summary>Enhanced meta processing</summary>

To process Japanese well on a machine requires quite complicated mechanism.
The index, sort, and search features of Jpsonic have been replaced with more accurate and reliable processing than Subsonic and Airsonic.
It's not just a fix that is useful only to Japanese people.
It will be further improved in the future, and Japanese index feature for overseas users will be added.

</details>

<details>
<summary>General ID3 tags are supported</summary>

It supports standard tags, and the SONY/APPLE specifications are used as a reference.
The reason Jpsonic refers to the SONY/APPLE specification is that their specifications take into account global multilingual support.
Also supports multiple genres.

|tag name |tag id|Subsonic/Airsonic |Jpsonic |Music Center (SONY) |itunes (APPLE)
|:---|:---|:---:|:---:|:---:|:---:|
|title |TIT2 |● |● |● |●
|title sort|TSOT | |● |● |●
|artist|TPE1 |● |● |● |●
|artist sort|TSOP | |● |● |●
|album |TALB |● |● |● |●
|album sort|TSOA | |● |● |●
|album artist|TPE2 |● |● |● |●
|album artist sort|TSO2 | |● |● |●
|genre|TCON |● |● |● |●
|Release year|TYER |● |● |● |●
|composer|TCOM | |● |● |●
|composer sort|TSOC | |● | |●
|track no|TRCK |● |● |● |●
|disk no|TPOS |● |● |● |●

</details>

<details>
<summary>Reimplemented DLNA features</summary>

DLNA (UPnP) and OpenHome is mainstream technology in Japanese typical household.
For this reason, many new features have been added to Jpsonic.
Many display variations can be selected as options.
You can specify the Music Folder to be published.
It also runs faster than Subsonic and Airsonic.

</details>

<details>
<summary>Support for speech recognition</summary>

The point of Japanese meta processing is the handling of syllables.
The search feature by speech recognition from applications and browsers is inevitably enhanced.
Even in languages other than Japanese where syllable processing is difficult, there is a possibility that it can be processed in the same way as Japanese by using sort tags well. You can use apps that support server side search and use speech recognition, such as [Subsonic Music Streamer](https://play.google.com/store/apps/details?id=net.sourceforge.subsonic.androidapp&hl=ja&gl=US) and [BubbleUPnP](https://play.google.com/store/apps/details?id=com.bubblesoft.android.bubbleupnp&hl=en) .
With a specific browser, voice recognition is possible from a headset connected to a PC.

</details>

Usage
-----

The basic installation procedure is almost the same as Airsonic. Please use the [Airsonic documentation](https://airsonic.github.io/docs/) for instructions on running Airsonic. A more detailed specification description can be found at the [author's site](https://tesshu.com/category/spec).

History
-----

<details>
<summary>Subsonic, Libresonic, Airsonic</summary>

The original *[Subsonic](http://www.subsonic.org/)* is developed by [Sindre Mehus](mailto:sindre@activeobjects.no). *Subsonic* was open source through version 6.0-beta1, and closed-source from then onwards.

*Libresonic* was created and maintained by [Eugene E. Kashpureff Jr](mailto:eugene@kashpureff.org). It originated as an unofficial("Kang") of Subsonic which did not contain the Licensing code checks present in the official builds. With the announcement of Subsonic's closed-source future, a decision was made to make a full fork and rebrand to Libresonic.

Around July 2017, it was discovered that Eugene had different intentions/goals for the project than some contributors had. 
*Airsonic* was created in order to provide a full-featured, stable, self-hosted media server based on the Subsonic codebase that is free, open source, and community driven.

</details>

<details>
<summary>Jpsonic</summary>

Around July 2018, *Jpsonic* was created in order to strengthen browsing and searching in Japanese.

In Japan, Subsonic is famous, but Airsonic was not yet well known.
Today, Airsonic, with its great engineers and great community, is gaining recognition.


![history](contrib/assets/history.png)

Jpsonic had to update its indexing, sorting, and searching due to its characteristics.
Many of these features have bug fixes or enhancements.
An update to the Jpsonic search engine has been provided to Airsonic.
Therefore, the design of the search function of Airsonic and Jpsonic is a bit similar.

</details>

<details>
<summary>Cherry Blossoms</summary>

The Japanese loved cherry blossoms for hundreds of years. Please ask the Japanese people "What is a flower?". The Japanese will answer "Sakura". The Japanese frequently plants cherry blossoms in international exchange to show respect for partners.
</details>

License
-------

Jpsonic is free software and licensed under the [GNU General Public License version 3](http://www.gnu.org/copyleft/gpl.html). The code in this repository (and associated binaries) are free of any "license key" or other restrictions.

The [Subsonic source code](https://github.com/airsonic/subsonic-svn) was released under the GPLv3 through version 6.0-beta1. Beginning with 6.0-beta2, source is no longer provided. Binaries of Subsonic are only available under a commercial license. There is a [Subsonic Premium](http://www.subsonic.org/pages/premium.jsp) service which adds functionality not available in Airsonic. Subsonic also offers RPM, Deb, Exe, and other pre-built packages that Airsonic [currently does not](https://github.com/airsonic/airsonic/issues/65).

The cover zooming feature is provided by [jquery.fancyzoom](https://github.com/keegnotrub/jquery.fancyzoom),
released under [MIT License](http://www.opensource.org/licenses/mit-license.php).

The icons are from the amazing [feather](https://feathericons.com/) project,
and are licensed under [MIT license](https://github.com/feathericons/feather/blob/master/LICENSE).

[Kazesawa font](https://kazesawa.github.io/) is used for Japanese fonts. Copyright (C) 2002-2015 M+ FONTS PROJECT.
