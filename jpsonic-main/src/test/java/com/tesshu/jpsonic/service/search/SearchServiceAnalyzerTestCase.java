/*
 This file is part of Jpsonic.

 Jpsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Jpsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2018 (C) tesshu.com
 */
package com.tesshu.jpsonic.service.search;

import java.text.Normalizer;
import java.util.List;

import javax.annotation.Resource;

import org.airsonic.player.service.SearchService;
import org.apache.lucene.analysis.Analyzer;

import junit.framework.TestCase;

public class SearchServiceAnalyzerTestCase extends TestCase {

	@Resource
	private SearchService searchService;

	private Analyzer analyzer;

	@Override
	public void setUp() {
		analyzer = AnalyzerUtil.createAirsonicAnalyzer(searchService);
	}

	public void testResource() {

		/*
		 * /MEDIAS/Music/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg Variations, Canons [Disc 1]
		 * /01 - Bach- Goldberg Variations, BWV 988 - Aria.flac
		 * 
		 */

		// title
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "Bach: Goldberg Variations, BWV 988 - Aria");
		assertEquals(6, terms.size());
		assertEquals("bach", terms.get(0));
		assertEquals("goldberg", terms.get(1));
		assertEquals("variations", terms.get(2));
		assertEquals("bwv", terms.get(3));
		assertEquals("988", terms.get(4));
		assertEquals("aria", terms.get(5));

		// artist
		terms = AnalyzerUtil.toTermString(analyzer, "_ID3_ARTIST_ Céline Frisch: Café Zimmermann");
		assertEquals(5, terms.size());
		assertEquals("id3_artist", terms.get(0));
		assertEquals("celine", terms.get(1));
		assertEquals("frisch", terms.get(2));
		assertEquals("cafe", terms.get(3));
		assertEquals("zimmermann", terms.get(4));

		// album
		terms = AnalyzerUtil.toTermString(analyzer, "_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]");
		assertEquals(7, terms.size());
		assertEquals("id3_album", terms.get(0));
		assertEquals("bach", terms.get(1));
		assertEquals("goldberg", terms.get(2));
		assertEquals("variations", terms.get(3));
		assertEquals("canons", terms.get(4));
		assertEquals("disc", terms.get(5));
		assertEquals("1", terms.get(6));

		/*
		 * /MEDIAS/Music/_DIR_ Céline Frisch- Café Zimmermann - Bach- Goldberg Variations, Canons [Disc 1]
		 * /02 - Bach- Goldberg Variations, BWV 988 - Variatio 1 A 1 Clav..flac
		 * 
		 */

		// title
		terms = AnalyzerUtil.toTermString(analyzer, "Bach: Goldberg Variations, BWV 988 - Variatio 1 A 1 Clav.");
		assertEquals(9, terms.size());
		assertEquals("bach", terms.get(0));
		assertEquals("goldberg", terms.get(1));
		assertEquals("variations", terms.get(2));
		assertEquals("bwv", terms.get(3));
		assertEquals("988", terms.get(4));
		assertEquals("variatio", terms.get(5));
		assertEquals("1", terms.get(6));
		assertEquals("1", terms.get(7));
		assertEquals("clav", terms.get(8));

		/*
		 * /MEDIAS/Music/_DIR_ Ravel/_DIR_ Ravel - Chamber Music With Voice
		 * /01 - Sonata Violin & Cello I. Allegro.ogg
		 */

		// title
		terms = AnalyzerUtil.toTermString(analyzer, "Sonata Violin & Cello I. Allegro");
		assertEquals(5, terms.size());
		assertEquals("sonata", terms.get(0));
		assertEquals("violin", terms.get(1));
		assertEquals("cello", terms.get(2));
		assertEquals("i", terms.get(3));
		assertEquals("allegro", terms.get(4));

		// artist
		terms = AnalyzerUtil.toTermString(analyzer, "_ID3_ARTIST_ Sarah Walker/Nash Ensemble");
		assertEquals(5, terms.size());
		assertEquals("id3_artist", terms.get(0));
		assertEquals("sarah", terms.get(1));
		assertEquals("walker", terms.get(2));
		assertEquals("nash", terms.get(3));
		assertEquals("ensemble", terms.get(4));

		// album
		terms = AnalyzerUtil.toTermString(analyzer, "_ID3_ALBUM_ Ravel - Chamber Music With Voice");
		assertEquals(5, terms.size());
		assertEquals("id3_album", terms.get(0));
		assertEquals("ravel", terms.get(1));
		assertEquals("chamber", terms.get(2));
		assertEquals("music", terms.get(3));
		assertEquals("voice", terms.get(4));

		/*
		 * /MEDIAS/Music/_DIR_ Ravel/_DIR_ Ravel - Chamber Music With Voice
		 * /01 - Sonata Violin & Cello I. Allegro.ogg
		 */

