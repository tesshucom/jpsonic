/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2018 tesshucom
 */

package com.tesshu.jpsonic.service.search;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import com.tesshu.jpsonic.domain.Album;
import com.tesshu.jpsonic.domain.Artist;
import com.tesshu.jpsonic.domain.MediaFile;
import com.tesshu.jpsonic.domain.MediaFile.MediaType;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaLexer;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaListener;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.AsteriskContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.BaseNameContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.BasePropertiesContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.BinOpContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.ClassNameContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.ClassRelExpContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.DQuoteContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.Def_returnContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.DerivedNameContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.ExistsOpContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.FormFeedContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.HTabContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.LineFeedContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.LinksToContainersContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.LogOpContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.ParseContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.PeopleInvolvedContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.PropertyBooleanValueContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.PropertyContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.PropertyExpContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.PropertyStringValueContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.RelOpContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.SearchCritContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.SearchExpContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.ShortNameContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.SpaceContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.StringOpContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.VTabContext;
import com.tesshu.jpsonic.service.upnp.UPnPSearchCriteriaParser.WCharContext;
import com.tesshu.jpsonic.service.upnp.processor.UpnpProcessorUtil;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Director class for use Lucene's QueryBuilder at the same time as UPnP message parsing.
 */
/*
 * Anltl4 syntax analysis class is automatically generated with reference to Service Template Version 1.01 (For UPnP
 * Version 1.0). Therefore, at this stage, this class has many redundant skeleton methods.
 */
public class UPnPSearchCriteriaDirector implements UPnPSearchCriteriaListener {

    private static final Logger LOG = LoggerFactory.getLogger(UPnPSearchCriteriaDirector.class);

    private final QueryFactory queryFactory;
    private final UpnpProcessorUtil upnpUtil;

    private BooleanQuery.Builder mediaTypeQueryBuilder;
    private BooleanQuery.Builder propExpQueryBuilder;
    private Occur lastLogOp;
    private Class<?> assignableClass;
    private int offset;
    private int count;
    private String upnpSearchQuery;
    private UPnPSearchCriteria result;

    private final BiConsumer<Boolean, String> notice = (b, message) -> {
        if (b) {
            LOG.warn("The entered query may have a grammatical error. Reason:{}", message);
        }
    };

    private static final List<String> UNSUPPORTED_CLASS = Arrays.asList("object.container.album.photoAlbum",
            "object.container.playlistContainer", "object.container.genre", "object.container.genre.musicGenre",
            "object.container.genre.movieGenre", "object.container.storageSystem", "object.container.storageVolume",
            "object.container.storageFolder");

    public UPnPSearchCriteriaDirector(QueryFactory queryFactory, UpnpProcessorUtil util) {
        this.queryFactory = queryFactory;
        this.upnpUtil = util;
    }

