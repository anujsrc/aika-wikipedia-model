package org.aika.wikipedia.importer.impl;


import org.aika.neuron.activation.Range;
import org.aika.wikipedia.importer.Annotation;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.*;


public class WikiArticle {

	public String title;
	public StringBuilder content;

	public boolean isDisambiguationPage;
	public String disambiguationKeyword;
	public boolean isRedirect = false;
	public boolean isCategoryPage = false;
	public String redirectsTo = null;
	public Annotation keywordAnnotation;
	public List<Annotation> annotations = new ArrayList<>();
	public final List<String> globalAttributes = new ArrayList<>();

	private static Set<String> languageLabels = initLanguageTags("af","als","am","an","ang","ar","arc","ast","az","bar","bat-smg","bcl","be","be-x-old","bg","bh","bm","bn","bo","bpy","br","bs","ca","ceb","co","cs","csb","cv","cy","da","diq","dsb","dv","el","en","eo","es","et",
			"eu","fa","fi","fiu-vro","fr","frp","fo","fur","fy","ga","gan","gd","gl","gn","gu","gv","haw","he","hi","hr","hsb","ht","hu","hy","ia","id","ie","ilo","io","is","it","iu","ja","jbo","jv","ka","kk","km","kn",
			"ko","ku","ksh","kw","la","lad","lb","li","lij","lmo","ln","lt","lv","map-bms","mi","mk","ml","mn","mr","ms","mt","mzn","nah","nap","nds","nds-nl","ne","new","nl","nn","no","nov","nrm","oc","os","pag","pam",
			"pdc","pi","pl","pms","pt","qu","rm","ro","roa-rup","roa-tara","ru","sa","scn","sco","se","sh","simple","sk","sl","sq","sr","Stó","sv","sw","ta","te","tg","th","tk","tl","to","tpi","tr","tt","ug","uk","ur",
			"uz","vec","vi","vls","vo","wa","war","wo","wuu","xh","yi","yo","zh","zh-classical","zh-min-nan","zh-yue","zu");


