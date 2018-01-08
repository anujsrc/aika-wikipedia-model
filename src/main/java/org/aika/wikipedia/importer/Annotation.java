package org.aika.wikipedia.importer;


import org.aika.corpus.Range;

import java.util.ArrayList;
import java.util.List;

public class Annotation {

    public Range r;

    public List<String>  attributes = new ArrayList<>();

    private Annotation() {}

    public Annotation(Range r) {
        this.r = r;
    }

}