    public UPnPSearchCriteria construct(int offset, int count, String upnpSearchQuery) {
        this.offset = offset;
        this.count = count;
        this.upnpSearchQuery = upnpSearchQuery;
        UPnPSearchCriteriaLexer lexer = new UPnPSearchCriteriaLexer(CharStreams.fromString(upnpSearchQuery));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        UPnPSearchCriteriaParser parser = new UPnPSearchCriteriaParser(stream);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, parser.parse());
        return result;
    }

    @Override
    public void enterAsterisk(AsteriskContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterBaseName(BaseNameContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterBaseProperties(BasePropertiesContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterBinOp(BinOpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterClassName(ClassNameContext ctx) {
        // Nothing is currently done.
    }

    private void addMediaTypeQuery(String fieldName, String mediaType, Occur occur) {
        if (!isEmpty(mediaTypeQueryBuilder)) {
            mediaTypeQueryBuilder.add(new TermQuery(new Term(fieldName, mediaType)), occur);
        }
    }

    /**
     * 7. Appendix C - AV Working Committee Class Definitions
     */
    @Override
    public void enterClassRelExp(ClassRelExpContext ctx) {
        List<ParseTree> children = ctx.children.stream().filter(p -> !isBlank(p.getText())).collect(toList());
        notice.accept(3 != children.size(), "The number of child elements of ClassRelExp is incorrect.");
        final String subject = children.get(0).getText();
        final String verb = children.get(1).getText();
        final String complement = children.get(2).getText();

        if (UNSUPPORTED_CLASS.contains(complement)) {
            throw createIllegal("The current version does not support searching for this class.", subject, verb,
                    complement);
        }

        if (complement.startsWith("object.item.audioItem") || complement.startsWith("object.item.videoItem")) {
            mediaTypeQueryBuilder = new BooleanQuery.Builder();
        }

        if ("derivedfrom".equals(verb)) {
            purseDerivedfrom(subject, verb, complement);
        } else if ("=".equals(verb)) {
            purseClass(subject, verb, complement);
        }
    }

    private void purseDerivedfrom(String subject, String verb, String complement) {
        switch (complement) {

        // artist
        case "object.container.person":
        case "object.container.person.musicArtist":
            assignableClass = Artist.class;
            break;

        // album
        case "object.container.album":
        case "object.container.album.musicAlbum":
            assignableClass = Album.class;
            break;

        // song
        case "object.item.audioItem.musicTrack":
            assignableClass = MediaFile.class;
            addMediaTypeQuery(FieldNamesConstants.MEDIA_TYPE, MediaType.MUSIC.name(), Occur.SHOULD);
            break;

        // audio
        case "object.item.audioItem":
            assignableClass = MediaFile.class;
            addMediaTypeQuery(FieldNamesConstants.MEDIA_TYPE, MediaType.MUSIC.name(), Occur.SHOULD);
            addMediaTypeQuery(FieldNamesConstants.MEDIA_TYPE, MediaType.PODCAST.name(), Occur.SHOULD);
            addMediaTypeQuery(FieldNamesConstants.MEDIA_TYPE, MediaType.AUDIOBOOK.name(), Occur.SHOULD);
            break;

        // video
        case "object.item.videoItem":
            assignableClass = MediaFile.class;
            addMediaTypeQuery(FieldNamesConstants.MEDIA_TYPE, MediaType.VIDEO.name(), Occur.MUST);
            break;

        default:
            throw createIllegal("An unknown class was specified.", subject, verb, complement);
        }
    }

    private void purseClass(String subject, String verb, String complement) {
        switch (complement) {

        // artist
        case "object.container.person.musicArtist":
            assignableClass = Artist.class;
            break;

        // album
        case "object.container.album.musicAlbum":
            assignableClass = Album.class;
            break;

        // audio
        case "object.item.audioItem.musicTrack":
            assignableClass = MediaFile.class;
            addMediaTypeQuery(FieldNamesConstants.MEDIA_TYPE, MediaType.MUSIC.name(), Occur.SHOULD);
            break;
        case "object.item.audioItem.audioBroadcast":
            assignableClass = MediaFile.class;
            addMediaTypeQuery(FieldNamesConstants.MEDIA_TYPE, MediaType.PODCAST.name(), Occur.SHOULD);
            break;
        case "object.item.audioItem.audioBook":
            assignableClass = MediaFile.class;
            addMediaTypeQuery(FieldNamesConstants.MEDIA_TYPE, MediaType.AUDIOBOOK.name(), Occur.SHOULD);
            break;

        // video
        case "object.item.videoItem.movie":
        case "object.item.videoItem.videoBroadcast":
        case "object.item.videoItem.musicVideoClip":
            assignableClass = MediaFile.class;
            addMediaTypeQuery(FieldNamesConstants.MEDIA_TYPE, MediaType.VIDEO.name(), Occur.MUST);
            break;

        default:
            throw createIllegal(
                    "An insufficient class hierarchy from derivedfrom or a class not supported by the server was specified.",
                    subject, verb, complement);
        }
    }

    @Override
    public void enterDef_return(Def_returnContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterDerivedName(DerivedNameContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterDQuote(DQuoteContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterExistsOp(ExistsOpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterFormFeed(FormFeedContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterHTab(HTabContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterLineFeed(LineFeedContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterLinksToContainers(LinksToContainersContext ctx) {
        // Nothing is currently done.
    }

    /*
     * 2.5.5.1. SearchCriteria String Syntax
     */
    @Override
    public void enterLogOp(LogOpContext ctx) {
        if ("and".equals(ctx.getText())) {
            lastLogOp = Occur.MUST;
        } else if ("or".equals(ctx.getText())) {
            lastLogOp = Occur.SHOULD;
        }
    }

    @Override
    public void enterParse(ParseContext ctx) {
        propExpQueryBuilder = new BooleanQuery.Builder();
    }

    @Override
    public void enterPeopleInvolved(PeopleInvolvedContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterProperty(PropertyContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterPropertyBooleanValue(PropertyBooleanValueContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterPropertyExp(PropertyExpContext ctx) {

        List<ParseTree> children = ctx.children.stream().filter(p -> !isBlank(p.getText())).collect(toList());
        notice.accept(3 != children.size(), "The number of child elements of ClassRelExp is incorrect.");
        final String subject = children.get(0).getText();
        final String complement = children.get(2).getText();
        List<String> fieldName = new ArrayList<>();

        if ("dc:title".equals(subject)) {
            if (Album.class == assignableClass) {
                fieldName.add(FieldNamesConstants.ALBUM);
                fieldName.add(FieldNamesConstants.ALBUM_READING);
            } else if (Artist.class == assignableClass) {
                fieldName.add(FieldNamesConstants.ARTIST);
                fieldName.add(FieldNamesConstants.ARTIST_READING);
            } else {
                fieldName.add(FieldNamesConstants.TITLE);
                fieldName.add(FieldNamesConstants.TITLE_READING);
            }
        } else if ("upnp:artist".equals(subject)) {
            fieldName.add(FieldNamesConstants.ARTIST);
            fieldName.add(FieldNamesConstants.ARTIST_READING);
        } else if ("dc:creator".equals(subject)) {
            fieldName.add(FieldNamesConstants.COMPOSER);
            fieldName.add(FieldNamesConstants.COMPOSER_READING);
        }
        notice.accept(0 == fieldName.size(), "Unexpected PropertyExpContext. -> " + subject);

        try {
            Query query = createMultiFieldQuery(fieldName.toArray(new String[0]), complement);
            if (!isEmpty(query)) {
                propExpQueryBuilder.add(query, isEmpty(lastLogOp) ? Occur.SHOULD : lastLogOp);
            }
        } catch (IOException e) {
            LOG.error("Failure when generating MultiFieldQuery : ", e);
        }

    }

    @Override
    public void enterPropertyStringValue(PropertyStringValueContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterRelOp(RelOpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterSearchCrit(SearchCritContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterSearchExp(SearchExpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterShortName(ShortNameContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterSpace(SpaceContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterStringOp(StringOpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterVTab(VTabContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void enterWChar(WCharContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitAsterisk(AsteriskContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitBaseName(BaseNameContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitBaseProperties(BasePropertiesContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitBinOp(BinOpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitClassName(ClassNameContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitClassRelExp(ClassRelExpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitDef_return(Def_returnContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitDerivedName(DerivedNameContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitDQuote(DQuoteContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitExistsOp(ExistsOpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitFormFeed(FormFeedContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitHTab(HTabContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitLineFeed(LineFeedContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitLinksToContainers(LinksToContainersContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitLogOp(LogOpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitParse(ParseContext ctx) {

        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();

        // prop
        mainQuery.add(propExpQueryBuilder.build(), Occur.MUST);

        // mediatype
        if (!isEmpty(mediaTypeQueryBuilder)) {
            mainQuery.add(mediaTypeQueryBuilder.build(), Occur.MUST);
        }

        // folder
        IndexType t = getIndexType();
        boolean isId3 = t == IndexType.ALBUM_ID3 || t == IndexType.ARTIST_ID3;
        Query folderQuery = queryFactory.toFolderQuery.apply(isId3, upnpUtil.getGuestMusicFolders());
        mainQuery.add(folderQuery, Occur.MUST);

        result = new UPnPSearchCriteria(upnpSearchQuery, offset, count);
        result.setAssignableClass(assignableClass);
        result.setParsedQuery(mainQuery.build());

    }

    @Override
    public void exitPeopleInvolved(PeopleInvolvedContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitProperty(PropertyContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitPropertyBooleanValue(PropertyBooleanValueContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitPropertyExp(PropertyExpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitPropertyStringValue(PropertyStringValueContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitRelOp(RelOpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitSearchCrit(SearchCritContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitSearchExp(SearchExpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitShortName(ShortNameContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitSpace(SpaceContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitStringOp(StringOpContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitVTab(VTabContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void exitWChar(WCharContext ctx) {
        // Nothing is currently done.
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        // Nothing is currently done.
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        // Nothing is currently done.
    }

    private IllegalArgumentException createIllegal(String message, String subject, String verb, String complement) {
        return new IllegalArgumentException(
                message.concat(" : ").concat(subject).concat(SPACE).concat(verb).concat(SPACE).concat(complement));
    }

    private Query createMultiFieldQuery(final String[] fields, final String query) throws IOException {
        return queryFactory.createPhraseQuery(fields, query, getIndexType());
    }

    private IndexType getIndexType() {
        if (MediaFile.class == assignableClass) {
            return IndexType.SONG;
        } else if (Artist.class == assignableClass) {
            return IndexType.ARTIST_ID3;
        } else if (Album.class == assignableClass) {
            return IndexType.ALBUM_ID3;
        }
        return null;
    }

}
