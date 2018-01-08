package org.aika.wikipedia.importer;

import org.aika.Model;
import org.aika.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


public class NodeStatistic implements Writable {

    public volatile int frequency;
    public volatile double nullHypFreq;
    public volatile double oldNullHypFreq;
    public volatile boolean frequencyHasChanged = true;
    public volatile int nOffset;

    public volatile int sizeSum = 0;
    public volatile int instanceSum = 0;


    public volatile int numberOfPositionsNotify;
    public volatile int frequencyNotify;

    public double weight = -1;


    public NodeStatistic(int nOffset) {
        this.nOffset = nOffset;
    }


    @Override
    public void write(DataOutput out) throws IOException {

    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {

    }
}
