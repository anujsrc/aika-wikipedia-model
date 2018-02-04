package org.aika.wikipedia;

import org.aika.*;
import org.aika.corpus.Document;
import org.aika.training.PatternDiscovery;
import org.aika.corpus.Range;
import org.aika.lattice.*;
import org.aika.neuron.Activation;
import org.aika.neuron.Neuron;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;
import org.aika.storage.NeuronRepository;
import org.aika.training.*;
import org.aika.wikipedia.importer.NodeStatistic;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;


@Component
public class Train {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static double SIGNIFICANCE_THRESHOLD = 0.98;

    @Autowired
    public Model aikaModel;

    @Autowired
    NeuronRepository neuronRepository;


    public Set<Provider<AndNode>> numberOfPositionsQueue = Collections.synchronizedSet(new TreeSet<>((n1, n2) -> {
                int r = Integer.compare(((NodeStatistic) n1.get().statistic).numberOfPositionsNotify, ((NodeStatistic) n2.get().statistic).numberOfPositionsNotify);
                if (r != 0) return r;
                return n1.compareTo(n2);
            })
    );

    public volatile int numberOfPositions;

    PatternDiscovery.Config discoveryConfig;
    SupervisedTraining.Config trainConfig;

    long visitedCounter = 0;

    public static final Synapse.Key DEFAULT_TOPIC_SYNAPSE_KEY = new Synapse.Key(
            false,
            null,
            null,
            Range.Relation.NONE,
            Range.Output.NONE
    );


    public Neuron supprN;


    @PostConstruct
    public void init() {

        aikaModel.setNodeStatisticFactory(() -> new NodeStatistic(numberOfPositions));
        aikaModel.setNeuronStatisticFactory(() -> new NeuronStatistic(numberOfPositions));

        supprN = Neuron.init(aikaModel.createNeuron("SUPPR"), 0.0, INeuron.Type.INHIBITORY);

        this.discoveryConfig = new PatternDiscovery.Config()
                .setCounter(act -> countPattern(act))
                .setCheckExpandable(act -> checkExpandable(act))
                .setCheckValidPattern(n -> evaluate(n));


        trainConfig = new SupervisedTraining.Config()
                .setLearnRate(0.2)
                .setPerformBackpropagation(true)
                .setSynapseEvaluation((s, iAct, oAct) -> synapseEval(iAct, oAct));

    }


    private SynapseEvaluation.Result synapseEval(Activation iAct, Activation oAct) {
        if (StringUtils.startsWith(iAct.key.node.neuron.get().label, "E-") &&
                StringUtils.startsWith(oAct.key.node.neuron.get().label, "T-")) {
            return new SynapseEvaluation.Result(DEFAULT_TOPIC_SYNAPSE_KEY, sig(iAct, oAct), SynapseEvaluation.DeleteMode.NONE);
        }

        return null;
    }

    private double sig(Activation iAct, Activation oAct) {
        return 0.0;
    }


    private boolean checkExpandable(NodeActivation act) {
        Node n = act.key.node;
        if (n instanceof InputNode) {
            InputNode in = (InputNode) n;
            if (!in.inputNeuron.get().label.startsWith("L-")) return false;
        }

        return ((NodeStatistic) n.statistic).frequency >= 1;
    }


    public void train(Document doc) {
        visitedCounter++;
        numberOfPositions += doc.length();

//        MetaNetwork.train(doc);

        countNeurons(doc);
        log.info(doc.activationsToString(true, true));

//        InterprSupprTraining.train(doc, new InterprSupprTraining.Config().setLearnRate(0.5));

/*        doc.supervisedTraining.train(trainConfig);

        LongTermLearning.train(doc,
                new LongTermLearning.Config()
        );

        doc.commit();
*/
/*        doc.discoverPatterns(discoveryConfig);


        ArrayList<AndNode> tmp = new ArrayList<>();
        for(Provider<AndNode> p: numberOfPositionsQueue) {
            AndNode n = p.get();

            Statistic s = (Statistic) n.statistic;
            if (s.numberOfPositionsNotify > numberOfPositions) break;

            tmp.add(n);
        }

        for(AndNode n: tmp) {
            updateWeight(n, doc, visitedCounter);
        }
*/
    }


    private void countNeurons(Document doc) {
        doc.finallyActivatedNeurons.forEach(n -> {
            NeuronStatistic ns = (NeuronStatistic) n.statistic;

            n.getFinalActivations(doc).forEach(act -> ns.frequency += act.getFinalState().value);
        });
    }


