package org.aika.wikipedia.importer;

import java.util.Iterator;

public class DefaultIterable< I > implements Iterable< I > {
	private Iterator< I > iterator;
	
	public DefaultIterable(Iterator< I > iterator) {
		super();
		this.iterator = iterator;
	}

	@Override
	public Iterator< I > iterator() {
		return iterator;
	}
}
