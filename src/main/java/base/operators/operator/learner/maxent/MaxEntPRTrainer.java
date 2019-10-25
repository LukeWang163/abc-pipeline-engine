/* Copyright (C) 2011 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.  For further
information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent;

import base.operators.example.ExampleSet;
import base.operators.example.set.MappedExampleSet;
import base.operators.operator.OperatorCapability;
import base.operators.operator.OperatorDescription;
import base.operators.operator.learner.maxent.constraint.pr.MaxEntL2FLPRConstraints;
import base.operators.operator.learner.maxent.constraint.pr.MaxEntPRConstraint;
import base.operators.operator.learner.maxent.constraintslearner.MaxEntAbstractLearner;
import base.operators.operator.learner.maxent.optimize.LimitedMemoryBFGS;
import base.operators.operator.learner.maxent.optimize.MatrixOps;
import base.operators.operator.learner.maxent.optimize.Optimizer;
import base.operators.operator.learner.maxent.utils.Maths;
import base.operators.operator.nio.file.SimpleFileObject;
import base.operators.parameter.*;
import base.operators.tools.RandomGenerator;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:Penalty (soft) version of Posterior Regularization (PR) for training MaxEnt.
*/

public class MaxEntPRTrainer extends MaxEntAbstractLearner implements Serializable {

    private static Logger logger = Logger.getLogger(MaxEntPRTrainer.class.getName());

    public static final String NORMALIZE = "normalize";
    public static final String USE_VALUES = "use_values";
    public static final String MIN_ITERATIONS = "min_iterations";
    public static final String MAX_ITERATIONS = "max_iterations";
    public static final String TOLERANCE = "tolerance";

    // for using this from the command line
    private boolean normalize;
    private boolean useValues;
    private int minIterations;
    private int maxIterations;
    private double tolerance;

    private double qGPV = 1.0;//MaxEntL2FLPRConstraint中的weight
    private File constraintsFile;

    private int numIterations = 0;
    private boolean converged = false;
    private double pGPV = 1.0;//MaxEntOptimizableByLabelDistribution中的GaussianPriorVariance
    private ArrayList<MaxEntPRConstraint> constraints;
    private MaxEnt p;
    private PRAuxClassifier q;

    public MaxEntPRTrainer(OperatorDescription operatorDescription) {super(operatorDescription);}

    public void setPGaussianPriorVariance(double pGPV) {
      this.pGPV = pGPV;
    }

    public void setQGaussianPriorVariance(double qGPV) {
      this.qGPV = qGPV;
    }

    public void setConstraintsFile(File file) {
        this.constraintsFile = file;
    }

    public void setUseValues(boolean flag) {
        this.useValues = flag;
    }

    public void setMinIterations(int minIterations) {
        this.minIterations = minIterations;
    }

    public void setMaxIterations(int minIterations) {
        this.maxIterations = minIterations;
    }

    public void setNormalize(boolean normalize) {
    this.normalize = normalize;
    }

    public Optimizer getOptimizer() {
        throw new RuntimeException("Not yet implemented!");
    }

    public int getIteration() {
        return numIterations;
    }

    public MaxEnt getClassifier() {
        return p;
    }

    public MaxEnt learn(ExampleSet trainingSet, SimpleFileObject simpleFileObject) {
        return learn(trainingSet, maxIterations, simpleFileObject);
    }

    public MaxEnt learn(ExampleSet trainingSet, int maxIterations,SimpleFileObject simpleFileObject) {
        return learn(trainingSet,Math.min(maxIterations,minIterations),maxIterations, simpleFileObject);
    }

