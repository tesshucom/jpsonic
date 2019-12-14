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
package org.airsonic.player.service.search;

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
import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.Artist;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.service.SettingsService;
import org.airsonic.player.service.search.lucene.UPnPSearchCriteria;
import org.airsonic.player.service.upnp.processor.UpnpProcessorUtil;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Director class for use Lucene's QueryBuilder at the same time as UPnP message
 * parsing.
 */
/*
 * Anltl4 syntax analysis class is automatically generated with reference to
 * Service Template Version 1.01 (For UPnP Version 1.0). Therefore, at this
 * stage, this class has many redundant skeleton methods.
 */
@Component
@Scope("prototype")
public class UPnPCriteriaDirector implements UPnPSearchCriteriaListener {

    private static final Logger LOG = LoggerFactory.getLogger(UPnPCriteriaDirector.class);

    private final QueryFactory queryFactory;

    private final UpnpProcessorUtil util;

    private final SettingsService settingsService;

    private BooleanQuery.Builder propExpQueryBuilder;

    private UPnPSearchCriteria criteria;

    private Occur lastLogOp;

    BiConsumer<Boolean, String> notice = (b, message) -> {
        if (b)
            LOG.warn("The entered query may have a grammatical error. Reason:{}", message);
    };

    public UPnPCriteriaDirector(QueryFactory queryFactory, SettingsService settingsService, UpnpProcessorUtil util) {
        this.queryFactory = queryFactory;
        this.settingsService = settingsService;
        this.util = util;
    }

