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
 * Provide analysis of Japanese artist name.
 * @author tesshu
 */
@Service
public class MediaFileJPSupport {
    
    private static final Pattern KATAKANA = Pattern.compile("^[\\u30A0-\\u30FF]+$");
    private static final Pattern ALPHA = Pattern.compile("^[a-zA-Z]+$");
    private static final String ASTER = "*";

    /**
     * Determine reading for token.
     * 
     * In case of katakana, return katakana.
     * Alphabets or character strings that can not be analyzed are returned as they are.
     * As a result Hiragana and Kanji return katakana.
     */
    private final Function<Token, String> readingAnalysis = token -> {
        String reading =
                KATAKANA.matcher(token.getSurface()).matches()
                    ? token.getSurface()
                    : ALPHA.matcher(token.getSurface()).matches()
                        ? token.getSurface()
                        : ASTER.equals(token.getReading())
                            ? token.getSurface()
                            : token.getReading();
        return reading;
    };
    
    private final Tokenizer tokenizer = new Tokenizer();
    
    /**
     * When first letter of MediaFile#artist is other than alphabet,
     * try reading Japanese and set MediaFile#artistReading.
     * @param mediaFile
     */
    public void setReading(MediaFile mediaFile){
        String artist = mediaFile.getArtist();
        if(null == artist || 0 == mediaFile.getName().length()){
            
        }
        
        /*
         * Originally classified as # Artist.
         * In some cases other than Japanese is included, but it is ignored as a result.
         */
        if ( !ALPHA.matcher(artist.substring(0, 1)).matches()) {
            Collector<String, StringBuilder, String> join =
                    Collector.of(StringBuilder::new, StringBuilder::append, StringBuilder::append, StringBuilder::toString);
            
            /*
             * Split into tokens.
             * The content depends on the accuracy of the morphological analysis engine.
             */
            List<Token> tokens = tokenizer.tokenize(artist);

            /*
             * Unify tokens string as much as possible in katakana.
             * (To compare with katakana code points and sort)
             * This analysis is not aimed at analyzing alphabets and symbols.
             * Logic for index rather than speech replacement.
             */
            String reading = tokens.stream().map(readingAnalysis).collect(join);

            mediaFile.setArtistReading(reading);
        }
        
    }
    
    /**
     * The reading of Japanese characters is classified
     * as clear sound, voiced sound, semi-voiced sound.
     * 
     * Voiced sound, semi-voiced sound is represented
     * by the combination of clear sound and special symbol on UNICODE.
     * 
     * Japanese indexes are usually created based on the clear sound
     * from which special symbols have been removed.
     * 
     * In "Reading", character codes of clear sound,
     * voiced sound, semi - voiced sound are mixed.
     * 
     * This method returns the normalized Artist name
     * that can also be used to create the index prefix.
     * 
     * @param mediaFile
     * @return indexable Name
     */
    public String createIndexableName(MediaFile mediaFile) {
        // http://www.unicode.org/reports/tr15/
    	if(null != mediaFile.getArtistSort()) {
    		return Normalizer.normalize(mediaFile.getArtistSort(), Normalizer.Form.NFD);
    	}
    	if(null != mediaFile.getArtistReading()) {
    		return Normalizer.normalize(mediaFile.getArtistReading(), Normalizer.Form.NFD);
    	}
        return mediaFile.getName();
    }    
    
    public String createIndexableName(Artist artist) {
        // http://www.unicode.org/reports/tr15/
        return artist.getSort() == null
                ? artist.getName()
                : Normalizer.normalize(artist.getSort(), Normalizer.Form.NFD);
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
    
    private String cleanUp(String dirty) {
    	return Normalizer.normalize(
    			Transliterator.getInstance("Hiragana-Katakana").transliterate(dirty), Normalizer.Form.NFKC);
    }

}
