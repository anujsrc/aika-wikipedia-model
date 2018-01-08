package org.aika.wikipedia.model;


import org.aika.ActivationFunction;
import org.aika.Input;
import org.aika.Model;
import org.aika.Neuron;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Relation;
import org.aika.corpus.Range.Operator;
import org.aika.lattice.AndNode;
import org.aika.neuron.INeuron;
import org.aika.storage.NeuronRepository;
import org.aika.training.MetaInput;
import org.aika.training.MetaNetwork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.aika.corpus.Range.Mapping.BEGIN;
import static org.aika.corpus.Range.Mapping.END;

@Component
public class WikipediaModel {

    @Autowired
    public Model aikaModel;

    @Autowired
    NeuronRepository neuronRepository;


    public Neuron upperCaseN;
    public Neuron documentN;
    public Neuron phraseN;
    public Neuron wordSuppr;
    public Neuron phraseSuppr;
    public Neuron entitySuppr;
    public Neuron topicSuppr;
    public Neuron phraseMetaN;
    public Neuron entityMetaN;
    public Neuron topicMetaN;


    public void init() {
        aikaModel.setAndNodeCheck(n -> {
            for(AndNode.Refinement ref: n.parents.keySet()) {
                if(!ref.input.get().inputNeuron.get().label.startsWith("S-")) return false;
            }
            return true;
        });

        upperCaseN = neuronRepository.lookupNeuronProvider("UPPER CASE");
        documentN = neuronRepository.lookupNeuronProvider("DOCUMENT");
        phraseN = neuronRepository.lookupNeuronProvider("PHRASE");

        wordSuppr = neuronRepository.lookupNeuronProvider("S-WORD");
        phraseSuppr = neuronRepository.lookupNeuronProvider("S-PHRASE");
        entitySuppr = neuronRepository.lookupNeuronProvider("S-ENTITY");
        topicSuppr = neuronRepository.lookupNeuronProvider("S-TOPIC");

        phraseMetaN = neuronRepository.lookupNeuronProvider("M-PHRASE");
        entityMetaN = neuronRepository.lookupNeuronProvider("M-ENTITY");
        topicMetaN = neuronRepository.lookupNeuronProvider("M-TOPIC");

        MetaNetwork.initMetaNeuron(aikaModel, phraseMetaN, 5.0, 5.0,
                new Input()
                        .setNeuron(phraseN)
                        .setWeight(40.0)
                        .setBias(-40.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new MetaInput()
                        .setMetaWeight(20.0)
                        .setMetaBias(-20.0)
                        .setMetaRelativeRid(true)
                        .setNeuron(wordSuppr)
                        .setWeight(20.0)
                        .setBias(-20.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Operator.EQUALS, Operator.GREATER_THAN_EQUAL)
                        .setRangeOutput(true, false),
                new MetaInput()
                        .setMetaWeight(20.0)
                        .setMetaBias(-20.0)
                        .setMetaRelativeRid(true)
                        .setNeuron(wordSuppr)
                        .setWeight(20.0)
                        .setBias(-20.0)
                        .setRelativeRid(null)
                        .setRangeMatch(Operator.LESS_THAN_EQUAL, Operator.EQUALS)
                        .setRangeOutput(false, true),
                new MetaInput()
                        .setMetaWeight(10.0)
                        .setMetaBias(-10.0)
                        .setMetaRelativeRid(true)
                        .setNeuron(wordSuppr)
                        .setWeight(0.0)
                        .setBias(0.0)
                        .setRelativeRid(null)
                        .setRangeMatch(Operator.LESS_THAN, Operator.GREATER_THAN)
                        .setRangeOutput(false, false),
                new MetaInput()
                        .setMetaWeight(-100.0)
                        .setMetaBias(0.0)
                        .setNeuron(phraseSuppr)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRelativeRid(null)
                        .setRangeMatch(Relation.OVERLAPS)
        );

        MetaNetwork.initMetaNeuron(aikaModel, entityMetaN, 5.0, 5.0,
                new Input()
                        .setNeuron(phraseN)
                        .setWeight(40.0)
                        .setBias(-40.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new MetaInput()
                        .setMetaWeight(40.0)
                        .setMetaBias(-40.0)
                        .setNeuron(phraseSuppr)
                        .setWeight(40.0)
                        .setBias(-40.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new MetaInput()
                        .setMetaWeight(2.0)
                        .setMetaBias(-1.0)
                        .setNeuron(topicSuppr)
                        .setWeight(0.0)
                        .setBias(0.0)
                        .setRelativeRid(null)
                        .setRangeMatch(Relation.CONTAINED_IN)
                        .setRangeOutput(false),
                new MetaInput()
                        .setMetaWeight(-100.0)
                        .setMetaBias(0.0)
                        .setNeuron(entitySuppr)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRelativeRid(null)
                        .setRangeMatch(Relation.OVERLAPS)
        );

        MetaNetwork.initMetaNeuron(aikaModel, topicMetaN, 5.0, 5.0,
                new MetaInput()
                        .setMetaWeight(500.0)
                        .setMetaBias(-500.0)
                        .setNeuron(documentN)
                        .setWeight(500.0)
                        .setBias(-500.0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true),
                new MetaInput()
                        .setMetaWeight(2.0)
                        .setMetaBias(-1.0)
                        .setNeuron(entitySuppr)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(Relation.CONTAINS)
                        .setRangeOutput(false),
                new Input()
                        .setNeuron(topicSuppr)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRangeMatch(Relation.NONE)
                        .setRangeOutput(false)
        );

        aikaModel.addSynapse(phraseSuppr,
                new MetaInput()
                        .setMetaWeight(1.0)
                        .setMetaBias(0.0)
                        .setNeuron(phraseMetaN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );


        aikaModel.initNeuron(wordSuppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY);

        aikaModel.initNeuron(phraseSuppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY);

        aikaModel.initNeuron(entitySuppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY,
                new MetaInput()
                        .setMetaWeight(1.0)
                        .setMetaBias(0.0)
                        .setNeuron(entityMetaN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );

        aikaModel.initNeuron(topicSuppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY,
                new MetaInput()
                        .setMetaWeight(1.0)
                        .setMetaBias(0.0)
                        .setNeuron(topicMetaN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );
    }


    public Neuron lookupWordNeuron(String word) {
        return lookupNeuron(wordSuppr, word);
    }


    public Neuron lookupEntityNeuron(String entity) {
        return lookupNeuron(entitySuppr, entity);
    }


    public Neuron lookupTopicNeuron(String topic) {
        return lookupNeuron(topicSuppr, topic);
    }


    private Neuron lookupNeuron(Neuron suppr, String key) {
        Neuron n = neuronRepository.getNeuronProvider(key);
        if(n != null) {
            return n;
        }

        n = neuronRepository.lookupNeuronProvider(key);

        aikaModel.addSynapse(suppr,
                new Input()
                        .setNeuron(n)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Relation.EQUALS)
                        .setRangeOutput(true)
        );

        return n;
    }


    public void processEntity(Document doc, int b, int e, String entity) {
        phraseN.addInput(doc, b, e);

        InterprNode pin = InterprNode.addPrimitive(doc);
        pin.fixed = true;
        Neuron n = lookupEntityNeuron(entity);
        n.addInput(doc, b, e, null, pin, 1.0, 1.0);
    }


    public void processTopic(Document doc, String topic) {
        InterprNode pin = InterprNode.addPrimitive(doc);
        pin.fixed = true;

        Neuron tn = lookupTopicNeuron(topic);
        tn.addInput(doc, 0, doc.length(), null, pin, 1.0, 1.0);
    }


    public void parseWord(Document doc, int begin, int end, int wordCount, String w) {
        Neuron inputNeuron = lookupWordNeuron("W-" + w.toLowerCase());
        if (inputNeuron != null) {
            inputNeuron.addInput(doc, begin, end, wordCount);
        }

        if (Character.isUpperCase(w.charAt(0))) {
            upperCaseN.addInput(doc, begin, end, wordCount);
        }
    }
}