    public UPnPSearchCriteria construct(int offset, int count, String upnpSearchQuery) {
        UPnPSearchCriteriaLexer lexer = new UPnPSearchCriteriaLexer(CharStreams.fromString(upnpSearchQuery));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        UPnPSearchCriteriaParser parser = new UPnPSearchCriteriaParser(stream);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, parser.parse());
        criteria.setOffset(offset);
        criteria.setCount(count);
        criteria.setQuery(upnpSearchQuery);
        return criteria;
    }

    @Override
    public void enterAsterisk(AsteriskContext ctx) {
    }
    
    @Override
    public void enterBaseName(BaseNameContext ctx) {
    }

    @Override
    public void enterBaseProperties(BasePropertiesContext ctx) {
    }

    @Override
    public void enterBinOp(BinOpContext ctx) {
    }

    @Override
    public void enterClassName(ClassNameContext ctx) {
    }

    @Override
    public void enterClassRelExp(ClassRelExpContext ctx) {
        List<ParseTree> children = ctx.children.stream().filter(p -> !isBlank(p.getText())).collect(toList());
        notice.accept(3 != children.size(), "The number of child elements of ClassRelExp is incorrect.");
        final String V = children.get(1).getText();
        final String C = children.get(2).getText();

        criteria = new UPnPSearchCriteria();
        switch (V) {
            case "derivedfrom":
                switch (C) {
                    case "object.item.audioItem":
                        criteria.setAssignableClass(MediaFile.class);
                        break;
                    case "object.item.videoItem":
                        criteria.setAssignableClass(MediaFile.class);
                        break;
                    default:
                        notice.accept(true, "ClassRelExp:Unexpected class specification. -> " + C);
                        break;
                }
                break;
            case "=":
                switch (C) {
                    case "object.container.album.musicAlbum":
                        criteria.setAssignableClass(Album.class);
                        break;
                    case "object.container.person.musicArtist":
                        criteria.setAssignableClass(Artist.class);
                        break;
                    default:
                        notice.accept(true, "ClassRelExp:Unexpected class specification. -> " + C);
                        break;
                }
                break;
            default:
                notice.accept(3 != children.size(), "ClassRelExp:Unexpected binOp.");
                break;
        }

        criteria.setIncludeComposer(settingsService.isSearchComposer());

    }

    @Override
    public void enterDef_return(Def_returnContext ctx) {
    }

    @Override
    public void enterDerivedName(DerivedNameContext ctx) {
    }

    @Override
    public void enterDQuote(DQuoteContext ctx) {
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
    }

    @Override
    public void enterExistsOp(ExistsOpContext ctx) {
    }

    @Override
    public void enterFormFeed(FormFeedContext ctx) {
    }

    @Override
    public void enterHTab(HTabContext ctx) {
    }

    @Override
    public void enterLineFeed(LineFeedContext ctx) {
    }

    @Override
    public void enterLinksToContainers(LinksToContainersContext ctx) {
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
    }

    @Override
    public void enterProperty(PropertyContext ctx) {
    }

    @Override
    public void enterPropertyBooleanValue(PropertyBooleanValueContext ctx) {
    }

    @Override
    public void enterPropertyExp(PropertyExpContext ctx) {

        List<ParseTree> children = ctx.children.stream().filter(p -> !isBlank(p.getText())).collect(toList());
        notice.accept(3 != children.size(), "The number of child elements of ClassRelExp is incorrect.");
        final String S = children.get(0).getText();
        // final String V = children.get(1).getText();
        final String C = children.get(2).getText();

        List<String> fieldName = new ArrayList<String>();

        // BaseProperty
        if ("dc:title".equals(S)) {
            if (Album.class == criteria.getAssignableClass()) {
                fieldName.add(FieldNames.ALBUM_EX);
                fieldName.add(FieldNames.ALBUM);
            } else if (Artist.class == criteria.getAssignableClass()) {
                fieldName.add(FieldNames.ARTIST_READING);
                fieldName.add(FieldNames.ARTIST_EX);
                fieldName.add(FieldNames.ARTIST);
            } else {
                fieldName.add(FieldNames.TITLE_EX);
                fieldName.add(FieldNames.TITLE);
            }
        } else if ("dc:creator".equals(S)) {
            fieldName.add(FieldNames.COMPOSER_READING);
            fieldName.add(FieldNames.COMPOSER);
        } else if ("upnp:artist".equals(S)) {
            fieldName.add(FieldNames.ARTIST_READING);
            fieldName.add(FieldNames.ARTIST_EX);
            fieldName.add(FieldNames.ARTIST);
        }
        notice.accept(0 == fieldName.size(), "Unexpected PropertyExpContext. -> " + S);

        try {
            Query query = createMultiFieldQuery(fieldName.toArray(new String[fieldName.size()]), C);
            if (!isEmpty(query)) {
                propExpQueryBuilder.add(query, isEmpty(lastLogOp) ? Occur.SHOULD : lastLogOp);
            }
        } catch (IOException e) {
            LOG.error("Failure when generating MultiFieldQuery : ", e);
        }

    }

    @Override
    public void enterPropertyStringValue(PropertyStringValueContext ctx) {
    }

    @Override
    public void enterRelOp(RelOpContext ctx) {
    }

    @Override
    public void enterSearchCrit(SearchCritContext ctx) {
    }

    @Override
    public void enterSearchExp(SearchExpContext ctx) {
    }

    @Override
    public void enterShortName(ShortNameContext ctx) {
    }

    @Override
    public void enterSpace(SpaceContext ctx) {
    }

    @Override
    public void enterStringOp(StringOpContext ctx) {
    }

    @Override
    public void enterVTab(VTabContext ctx) {
    }

    @Override
    public void enterWChar(WCharContext ctx) {
    }

    @Override
    public void exitAsterisk(AsteriskContext ctx) {
    }

    @Override
    public void exitBaseName(BaseNameContext ctx) {
    }

    @Override
    public void exitBaseProperties(BasePropertiesContext ctx) {
    }

    @Override
    public void exitBinOp(BinOpContext ctx) {
    }

    @Override
    public void exitClassName(ClassNameContext ctx) {
    }

    @Override
    public void exitClassRelExp(ClassRelExpContext ctx) {
    }

    @Override
    public void exitDef_return(Def_returnContext ctx) {
    }

    @Override
    public void exitDerivedName(DerivedNameContext ctx) {
    }

    @Override
    public void exitDQuote(DQuoteContext ctx) {
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
    }

    @Override
    public void exitExistsOp(ExistsOpContext ctx) {
    }

    @Override
    public void exitFormFeed(FormFeedContext ctx) {
    }

    @Override
    public void exitHTab(HTabContext ctx) {
    }

    @Override
    public void exitLineFeed(LineFeedContext ctx) {
    }

    @Override
    public void exitLinksToContainers(LinksToContainersContext ctx) {
    }

    @Override
    public void exitLogOp(LogOpContext ctx) {
    }

    @Override
    public void exitParse(ParseContext ctx) {

        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();
        Query propExpQuery = propExpQueryBuilder.build();
        mainQuery.add(propExpQuery, Occur.MUST);

        IndexType t = getIndexType();
        boolean isId3 = t == IndexType.ALBUM_ID3 || t == IndexType.ARTIST_ID3;
        Query folderQuery = queryFactory.toFolderQuery.apply(isId3, util.getAllMusicFolders());
        mainQuery.add(folderQuery, Occur.MUST);

        criteria.setParsedQuery(mainQuery.build());
    }

    @Override
    public void exitPeopleInvolved(PeopleInvolvedContext ctx) {
    }

    @Override
    public void exitProperty(PropertyContext ctx) {
    }

    @Override
    public void exitPropertyBooleanValue(PropertyBooleanValueContext ctx) {
    }

    @Override
    public void exitPropertyExp(PropertyExpContext ctx) {
    }

    @Override
    public void exitPropertyStringValue(PropertyStringValueContext ctx) {
    }

    @Override
    public void exitRelOp(RelOpContext ctx) {
    }

    @Override
    public void exitSearchCrit(SearchCritContext ctx) {
    }

    @Override
    public void exitSearchExp(SearchExpContext ctx) {
    }

    @Override
    public void exitShortName(ShortNameContext ctx) {
    }

    @Override
    public void exitSpace(SpaceContext ctx) {
    }

    @Override
    public void exitStringOp(StringOpContext ctx) {
    }

    @Override
    public void exitVTab(VTabContext ctx) {
    }

    @Override
    public void exitWChar(WCharContext ctx) {
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
    }

    @Override
    public void visitTerminal(TerminalNode node) {
    }

    private Query createMultiFieldQuery(final String[] fields, final String query) throws IOException {
        List<String> subjectFields = new ArrayList<>();
        boolean composerUsable = getIndexType() == IndexType.SONG && criteria.isIncludeComposer();
        Arrays.asList(fields).stream().forEach(f -> {
            // TODO #353
            if (FieldNames.COMPOSER_READING.equals(f) || FieldNames.COMPOSER.equals(f)) {
                if (composerUsable) {
                    subjectFields.add(f);
                }
            } else {
                subjectFields.add(f);
            }
        });
        if (0 == subjectFields.size()) {
            return null;
        }
        String[] f = subjectFields.toArray(new String[subjectFields.size()]);
        return queryFactory.createMultiFieldWildQuery(f, query, getIndexType());
    }

    private IndexType getIndexType() {
        Class<?> clazz = criteria.getAssignableClass();
        if (MediaFile.class == clazz) {
            return IndexType.SONG;
        } else if (Artist.class == clazz) {
            return IndexType.ARTIST_ID3;
        } else if (Album.class == clazz) {
            return IndexType.ALBUM_ID3;
        }
        return null;
    }

}
