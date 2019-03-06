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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.airsonic.player.service.SearchService;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzerUtil {

	private static final Logger LOG = LoggerFactory.getLogger(AnalyzerUtil.class);

	private AnalyzerUtil() {
	}

	public static List<String> toTermString(Analyzer analyzer, String str) {
		List<String> result = new ArrayList<>();
		try {
			TokenStream stream = analyzer.tokenStream(null, new StringReader(str));
			stream.reset();
			while (stream.incrementToken()) {
				result.add(stream.getAttribute(TermAttribute.class).term());
			}
		} catch (IOException e) {
			LOG.error("Error during Token processing.", e);
		}
		return result;
	}

	public static Analyzer createAirsonicAnalyzer(SearchService serviceInstance) {
		try {
			ClassLoader loader = ClassLoader.getSystemClassLoader();
			Class<?> innerClazz = loader.loadClass("org.airsonic.player.service.SearchService$CustomAnalyzer");
			Constructor<?> constructor = innerClazz.getDeclaredConstructors()[0];
			constructor.setAccessible(true);
			Analyzer analyzer;
			analyzer = (Analyzer) constructor.newInstance(serviceInstance, null);
			return analyzer;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			LOG.error("Error when initializing Analyzer.", e);
		}
		return null;
	}

}
