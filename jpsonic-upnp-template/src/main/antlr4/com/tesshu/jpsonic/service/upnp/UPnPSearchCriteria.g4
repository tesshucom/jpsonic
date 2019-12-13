// This file is part of Jpsonic.
//
// Jpsonic is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Jpsonic is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Jpsonic.  If not, see <http://www.gnu.org/licenses/>.
//
// Copyright 2019 (C) tesshu.com

grammar UPnPSearchCriteria ;

parse
	: searchCrit ;

className
	: baseName
	| derivedName ;

baseName
	: 'object' ;

derivedName
	: baseName ('.' shortName)* ;

shortName
	: ALPHANUM ;

baseProperties
	: 'dc:title'
	| 'dc:creator'
	| 'upnp:class' ;

peopleInvolved
	: 'upnp:artist'
	| 'upnp:artist@role'
	| 'upnp:actor'
	| 'upnp:actor@role'
	| 'upnp:author'
	| 'upnp:author@role'
	| 'upnp:producer'
	| 'upnp:director'
	| 'dc:publisher'
	| 'dc:contributor'
	| 'upnp:director' ;

linksToContainers
	: 'dc:genre'
	| 'dc:album'
	| 'upnp:playlist' ;

searchCrit
	: searchExp
	| '(' searchExp ')'
	| asterisk ;

searchExp
	: classRelExp wChar+ 'and' wChar+ ( '(' propertyExp  (wChar+ logOp wChar+ propertyExp)* ')' | propertyExp ) ;

logOp
	: 'and'
	| 'or' ;

classRelExp
	: 'upnp:class' wChar+ binOp wChar+ STRING
	| propertyExp ;

propertyExp
	: property wChar+ binOp wChar+ propertyStringValue
	| property wChar+ existsOp wChar+ propertyBooleanValue ;

binOp
	: relOp
	| stringOp ;

relOp
	: '='
	| '!='
	| '<'
	| '<='
	| '>'
	| '>=' ;

stringOp
	: 'contains'
	| 'doesNotContain'
	| 'derivedfrom' ;

existsOp
	: 'exists' ;

wChar
	: space
	| hTab
	| lineFeed
	| vTab
	| formFeed
	| def_return ;

property
	: baseProperties
	| peopleInvolved
	| linksToContainers ;

propertyStringValue
	: STRING ;

propertyBooleanValue
	: BOOLEAN ;

hTab
	: '\\u0x09' ;

lineFeed
	: '\n'
	| '\\u0x0A' ;

vTab
	: '\t'
	| '\\u0x0B' ;

formFeed
	: '\\u0x0C' ;

def_return
	: '\r'
	| '\n'
	| '\\u0x0D' ;

space
	: ' '
	| '\\0x20' ;

dQuote
	: '"' ;

asterisk
	: '*'
	| '\\0x2A' ;

STRING
	: '"' (~[\r\n"] | '""')+ '"' {
     String s = getText();
     s = s.substring(1, s.length() - 1);
     s = s.replace("\"\"", "\"");
     setText(s);
	}
	;

BOOLEAN
	: 'true'
	| 'false' ;

ALPHANUM
	: ( [a-zA-Z]+ | [0-9]+)+ ;

