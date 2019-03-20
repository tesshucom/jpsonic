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
 along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2019 (C) tesshu.com
 */
package com.tesshu.jpsonic.service.search;

import com.tesshu.jpsonic.service.search.IndexType.FieldNames;

import org.airsonic.player.domain.MediaFile.MediaType;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.airsonic.player.domain.SearchCriteria;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.tesshu.jpsonic.service.search.IndexType.normalizeGenre;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Factory class of Lucene Query.
 * This class is a division of what was once part of SearchService
 * and added functionality.
 **/
/*
 * Generate queries that have roughly the same meaning as old queries.
 * However, the implementation version differs significantly from legacy services.
 * Improvements have been made to 
 * "parts with minor differences in API" and
 * "query that is originally unsuitable for specification".
 */
public class QueryFactory {

	private QueryFactory() {
	}

	public static Query createQuery(SearchCriteria criteria, List<MusicFolder> musicFolders, IndexType indexType) {

		String[] targetFields = Arrays.stream(indexType.getFields())
				.filter(f -> !f.equals(FieldNames.FOLDER))
				.toArray(s -> new String[s]);
		
		MultiFieldQueryParser parser =
		    new MultiFieldQueryParser(targetFields, AnalyzerFactory.getInstance().getAnalyzer());

		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		try {
			builder.add(parser.parse(criteria.getQuery()), Occur.MUST);
		} catch (ParseException e) {
			LoggerFactory.getLogger(QueryFactory.class).error("Error during query analysis.", e);
		}

    BooleanQuery.Builder sub = new BooleanQuery.Builder();
    musicFolders.forEach(musicFolder -> {
      if (indexType == IndexType.ALBUM_ID3 || indexType == IndexType.ARTIST_ID3) {
        sub.add(new TermQuery(new Term(FieldNames.FOLDER_ID, musicFolder.getId().toString())), Occur.SHOULD);
      } else {
        sub.add(new TermQuery(new Term(FieldNames.FOLDER, musicFolder.getPath().getPath())), Occur.SHOULD);
      }
    });

    return builder.add(sub.build(), Occur.MUST).build();

	}

	public static Query createQuery(RandomSearchCriteria criteria) {

		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		builder.add(new TermQuery(new Term(FieldNames.MEDIA_TYPE, MediaType.MUSIC.name().toLowerCase())), Occur.MUST);

		if (!isEmpty(criteria.getGenre())) {
			builder.add(new TermQuery(
					new Term(FieldNames.GENRE, normalizeGenre.apply(criteria.getGenre()))), Occur.MUST);
		}

		if(!(isEmpty(criteria.getFromYear()) && isEmpty(criteria.getToYear()))) {
			builder.add(IntPoint.newRangeQuery(
					FieldNames.YEAR,
					isEmpty(criteria.getFromYear()) ? Integer.MIN_VALUE : criteria.getFromYear(),
					isEmpty(criteria.getToYear()) ? Integer.MAX_VALUE : criteria.getToYear()),
					Occur.MUST);
		}

		BooleanQuery.Builder sub = new BooleanQuery.Builder();
		criteria.getMusicFolders().forEach(musicFolder -> 
		sub.add(new TermQuery(new Term(
		      FieldNames.FOLDER,
		      musicFolder.getPath().getPath())),
		      Occur.SHOULD));

		return builder.add(sub.build(), Occur.MUST).build();
	}

	public static Query searchByName(String name, String field) {
		QueryParser queryParser = new QueryParser(field, AnalyzerFactory.getInstance().getAnalyzer());
		queryParser.setDefaultOperator(QueryParser.Operator.AND);
		Query parsedQuery = null;
		    parsedQuery = queryParser.createBooleanQuery(field, QueryParser.escape(name), Occur.MUST);
		return parsedQuery;
	}

	public static Query searchRandomAlbum(List<MusicFolder> musicFolders) {
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    BooleanQuery.Builder sub = new BooleanQuery.Builder();
    musicFolders.forEach(musicFolder -> 
    sub.add(new TermQuery(new Term(
          FieldNames.FOLDER,
          musicFolder.getPath().getPath())),
          Occur.SHOULD));
    return builder.add(sub.build(), Occur.MUST).build();
	}

	public static Query searchRandomAlbumId3(List<MusicFolder> musicFolders) {
	  BooleanQuery.Builder builder = new BooleanQuery.Builder();
	  BooleanQuery.Builder sub = new BooleanQuery.Builder();
	  musicFolders.forEach(musicFolder -> 
	    sub.add(new TermQuery(new Term(
	        FieldNames.FOLDER_ID,
	        musicFolder.getId().toString())),
	        Occur.SHOULD));
	    return builder.add(sub.build(), Occur.MUST).build();
	}

}