    public MaxEnt learn(ExampleSet data, int minIterations, int maxIterations, SimpleFileObject simpleFileObject) {
        try {
            normalize = getParameterAsBoolean(NORMALIZE);
            useValues = getParameterAsBoolean(USE_VALUES);
            minIterations = getParameterAsInt(MIN_ITERATIONS);
            maxIterations = getParameterAsInt(MAX_ITERATIONS);
            tolerance = getParameterAsDouble(TOLERANCE);
        } catch (UndefinedParameterError undefinedParameterError) {
            undefinedParameterError.printStackTrace();
        }

        constraintsFile = simpleFileObject.getFile();
        if (constraints == null && constraintsFile != null) {
            HashMap<Integer,double[]> constraintsMap =
            FeatureConstraintUtil.readConstraintsFromFile(constraintsFile, data);
            logger.info("number of constraints: " + constraintsMap.size());
            constraints = new ArrayList<MaxEntPRConstraint>();
            int regular_attributes_size = data.getAttributes().createRegularAttributeArray().length;
            MaxEntL2FLPRConstraints prConstraints = new MaxEntL2FLPRConstraints(regular_attributes_size,
            data.getAttributes().getLabel().getMapping().size(),useValues,normalize);
            for (int fi : constraintsMap.keySet()) {
                prConstraints.addConstraint(fi, constraintsMap.get(fi), qGPV);
            }
            constraints.add(prConstraints);
        }

        BitSet instancesWithConstraints = new BitSet(data.size());
        for (MaxEntPRConstraint constraint : constraints) {
            BitSet bitset = constraint.preProcess(data);
            instancesWithConstraints.or(bitset);
        }
        List<Integer> mapping = new ArrayList<>();
        for (int ii = 0; ii < data.size(); ii++) {
            if (instancesWithConstraints.get(ii)) {
    //          boolean noLabel = data.get(ii).getTarget() == null;
    //          if (noLabel) {
    //              data.get(ii).unLock();
    //              data.get(ii).setTarget(new NullLabel((LabelAlphabet)data.getTargetAlphabet()));
    //          }
                mapping.add(ii);
            }
        }
        int[]  mappingArray = new int[mapping.size()];
        for (int jj = 0; jj < mapping.size(); jj++) {
            mappingArray[jj] = mapping.get(jj);
        }
        ExampleSet unlabeled = new MappedExampleSet(data, mappingArray, true,true);

        int numFeatures = unlabeled.getAttributes().createRegularAttributeArray().length;

        // setup model
        int numParameters = (numFeatures + 1) * unlabeled.getAttributes().getLabel().getMapping().size();
        if (p == null) {
            p = new MaxEnt(unlabeled,new double[numParameters]);
        }

        // setup aux model
        q = new PRAuxClassifier(unlabeled, constraints);

        double oldValue = -Double.MAX_VALUE;
        for (numIterations = 0; numIterations < maxIterations; numIterations++) {

            double[][] base = optimizeQ(unlabeled, p,numIterations==0);

            double value = optimizePAndComputeValue(unlabeled, q, base, pGPV);
            logger.info("iteration " + numIterations + " total value " + value);

            if (numIterations >= (minIterations-1) && 2.0*Math.abs(value-oldValue) <= tolerance *
                (Math.abs(value)+Math.abs(oldValue) + 1e-5)){
                logger.info("PR value difference below tolerance (oldValue: " + oldValue + " newValue: " + value + ")");
                converged = true;
                break;
            }
            oldValue = value;
        }
        return p;
    }