    private void countPattern(NodeActivation act) {
        Node n = act.key.node;
        if (n instanceof OrNode) return;

        NodeStatistic s = (NodeStatistic) n.statistic;

        double c = getCovered(act);

        s.frequency += 1.0 - c;
        s.frequencyHasChanged = true;

        s.sizeSum += act.key.range.begin == Integer.MIN_VALUE || act.key.range.end == Integer.MAX_VALUE ? 1 : Math.max(1, act.key.range.end - act.key.range.begin);
        s.instanceSum++;

        if (n instanceof AndNode) {
            AndNode an = (AndNode) n;
            computeNullHyp(an);
            if (s.frequencyHasChanged && discoveryConfig.checkExpandable.evaluate(act)) {
                s.frequencyHasChanged = false;

                updateWeight(an, act.doc, visitedCounter);
            }
        }
    }


    private double getCovered(NodeActivation<?> act) {
        ArrayList<NodeActivation<InputNode>> inputs = new ArrayList<>();
        collectInputActivations(inputs, act, visitedCounter++);

        if (inputs.isEmpty()) return 1.0;

        double covered = 0.0;
        Activation iNAct = (Activation) inputs.get(0).inputs.firstEntry().getValue();
        for (Activation.SynapseActivation sa : iNAct.neuronOutputs) {
            Activation oNAct = sa.output;

            if (!"SUPPR".equals(oNAct.key.node.neuron.get().label)) {
                double c = 1.0;
                for (NodeActivation<InputNode> iAct : inputs) {
                    Synapse s = iAct.key.node.getSynapse(Utils.nullSafeSub(oNAct.key.rid, false, iAct.key.rid, false), oNAct.key.node.neuron);
                    Activation ina = (Activation) iAct.inputs.firstEntry().getValue();

                    if (s != null && oNAct.neuronInputs.contains(new Activation.SynapseActivation(s, ina, oNAct))) {
                        double x = s.input.get().activationFunction.f(s.weight * ina.getFinalState().value);
                        c = Math.min(c, x);
                    }
                }

                covered = Math.max(covered, c * oNAct.getFinalState().value);
            }
        }

        return covered;
    }


    private void collectInputActivations(List<NodeActivation<InputNode>> results, NodeActivation<?> act, long v) {
        if (act.visited == v) return;
        act.visited = v;

        if (act.key.node instanceof InputNode) {
            InputNode in = (InputNode) act.key.node;
            if (!in.inputNeuron.get().label.startsWith("SUPPR")) {
                results.add((NodeActivation<InputNode>) act);
            }
        } else {
            for (NodeActivation<?> iAct : act.inputs.values()) {
                collectInputActivations(results, iAct, v);
            }
        }
    }


    private void computeNullHyp(AndNode node) {
        NodeStatistic s = (NodeStatistic) node.statistic;

        double avgSize = s.sizeSum / s.instanceSum;
        double n = (double) (numberOfPositions - s.nOffset) / avgSize;

        double nullHyp = 0.0;
        for (Map.Entry<AndNode.Refinement, Provider<? extends Node>> me : node.parents.entrySet()) {
            Node pn = me.getValue().get();
            InputNode in = me.getKey().input.get();
            NodeStatistic ins = (NodeStatistic) in.statistic;
            NodeStatistic pns = (NodeStatistic) pn.statistic;

            double inputNA = (double) (numberOfPositions - ins.nOffset) / avgSize;
            double inputNB = (double) (numberOfPositions - pns.nOffset) / avgSize;

            double nh = Math.min(1.0, ins.frequency / inputNA) * Math.min(1.0, Math.max(pns.frequency, pns.nullHypFreq) / inputNB);
            nullHyp = Math.max(nullHyp, nh);
        }

        s.nullHypFreq = nullHyp * n;
    }


