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

/*
 * Unlike Subsonic, Jpsonic uses character separation of UAX#29.
 * Also, due to the importance of query search, the stop word covers only articles.
 */
public class SearchServiceAnalyzerTestCase extends TestCase {

	@Resource
	private SearchService searchService;

	private Analyzer analyzer;

	@Override
	public void setUp() {
		analyzer = AnalyzerUtil.createJpsonicAnalyzer(searchService);
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
		assertEquals(7, terms.size());
		/*
		 * (lucene 3.0)
		 * id3_artist
		 * 
		 * (UAX#29)
		 * id
		 * 3
		 * artist
		 */
		assertEquals("id", terms.get(0));
		assertEquals("3", terms.get(1));
		assertEquals("artist", terms.get(2));
		assertEquals("celine", terms.get(3));
		assertEquals("frisch", terms.get(4));
		assertEquals("cafe", terms.get(5));
		assertEquals("zimmermann", terms.get(6));

		// album
		terms = AnalyzerUtil.toTermString(analyzer, "_ID3_ALBUM_ Bach: Goldberg Variations, Canons [Disc 1]");
		assertEquals(9, terms.size());
		/*
		 * (lucene 3.0)
		 * id3_album
		 * 
		 * (UAX#29)
		 * id
		 * 3
		 * album
		 */
		assertEquals("id", terms.get(0));
		assertEquals("3", terms.get(1));
		assertEquals("album", terms.get(2));
		assertEquals("bach", terms.get(3));
		assertEquals("goldberg", terms.get(4));
		assertEquals("variations", terms.get(5));
		assertEquals("canons", terms.get(6));
		assertEquals("disc", terms.get(7));
		assertEquals("1", terms.get(8));

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
		assertEquals(7, terms.size());
		/*
		 * (lucene 3.0)
		 * id3_artist
		 * 
		 * (UAX#29)
		 * id
		 * 3
		 * artist
		 */
		assertEquals("id", terms.get(0));
		assertEquals("3", terms.get(1));
		assertEquals("artist", terms.get(2));
		assertEquals("sarah", terms.get(3));
		assertEquals("walker", terms.get(4));
		assertEquals("nash", terms.get(5));
		assertEquals("ensemble", terms.get(6));

		// album
		terms = AnalyzerUtil.toTermString(analyzer, "_ID3_ALBUM_ Ravel - Chamber Music With Voice");
		assertEquals(8, terms.size());
		/*
		 * (lucene 3.0)
		 * id3_album
		 * 
		 * (UAX#29)
		 * id
		 * 3
		 * album
		 */
		assertEquals("id", terms.get(0));
		assertEquals("3", terms.get(1));
		assertEquals("album", terms.get(2));
		assertEquals("ravel", terms.get(3));
		assertEquals("chamber", terms.get(4));
		assertEquals("music", terms.get(5));
		assertEquals("with", terms.get(6));// currently not stopward
		assertEquals("voice", terms.get(7));

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
		assertEquals(5, terms.size());
		assertEquals("this", terms.get(0));// currently not stopward
		assertEquals("is", terms.get(1));// currently not stopward
		assertEquals("half", terms.get(2));
		assertEquals("width", terms.get(3));
		assertEquals("sentences", terms.get(4));
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

	/* air -> jp
	 * (lucene 3.0 -> lucene 3.1 and above)
	 */
	public void testPossessiveCase() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "This is Airsonic's analysis.");
		assertEquals(5, terms.size());
		/*
		 * (lucene 3.0)
		 * airsonic
		 * 
		 * (UAX#29)
		 * airsonic
		 * s
		 */
		assertEquals("this", terms.get(0));// currently not stopward
		assertEquals("is", terms.get(1));// currently not stopward
		assertEquals("airsonic", terms.get(2));// removal of apostrophes
		assertEquals("s", terms.get(3)); // apostrophes is a delimiter and is not filtered. , "s" remain.
		assertEquals("analysis", terms.get(4));
	}

	public void testPastParticiple() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer,
				"This is formed with a form of the verb \"have\" and a past participl.");
		assertEquals(11, terms.size());
		assertEquals("this", terms.get(0));// currently not stopward
		assertEquals("is", terms.get(1));// currently not stopward
		assertEquals("formed", terms.get(2));// leave passive / not "form"
		assertEquals("with", terms.get(3));// currently not stopward
		assertEquals("form", terms.get(4));
		assertEquals("of", terms.get(5));// currently not stopward
		assertEquals("verb", terms.get(6));
		assertEquals("have", terms.get(7));
		assertEquals("and", terms.get(8));// currently not stopward
		assertEquals("past", terms.get(9));
		assertEquals("participl", terms.get(10));
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
		assertEquals(4, terms.size());
		assertEquals("olympic", terms.get(0));
		assertEquals("games", terms.get(1));
		assertEquals("in", terms.get(2));// currently not stopward
		assertEquals("2020", terms.get(3));// numbers are not removed
	}

	/*
	 * air -> jp
	 * The case is changed because syntax processing of kanji is possible.
	 * However, because it is a Japanese parsing analysis, it may be crowded with Chinese. 
	 */
	public void testAsianCharacters() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "大丈夫");
		assertEquals(1, terms.size());
		assertEquals("大丈夫", terms.get(0));
	}

	/*
	 * An example of correct Japanese analysis.
	 */
	public void testJapaneseCharacters() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "日本語は大丈夫");
		assertEquals(2, terms.size());
		assertEquals("日本語", terms.get(0));
		assertEquals("大丈夫", terms.get(1));
	}

	/*
	 * air -> jp
	 * Airsonic removes all common stops.
	 * Jpsonic removes only articles. 
	 */
	public void testStopward() {
		List<String> terms = AnalyzerUtil.toTermString(analyzer, "a an the el la los las le les");
		assertEquals(0, terms.size());
		terms = AnalyzerUtil.toTermString(analyzer,
				"and are as at be but by for if in into is it no not of on or such that their then there these they this to was will with");
		assertEquals(30, terms.size());
	}

	/*
	 * From here only observing the current situation.
	 * Basically, rounding by ICU4J is necessary.
	 * Dedicated parsing is required for complete processing.
	 * 
	 * In reality, as Jpsonic is doing,
	 * I think that it is bette
	 * to use extension tags that are useful for analysis apart
	 * from the display character string.
	 *  
	 *  ex)Julius Cæsar
	 *  
	 *  Register "Julius Caesar" in the SORT tag
	 *  and use it for searching and indexing.
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