    private double optimizePAndComputeValue(ExampleSet data, PRAuxClassifier q, double[][] base, double pGPV) {
        double entropy = 0;
        int numLabels = data.getAttributes().getLabel().getMapping().size();
        List<Integer> mapping = new ArrayList<>();
        double[][] data_scores = new double[data.size()][numLabels];
        for (int ii = 0; ii < data.size(); ii++) {
            double[] scores = new double[numLabels];
            q.getClassificationScores(data.getExample(ii), scores);
            for (int li = 0; li < numLabels; li++) {
                if (base != null && base[ii][li] == 0) {
                    scores[li] = Double.NEGATIVE_INFINITY;
                }else if (base != null) {
                    double logP = Math.log(base[ii][li]);
                    scores[li] += logP;
                }
            }
            MatrixOps.expNormalize(scores);
            entropy += Maths.getEntropy(scores);
            //还有疑问，这个scores跟着label以后有没有，是跟下边改造的等价.
//            LabelVector lv = new LabelVector((LabelAlphabet)data.getTargetAlphabet(), scores);
//            Instance instance = new Instance(data.get(ii).getData(),lv,null,null);
//            for (int kk = 0; kk < scores.length; kk++) {
//                data.getExample(ii).setConfidence(data.getAttributes().getLabel().getMapping().mapIndex(kk), scores[kk]);
//            }
            data_scores[ii] = scores;
            mapping.add(ii);
        }
        int[]  mappingArray = new int[mapping.size()];
        for (int jj = 0; jj < mapping.size(); jj++) {
            mappingArray[jj] = mapping.get(jj);
        }
        ExampleSet dataLabeled = new MappedExampleSet(data, mappingArray, true,true);

        // train supervised
        MaxEntOptimizableByLabelDistribution opt = new  MaxEntOptimizableByLabelDistribution(dataLabeled, data_scores, p);
        opt.setGaussianPriorVariance(pGPV);

        LimitedMemoryBFGS bfgs = new LimitedMemoryBFGS(opt);
        try { bfgs.optimize(); } catch (Exception e) { e.printStackTrace(); }
        bfgs.reset();
        try { bfgs.optimize(); } catch (Exception e) { e.printStackTrace(); }

        double value = 0;
        for (MaxEntPRConstraint constraint : q.getConstraintFeatures()) {
            // plus sign because this returns negative values
            value += constraint.getCompleteValueContribution();
        }
        value += entropy + opt.getValue();
        return value;
    }

    private double[][] optimizeQ(ExampleSet data, MaxEnt p, boolean firstIter) {
        int numLabels = data.getAttributes().getLabel().getMapping().size();

        double[][] base;
        if (firstIter) {
            base = null;
        }else {
            base = new double[data.size()][numLabels];
            for (int ii = 0; ii < data.size(); ii++) {
            double[] pros = p.predictLabelAndProbability(data.getExample(ii));
            for (int jj = 0; jj < pros.length; jj++)
                base[ii][jj] += pros[jj] * 1.0;
            }
        }

        PRAuxClassifierOptimizable optimizable = new PRAuxClassifierOptimizable(data,base,q);

        LimitedMemoryBFGS bfgs = new LimitedMemoryBFGS(optimizable);
        try { bfgs.optimize(); } catch (Exception e) { e.printStackTrace(); }
        bfgs.reset();
        try { bfgs.optimize(); } catch (Exception e) { e.printStackTrace(); }

        return base;
    }


    public boolean supportsCapability(OperatorCapability capability){
        switch (capability) {
        //            case POLYNOMINAL_ATTRIBUTES:
        //            case BINOMINAL_ATTRIBUTES:
        case NUMERICAL_ATTRIBUTES:
        case BINOMINAL_LABEL:
        case NUMERICAL_LABEL:
        case POLYNOMINAL_LABEL:
        case WEIGHTED_EXAMPLES:
        case UPDATABLE:
        case MISSING_VALUES:
            return true;
        default:
            return false;
        }
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeBoolean(NORMALIZE, "The paramter of normalize.", true));
        types.add(new ParameterTypeBoolean(USE_VALUES, "The paramter of use_values.", false));
        types.add(new ParameterTypeInt(MIN_ITERATIONS, "The paramter of min_iterations.", 1, Integer.MAX_VALUE,
                10, false));
        types.add(new ParameterTypeInt(MAX_ITERATIONS, "The paramter of max_iterations.", 1, Integer.MAX_VALUE,
                500, false));
        types.add(new ParameterTypeDouble(TOLERANCE, "The paramter of tolerance.", 0, Double.MAX_VALUE,
                0.001, false));

        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
        return types;
    }
}