		// title
		terms = AnalyzerUtil.toTermString(analyzer, "Sonata Violin & Cello II. Tres Vif");
		assertEquals(6, terms.size());
		assertEquals("sonata", terms.get(0));
		assertEquals("violin", terms.get(1));
		assertEquals("cello", terms.get(2));
		assertEquals("ii", terms.get(3));
		assertEquals("tres", terms.get(4));
		assertEquals("vif", terms.get(5));

	}

	public void testHyphen() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "FULL-WIDTH.");
		assertEquals(2, terms.size());
		assertEquals("full", terms.get(0));// divided position
		assertEquals("width", terms.get(1));
	}

	public void testSymbol() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "!\"#$%&\'()=~|-^\\@[`{;:]+*},.///\\<>?_\'");
		assertEquals(0, terms.size());// remove symbols
	}

	public void testHalfWidth() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "THIS IS HALF-WIDTH SENTENCES.");
		assertEquals(3, terms.size());
		assertEquals("half", terms.get(0));// all lowercase letters
		assertEquals("width", terms.get(1));
		assertEquals("sentences", terms.get(2));
	}

	public void testFullWidth() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "ＴＨＩＳ　ＩＳ　ＦＵＬＬ－ＷＩＤＴＨ　ＳＥＮＴＥＮＣＥＳ.");
		assertEquals(5, terms.size());
		assertEquals("this", terms.get(0));// removal target is ignored
		assertEquals("is", terms.get(1));
		assertEquals("full", terms.get(2));
		assertEquals("width", terms.get(3));
		assertEquals("sentences", terms.get(4));
	}

	public void testPossessiveCase() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "This is Airsonic's analysis.");
		assertEquals(2, terms.size());
		assertEquals("airsonic", terms.get(0));// removal of apostrophes
		assertEquals("analysis", terms.get(1));
	}

	public void testPastParticiple() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer,
				"This is formed with a form of the verb \"have\" and a past participl.");
		assertEquals(6, terms.size());
		assertEquals("formed", terms.get(0));// leave passive / not "form"
		assertEquals("form", terms.get(1));
		assertEquals("verb", terms.get(2));
		assertEquals("have", terms.get(3));
		assertEquals("past", terms.get(4));
		assertEquals("participl", terms.get(5));
	}

	public void testNumeral() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "books boxes cities leaves men glasses");
		assertEquals(6, terms.size());
		assertEquals("books", terms.get(0));// leave numeral / not singular
		assertEquals("boxes", terms.get(1));
		assertEquals("cities", terms.get(2));
		assertEquals("leaves", terms.get(3));
		assertEquals("men", terms.get(4));
		assertEquals("glasses", terms.get(5));
	}

	public void testNumbers() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "Olympic Games in 2020.");
		assertEquals(3, terms.size());
		assertEquals("olympic", terms.get(0));
		assertEquals("games", terms.get(1));
		assertEquals("2020", terms.get(2));// numbers are not removed
	}

	public void testAsianCharacters() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "大丈夫");
		assertEquals(3, terms.size());
		assertEquals("大", terms.get(0));// When separating by white space is impossible / Not alphabet
		assertEquals("丈", terms.get(1));
		assertEquals("夫", terms.get(2));
	}

	/*
	 * From here only observing the current situation. Basically, rounding by ICU4J
	 * is necessary.
	 */

	public void testGreekAcute() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "ΆάΈέΉήΊίΐΌόΎύΰϓΏώ");
		assertEquals(1, terms.size());// may be difficult
		// assertEquals("?????????????????", terms.get(0));
	}

	public void testGreekAcute2() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer,
				Normalizer.normalize("ΆάΈέΉήΊίΐΌόΎύΰϓΏώ", java.text.Normalizer.Form.NFC));
		assertEquals(1, terms.size());// may be difficult
		// assertEquals("?????????????????", terms.get(0));
	}

	public void testCyrillicAcute() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "ЃѓЌќ");
		assertEquals(1, terms.size());// may be difficult
		// assertEquals("????", terms.get(0));
	}

	public void testLatinAcuteFailPattern() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "ÁáĀāǺǻĄą Ćć Ć̣ć̣");
		assertEquals(4, terms.size());// may be difficult
		assertEquals("aaaaaaaa", terms.get(0));
		assertEquals("cc", terms.get(1));
		assertEquals("c", terms.get(2));
		assertEquals("c", terms.get(3));
	}

	public void testLatinAcuteSuccessPattern() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "Céline");
		assertEquals(1, terms.size());// no problem depending on how to input
		assertEquals("celine", terms.get(0));
	}

	public void testLigature() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "a æ e");
		assertEquals(2, terms.size());// may be difficult
		assertEquals("ae", terms.get(0));
		assertEquals("e", terms.get(1));
	}

}