	public StringBuilder text = new StringBuilder();
	int sectionStart[] = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};

	
	private static Set<String> initLanguageTags(String... tags) {
		return new HashSet<>(Arrays.asList(tags));
	}

	private WikiArticle() {

	}

	public WikiArticle(String t, String content) {
		t = t.replaceAll("\\u00A0", " ");
		if(t.startsWith("Kategorie:")) {
			title = t.substring(10).trim();
			isCategoryPage = true;
		} else {
			title = t.trim();
		}

		this.content = new StringBuilder(content.replaceAll("\\u00A0", " "));
	}


	public String getMainKey() {
		return (isCategoryPage ? "C-" : "E-") + title;
	}


	private void addHeadline(int startOfHeadline) {
		int endOfHeadline = this.text.length();
		if(startOfHeadline < endOfHeadline && this.text.charAt(startOfHeadline) == ' ') {
			startOfHeadline++;
		}
		if(endOfHeadline > 0 && startOfHeadline < endOfHeadline && this.text.charAt(endOfHeadline - 1) == ' ') {
			endOfHeadline--;
		}

		Annotation a = new Annotation(new Range(startOfHeadline,endOfHeadline));
		a.attributes.add("HEADLINE");
		this.annotations.add(a);
	}


	private void setWikiKeyword(int startOfHeadline) {
		String keyword = this.text.substring(startOfHeadline);

		int ke = keyword.indexOf(" (");
		if(ke == -1) {
			ke = this.text.length();
		} else {
			int dkwEnd = keyword.indexOf(")", ke + 2);
			if(dkwEnd != -1) {
				disambiguationKeyword = keyword.substring(ke + 2, dkwEnd);
			}
			ke += startOfHeadline;
		}
		this.keywordAnnotation = new Annotation(new Range(startOfHeadline,ke));
		this.keywordAnnotation.attributes.add("ARTICLE-KEYWORD");
		this.keywordAnnotation.attributes.add("HEADLINE");

		String label = getLinkAttribute(keyword);
		if(label != null) {
			this.keywordAnnotation.attributes.add("E-" + label);
		}
		this.annotations.add(this.keywordAnnotation);
	}


	private void processLink(String linkTitel, Level level, int s, int e) {
		Annotation linkObject = new Annotation(new Range(s,e));

		Annotation entityObject = null;
		if(!level.getFlag("isCategoryLink")) {
			String[] languageLink = getLanguageLink(linkTitel);
			if(languageLink == null) {
				String label = getLinkAttribute(linkTitel);

				if(label != null) {
					entityObject = new Annotation(new Range(s,e));
					entityObject.attributes.add("E-" + label);
					this.annotations.add(entityObject);
				}
			} else {
				/*
				// TODO: Language links generate too many entity labels.
				label = getLinkAttribute(languageLink[1]);
				 */
				linkObject.attributes.add("LANGUAGE_LINK");
			}

		} else {
			linkObject.attributes.add("CATEGORY");
			this.globalAttributes.add(getCategoryAttribute(linkTitel));
		}

		this.annotations.add(linkObject);
	}

	private String[] getLanguageLink(String linkTitel) {
		// Remove trailing :
		if(linkTitel.startsWith(":")) {
			linkTitel = linkTitel.substring(1);
		}

		int sepPos = linkTitel.indexOf(":");
		if(sepPos == -1) {
			return null;
		}
		String language = linkTitel.substring(0,sepPos);
		if(WikiArticle.languageLabels.contains(language)) {
			String[] r = new String[2];
			r[0] = language;
			r[1] = linkTitel.substring(sepPos + 1);
			return r;
		}
		return null;
	}

	private String getLinkAttribute(String linkTitle) {
		assert linkTitle != null;

		// TODO: Hack: Filter out falsely encoded strings.
		if(countNumberOfPercentSigns(linkTitle) > 3) {
			return null;
		}

		if(linkTitle.startsWith("Image:")) {
			return null;
		}

		return linkTitle;
	}

	private int countNumberOfPercentSigns(String s) {
		int c = 0;
		for(int i = 0; i < s.length(); i++) {
			if(s.charAt(i) == '%') {
				c++;
			}
		}
		return c;
	}

	private String getCategoryAttribute(String linkTitel) {
		if(linkTitel.startsWith("Kategorie:")) {
			linkTitel = linkTitel.substring(10);
		}
		return "C-" + linkTitel.trim();
	}

	private void nextSection(int h) {
		for(int i = h; i < this.sectionStart.length; i++) {
			if(this.sectionStart[i] != -1) {
				Annotation a = new Annotation(new Range(this.sectionStart[i],this.text.length()));
				a.attributes.add("PARAGRAPH");
				this.annotations.add(a);
				this.sectionStart[i] = -1;
			}
		}
		this.sectionStart[h] = this.text.length();
	}


	public void parse() {
		LinkedList<Level> stack = new LinkedList<Level>();

		String link = null;

		this.text.append(this.title);
		addHeadline(0);
		setWikiKeyword(0);
		this.text.append(" ");

		labelWikipediaDisambiguationTerm();
		
		int variableLevel = 0;
		StringBuilder variable = null;

		boolean isComment = false;
		boolean record = true;

		String content = this.content.toString();
		if(content.contains("{{Begriffsklärung}}")) {
			isDisambiguationPage = true;
		}
		if(content.contains("#REDIRECT")) {
			isRedirect = true;
		}

//		if((pass == 0) != (isDisambiguationPage || isRedirect)) return;

		content = StringEscapeUtils.unescapeHtml4(content);

		for(int i = 0; i < content.length(); i++) {
			Level level = !stack.isEmpty() ? stack.getFirst() : null;
			WikiTag t = matchTag(content, i, this.text.length(), stack, isComment);
			if(t == null) {
				if(record) {
					this.text.append(content.charAt(i));
				}
				if(variable != null) {
					variable.append(content.charAt(i));
				}
			} else {
				switch(t) {
				case OPEN_INTERNAL_LINK:
					link = null;
					break;
				case CLOSE_INTERNAL_LINK:
					if(!level.getFlag("isImageLink") && !level.getFlag("invalidLink")) {
						if(link == null) {
							link = this.text.substring(level.getStartPos());
						}
						if(link.length() != 0) {
							processLink(link,level,level.getStartPos(),this.text.length());
							if(this.isRedirect && this.redirectsTo == null) {
								this.redirectsTo = link;
							}
						}
					}
					link = null;
					break;
				case IMAGE_LINK:
					level.setFlag("isImageLink",true);
					break;
				case CATEGORY1_LINK:
					level.setFlag("isCategoryLink",true);
					break;
				case CATEGORY2_LINK:
					level.setFlag("isCategoryLink",true);
					break;
				case INTERNAL_SECTION_LINK:
					this.text.append("#");
					break;
				case INTERNAL_LINK_SEPERATOR:
					if(level.getFlag("linkSeperatorWasFound")) {
						level.setFlag("invalidLink",true);
					}
					if(level.getTag() == WikiTag.OPEN_INTERNAL_LINK) { //  && !level.getFlag("isImageLink") && !level.getFlag("invalidLink")
						link = this.text.substring(level.getStartPos());
						level.setFlag("linkSeperatorWasFound",true);

						this.text.delete(level.getStartPos(),this.text.length());
					}
					break;
				case OPEN_EXTERNAL_LINK:
					record = false;
					break;
				case CLOSE_EXTERNAL_LINK:
					record = true;
					break;
				case OPEN_VARIABLE:
					variableLevel++;
					record = false;
					variable = new StringBuilder();
					break;
				case CLOSE_VARIABLE:
					variableLevel--;
					if(variableLevel == 0) {
						parseVariable(variable);
						record = true;
						variable = null;
					} else if(variableLevel < 0) {
						variableLevel = 0;
					}
					break;
				case OPEN_HEADLINE8:
					nextSection(7);
					break;
				case CLOSE_HEADLINE8:
					addHeadline(level.getStartPos());
					break;
				case OPEN_HEADLINE7:
					nextSection(6);
					break;
				case CLOSE_HEADLINE7:
					addHeadline(level.getStartPos());
					break;
				case OPEN_HEADLINE6:
					nextSection(5);
					break;
				case CLOSE_HEADLINE6:
					addHeadline(level.getStartPos());
					break;
				case OPEN_HEADLINE5:
					nextSection(4);
					break;
				case CLOSE_HEADLINE5:
					addHeadline(level.getStartPos());
					break;
				case OPEN_HEADLINE4:
					nextSection(3);
					break;
				case CLOSE_HEADLINE4:
					addHeadline(level.getStartPos());
					break;
				case OPEN_HEADLINE3:
					nextSection(2);
					break;
				case CLOSE_HEADLINE3:
					addHeadline(level.getStartPos());
					break;
				case OPEN_HEADLINE2:
					nextSection(1);
					break;
				case CLOSE_HEADLINE2:
					addHeadline(level.getStartPos());
					break;
					
				case NUMBERED_ITEM8:
				case NUMBERED_ITEM7:
				case NUMBERED_ITEM6:
				case NUMBERED_ITEM5:
				case NUMBERED_ITEM4:
				case NUMBERED_ITEM3:
				case NUMBERED_ITEM2:
				case NUMBERED_ITEM1:
				case BULLET_POINT8:
				case BULLET_POINT7:
				case BULLET_POINT6:
				case BULLET_POINT5:
				case BULLET_POINT4:
				case BULLET_POINT3:
				case BULLET_POINT2:
				case BULLET_POINT1:
					this.text.append("\n");
					break;

					
				case OPEN_EXTERNAL_REFERENCE:
				case OPEN_MATH:
				case OPEN_COMMENT:
					isComment = true;
					break;
					
				case CLOSE_EXTERNAL_REFERENCE:
				case CLOSE_MATH:
				case CLOSE_COMMENT:
					isComment = false;
					break;

				case NON_BREAKING_SPACE:
					this.text.append(" ");
					break;
				case AMPERSAND:
					this.text.append("&");
					break;
				default:
					break;
				}

				i += t.length() - 1;
			}
		}

		nextSection(0);

		for(String l: this.globalAttributes) {
			this.keywordAnnotation.attributes.add(l);
		}
	}


	private void parseVariable(StringBuilder variable) {
		if(variable != null && variable.length() >= 4 && variable.substring(0,4).equalsIgnoreCase("PND|")) {
			int varEnd = variable.indexOf("|",4);
			if(varEnd == -1) {
				varEnd = variable.length();
			}
			String pndId = variable.substring(4,varEnd);
			this.globalAttributes.add("PND_PREFIX" + pndId);
		}
	}


	private void labelWikipediaDisambiguationTerm() {
		int cwb = this.text.indexOf("(");
		int cwe = cwb > 0 ? this.text.indexOf(")",cwb) : -1;

		if(cwb > 0 && cwe > 0) {
			Annotation o = new Annotation(new Range(cwb + 1,cwe));
			o.attributes.add("DISAMBIGUATION-TERM");
			this.annotations.add(o);
		}
	}


	private WikiTag matchTag(String page, int pos, int tp, LinkedList<Level> stack, boolean isComment) {
		WikiTag bestPattern = null;

		for(int i = pos; i <= Math.min(pos + WikiTag.maxLength, page.length()); i++) {
			WikiTag[] p = WikiTag.tags.get(page.substring(pos, i));

			if(p != null) {
				if (p[0] != null) {
					bestPattern = p[0];
				}
				if (p[1] != null && !stack.isEmpty() && findTagOnStack(stack, p[1].requiresPattern)) {
					bestPattern = p[1];
				}
			}
		}

		if(isComment && bestPattern != WikiTag.CLOSE_COMMENT && bestPattern != WikiTag.CLOSE_MATH && bestPattern != WikiTag.CLOSE_EXTERNAL_REFERENCE) {
			return null;
		}

		if(bestPattern != null && bestPattern.isSegment) {
			if(bestPattern.requiresPattern == null) {
				stack.push(new Level(tp, bestPattern));
			} else {
				while(stack.pop().getTag() != bestPattern.requiresPattern) {}
			}
		}
		return bestPattern;
	}


	private boolean findTagOnStack(LinkedList<Level> stack, WikiTag tag) {
		for(Level l: stack) {
			if(l.getTag() == tag) {
				return true;
			}
		}
		return false;
	}

}
