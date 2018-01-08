package org.aika.wikipedia.importer.impl;


import java.util.TreeMap;

public enum WikiTag {
	OPEN_INTERNAL_LINK("[[",null,true),
	CLOSE_INTERNAL_LINK("]]",OPEN_INTERNAL_LINK,true),
	INTERNAL_SECTION_LINK("#",OPEN_INTERNAL_LINK,false),
	INTERNAL_LINK_SEPERATOR("|",OPEN_INTERNAL_LINK,false),
	IMAGE_LINK("Bild:",OPEN_INTERNAL_LINK,false),
	CATEGORY1_LINK("Kategorie:",OPEN_INTERNAL_LINK,false),
	CATEGORY2_LINK(":Kategorie:",OPEN_INTERNAL_LINK,false),
	OPEN_EXTERNAL_LINK("[http:",null,true),
	CLOSE_EXTERNAL_LINK("]",OPEN_EXTERNAL_LINK,true),
	OPEN_EXTERNAL_REFERENCE("<ref>",null,true),
	CLOSE_EXTERNAL_REFERENCE("</ref>",OPEN_EXTERNAL_REFERENCE,true),
	OPEN_VARIABLE("{{",null,true),
	CLOSE_VARIABLE("}}",OPEN_VARIABLE,true),
	//		OPEN_TABLE("{",null,true),
	//		CLOSE_TABLE("}",OPEN_TABLE,true),
	INDENTED8("\n::::::::",null,false),
	INDENTED7("\n:::::::",null,false),
	INDENTED6("\n::::::",null,false),
	INDENTED5("\n:::::",null,false),
	INDENTED4("\n::::",null,false),
	INDENTED3("\n:::",null,false),
	INDENTED2("\n::",null,false),
	INDENTED1("\n:",null,false),
	OPEN_BOLDANDITALIC("'''''",null,true),
	CLOSE_BOLDANDITALIC("'''''",OPEN_BOLDANDITALIC,true),
	OPEN_BOLD("'''",null,true),
	CLOSE_BOLD("'''",OPEN_BOLD,true),
	OPEN_ITALIC("''",null,true),
	CLOSE_ITALIC("''",OPEN_ITALIC,true),
	OPEN_HEADLINE8("\n========",null,true),
	CLOSE_HEADLINE8("========",OPEN_HEADLINE8,true),
	OPEN_HEADLINE7("\n=======",null,true),
	CLOSE_HEADLINE7("=======",OPEN_HEADLINE7,true),
	OPEN_HEADLINE6("\n======",null,true),
	CLOSE_HEADLINE6("======",OPEN_HEADLINE6,true),
	OPEN_HEADLINE5("\n=====",null,true),
	CLOSE_HEADLINE5("=====",OPEN_HEADLINE5,true),
	OPEN_HEADLINE4("\n====",null,true),
	CLOSE_HEADLINE4("====",OPEN_HEADLINE4,true),
	OPEN_HEADLINE3("\n===",null,true),
	CLOSE_HEADLINE3("===",OPEN_HEADLINE3,true),
	OPEN_HEADLINE2("\n==",null,true),
	CLOSE_HEADLINE2("==",OPEN_HEADLINE2,true),
	NUMBERED_ITEM8("\n#########",null,false),
	NUMBERED_ITEM7("\n########",null,false),
	NUMBERED_ITEM6("\n#######",null,false),
	NUMBERED_ITEM5("\n######",null,false),
	NUMBERED_ITEM4("\n#####",null,false),
	NUMBERED_ITEM3("\n###",null,false),
	NUMBERED_ITEM2("\n##",null,false),
	NUMBERED_ITEM1("\n#",null,false),
	BULLET_POINT8("\n********",null,false),
	BULLET_POINT7("\n*******",null,false),
	BULLET_POINT6("\n******",null,false),
	BULLET_POINT5("\n*****",null,false),
	BULLET_POINT4("\n****",null,false),
	BULLET_POINT3("\n***",null,false),
	BULLET_POINT2("\n**",null,false),
	BULLET_POINT1("\n*",null,false),
	TABLE_OF_CONTENT("__TOC__",null,false),
	NO_TABLE_OF_CONTENT("__NOTOC__",null,false),
	OPEN_MATH("<math>",null,true),
	CLOSE_MATH("</math>",OPEN_MATH,true),
	OPEN_SMALL("<small>",null,true),
	CLOSE_SMALL("</small>",OPEN_SMALL,true),
	OPEN_BIG("<big>",null,true),
	CLOSE_BIG("</big>",OPEN_BIG,true),
	OPEN_PARAGRAPH("<p>",null,true),
	CLOSE_PARAGRAPH("</p>",OPEN_PARAGRAPH,true),
	OPEN_STRIKEOUT("<s>",null,true),
	CLOSE_STRIKEOUT("</s>",OPEN_STRIKEOUT,true),
	OPEN_UNDERLINE("<u>",null,true),
	CLOSE_UNDERLINE("</u>",OPEN_UNDERLINE,true),
	OPEN_INSERTED("<ins>",null,false),
	CLOSE_INSERTED("</ins>",OPEN_INSERTED,true),
	OPEN_DELETED("<del>",null,true),
	CLOSE_DELETED("</del>",OPEN_DELETED,true),
	OPEN_SUBSCRIPT("<sub>",null,true),
	CLOSE_SUBSCRIPT("</sub>",OPEN_SUBSCRIPT,true),
	OPEN_SUPERSCRIPT("<sup>",null,true),
	CLOSE_SUPERSCRIPT("</sup>",OPEN_SUPERSCRIPT,true),
	OPEN_NOWIKI("<nowiki>",null,true),
	CLOSE_NOWIKI("</nowiki>",OPEN_NOWIKI,true),
	OPEN_CODEBLOCK("<code>",null,true),
	CLOSE_CODEBLOCK("</code>",OPEN_CODEBLOCK,true),
	OPEN_PRE("<pre>",null,true),
	CLOSE_PRE("</pre>",OPEN_PRE,true),
	OPEN_DIV("<div",null,true),
	CLOSE_DIV("</div>",OPEN_DIV,true),
	OPEN_COMMENT("<!--",null,true),
	CLOSE_COMMENT("-->",OPEN_COMMENT,true),
	NON_BREAKING_SPACE("&nbsp;",null,false),
	AMPERSAND("&amp;",null,false),
	LINEBREAK("<br/>",null,false),
	LINEBREAK_A("<br />",null,false);

	final String tag;
	final WikiTag requiresPattern;
	final boolean isSegment;

	WikiTag(String tag, WikiTag requiresPattern, boolean isSegment) {
		this.tag = tag;
		this.requiresPattern = requiresPattern;
		this.isSegment = isSegment;
	}

	public int length() {
		return this.tag.length();
	}

	public static TreeMap<String, WikiTag[]> tags;
	public static int maxLength;

	static {
		tags = new TreeMap<>();
		for(WikiTag t: values()) {
			WikiTag[] wt = tags.get(t.tag);
			if(wt == null) {
				wt = new WikiTag[2];
				tags.put(t.tag, wt);
			}
			wt[t.requiresPattern == null ? 0 : 1] = t;

			maxLength = Math.max(maxLength, t.tag.length());
		}
	}
}