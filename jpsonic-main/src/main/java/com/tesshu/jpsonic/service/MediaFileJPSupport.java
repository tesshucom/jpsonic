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
package com.tesshu.jpsonic.service;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.ibm.icu.text.Transliterator;

import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;

/**
 * Provide analysis of Japanese name.
 * @author tesshu
 */
@Service
public class MediaFileJPSupport {
    
    public static final Pattern ALPHA = Pattern.compile("^[a-zA-Z]+$");
    private static final Pattern KATAKANA = Pattern.compile("^[\\u30A0-\\u30FF]+$");
    private static final String ASTER = "*";
    private final Tokenizer tokenizer = new Tokenizer();

    /** Determine reading for token. */
    private final Function<Token, String> readingAnalysis = token -> {
    	if(KATAKANA.matcher(token.getSurface()).matches()
    			|| ALPHA.matcher(token.getSurface()).matches()
    			|| ASTER.equals(token.getReading())) {
    		return token.getSurface();
    	}
        return token.getReading();
    };
    
    /**
     * When first letter of MediaFile#artist is other than alphabet,
     * try reading Japanese and set MediaFile#artistReading.
     * @param mediaFile
     */
	public void analyzeArtistReading(MediaFile mediaFile) {
		String artist = mediaFile.getArtist();
		mediaFile.setArtistReading(createReading(artist));
	}

	public void analyzeNameReading(Artist artist) {
		String name = artist.getName();
		artist.setReading(createReading(name));
	}

	public String createReading(String s) {
		if (StringUtils.isEmpty(s) || ALPHA.matcher(s.substring(0, 1)).matches()) {
			return null;
		}
		Collector<String, StringBuilder, String> join =
				Collector.of(StringBuilder::new, StringBuilder::append, StringBuilder::append, StringBuilder::toString);
		List<Token> tokens = tokenizer.tokenize(s);
		return tokens.stream().map(readingAnalysis).collect(join);
	}
    
    /**
     * This method returns the normalized Artist name that can also be used to create the index prefix.
     * @param mediaFile
     * @return indexable Name
     */
    public String createIndexableName(MediaFile mediaFile) {
        // http://www.unicode.org/reports/tr15/
    	if(!StringUtils.isEmpty(mediaFile.getArtistSort())) {
    		return Normalizer.normalize(mediaFile.getArtistSort(), Normalizer.Form.NFD);
    	}
    	if(!StringUtils.isEmpty(mediaFile.getArtistReading())) {
    		return Normalizer.normalize(mediaFile.getArtistReading(), Normalizer.Form.NFD);
    	}
        return mediaFile.getName();
    }    
    
	public String createIndexableName(Artist artist) {
		if (!StringUtils.isEmpty(artist.getSort())) {
			return Normalizer.normalize(artist.getSort(), Normalizer.Form.NFD);
		}
		if (!StringUtils.isEmpty(artist.getReading())) {
			return Normalizer.normalize(artist.getReading(), Normalizer.Form.NFD);
		}
		return artist.getName();
	}
    
    public List<MediaFile> createArtistSortToBeUpdate(List<MediaFile> candidates) {
    	List<MediaFile> toBeUpdate = new ArrayList<>();
    	for(MediaFile candidate : candidates) {
    		if(!StringUtils.isEmpty(candidate.getArtistReading())) {
        		String cleanedUpSort = cleanUp(candidate.getArtistSort());
        		if(!candidate.getArtistReading().equals(cleanedUpSort)) {
        			candidate.setArtistSort(cleanedUpSort);
        			toBeUpdate.add(candidate);
        		}
    		}
    	}
    	return toBeUpdate;
    }
    
    public String cleanUp(String dirty) {
    	return Normalizer.normalize(
    			Transliterator.getInstance("Hiragana-Katakana").transliterate(dirty), Normalizer.Form.NFKC);
    }

}