    public void updateWeight(AndNode node, Document doc, long v) {
        if (!isConnectedPattern(node)) return;

        Node.ThreadState th = node.getThreadState(doc.threadId, true);
        NodeStatistic s = (NodeStatistic) node.statistic;

        if ((numberOfPositions - s.nOffset) == 0 ||
                !discoveryConfig.checkValidPattern.evaluate(node) ||
                th.visited == v ||
                (s.numberOfPositionsNotify > numberOfPositions && s.frequencyNotify > s.frequency && Math.abs(s.nullHypFreq - s.oldNullHypFreq) < 0.01)
                ) {
            return;
        }

        th.visited = v;

        double avgSize = s.sizeSum / s.instanceSum;
        double n = (double) (numberOfPositions - s.nOffset) / avgSize;

        numberOfPositionsQueue.remove(node.provider);
        s.numberOfPositionsNotify = computeNotify(n) + numberOfPositions;
        numberOfPositionsQueue.add(node.provider);

        BinomialDistribution binDist = new BinomialDistribution(null, (int) Math.round(n), s.nullHypFreq / n);

        s.weight = binDist.cumulativeProbability(s.frequency - 1);

        s.frequencyNotify = computeNotify(s.frequency) + s.frequency;
        s.oldNullHypFreq = s.nullHypFreq;

        if (s.weight >= SIGNIFICANCE_THRESHOLD) {
            System.out.println(node.provider.id + " " + Train.nodeToString(node) + " " + node.logicToString() + " " + s.frequency + " " + s.weight);

            // TODO: Instantiate a Meta Neuron
        }
    }


    private static int computeNotify(double x) {
        return 1 + (int) Math.floor(Math.pow(x, 1.15) - x);
    }


    private boolean evaluate(Node n) {
        if (n instanceof InputNode) return true;
        if (n instanceof AndNode) {
            AndNode an = (AndNode) n;

            if (!isValidPattern(an)) return false;

            AndNode.Refinement beginRef = getBeginRef(an);
            AndNode.Refinement endRef = getEndRef(an);

            if (beginRef != null && endRef != null && beginRef.rid >= endRef.rid) return false;

            Integer firstRid = null;
            Integer lastRid = null;
            for (AndNode.Refinement ref : an.parents.keySet()) {
                if (beginRef != null && ref != beginRef && ref.rid <= beginRef.rid) return false;
                if (endRef != null && ref != endRef && ref.rid >= endRef.rid) return false;

                firstRid = Utils.nullSafeMin(Utils.nullSafeMin(0, firstRid), ref.rid);
                lastRid = Utils.nullSafeMax(Utils.nullSafeMin(0, lastRid), ref.rid);
            }

            if (lastRid - firstRid > 5) return false;
        }

        return true;
    }


    private boolean isConnectedPattern(AndNode n) {
        TreeSet<AndNode.Refinement> tmp = new TreeSet<>(new Comparator<AndNode.Refinement>() {
            @Override
            public int compare(AndNode.Refinement ref1, AndNode.Refinement ref2) {
                int r = Utils.compareInteger(ref1.rid, ref2.rid);
                if (r != 0) return r;
                return ref1.input.compareTo(ref2.input);
            }
        });
        tmp.addAll(n.parents.keySet());

        int i = 0;
        int j = 0;
        for (AndNode.Refinement ref : tmp) {
            if (i == 0 && !(ref.input.get().key.rangeOutput.begin == Range.Mapping.BEGIN)) return false;
            if (i + 1 == tmp.size() && !(ref.input.get().key.rangeOutput.end == Range.Mapping.END)) return false;
            if (i != 0 && j + 1 != ref.rid) return false;

            j = Math.max(0, ref.rid);
            i++;
        }

        return true;
    }


    private boolean isValidPattern(AndNode an) {
        for (AndNode.Refinement ref : an.parents.keySet()) {
            if (!ref.input.get().inputNeuron.get().label.startsWith("L-")) return false;
        }
        return true;
    }


    private AndNode.Refinement getBeginRef(AndNode an) {
        for (AndNode.Refinement ref : an.parents.keySet()) {
            Synapse.Key k = ref.input.get().key;

            if (k.rangeOutput.begin == Range.Mapping.BEGIN && k.rangeMatch.beginToBegin == Range.Operator.EQUALS) return ref;
        }
        return null;
    }


    private AndNode.Refinement getEndRef(AndNode an) {
        for (AndNode.Refinement ref : an.parents.keySet()) {
            Synapse.Key k = ref.input.get().key;

            if (k.rangeOutput.end == Range.Mapping.END && k.rangeMatch.endToEnd == Range.Operator.EQUALS) return ref;
        }
        return null;
    }


    public static String nodeToString(AndNode n) {
        char[] tmp = new char[]{' ', ' ', ' ', ' ', ' ', ' ', ' '};

        for (AndNode.Refinement ref : n.parents.keySet()) {
            tmp[Math.max(0, ref.rid)] = ref.input.get().inputNeuron.get().label.charAt(2);
        }
        return new String(tmp).trim();
    }
}
