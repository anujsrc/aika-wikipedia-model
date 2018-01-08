package org.aika.wikipedia.importer.impl;

import java.util.Set;
import java.util.TreeSet;


public class Level {
	private final int startPos;
	private final WikiTag tag;
	private final Set< String > flags = new TreeSet< String >();

	public Level(int startPos, WikiTag tag) {
		super();
		this.startPos = startPos;
		this.tag = tag;
	}

	public boolean getFlag(String flagName) {
		return this.flags.contains(flagName);
	}

	public void setFlag(String flagsName, boolean flag) {
		if(flag) {
			this.flags.add(flagsName);
		} else {
			this.flags.remove(flagsName);
		}
	}

	public int getStartPos() {
		return this.startPos;
	}

	public WikiTag getTag() {
		return this.tag;
	}

	@Override
	public String toString() {
		return "StartPos:" + this.startPos + " Tag:" + this.tag;
	}


};
