/* Copyright (C) 2009 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package base.operators.operator.learner.maxent;

import base.operators.example.Attribute;
import base.operators.example.ExampleSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
/**
 * @author wangpanpan
 * create time:  2019.07.29.
 * description:Utility functions for creating feature constraints that can be used with GE training.
 */

public class FeatureConstraintUtil {
  
	private static Logger logger = Logger.getLogger(FeatureConstraintUtil.class.getName());
  
  /**
   * Reads range constraints stored using strings from a file. Format can be either:
   * 
   * feature_name (label_name:lower_probability,upper_probability)+
   * 
   * or
   * 
   * feature_name (label_name:probability)+
   * 
   * Constraints are only added for feature-label pairs that are present.
   * 
   * @param file File with feature constraints.
   * @param data InstanceList used for alphabets.
   * @return Constraints.
   */
  public static HashMap<Integer,double[][]> readRangeConstraintsFromFile(File file, ExampleSet data) {
    HashMap<Integer,double[][]> constraints = new HashMap<Integer,double[][]>();
    
    for (int li = 0; li < data.getAttributes().getLabel().getMapping().size(); li++) {
      System.err.println(data.getAttributes().getLabel().getMapping().mapIndex(li));
    }
    
    try {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      String line = reader.readLine();
      while (line != null) {
        String[] split = line.split("\\s+");
        
        // assume the feature name has no spaces
        String featureName = split[0];
        List<Attribute> regularAttributes = Arrays.asList(data.getAttributes().createRegularAttributeArray());
        int featureIndex = regularAttributes.indexOf(data.getAttributes().get(featureName));
        if (featureIndex == -1) { 
          throw new RuntimeException("Feature " + featureName + " not found in the alphabet!");
        }
        
        double[][] probs = new double[data.getAttributes().getLabel().getMapping().size()][2];
        for (int i = 0; i < probs.length; i++) Arrays.fill(probs[i ],Double.NEGATIVE_INFINITY);
        for (int index = 1; index < split.length; index++) {
          String[] labelSplit = split[index].split(":");   
          
          int li = data.getAttributes().getLabel().getMapping().getIndex(labelSplit[0]);
          assert (li != -1) : labelSplit[0];
          
          if (labelSplit[1].contains(",")) {
            String[] rangeSplit = labelSplit[1].split(",");
            double lower = Double.parseDouble(rangeSplit[0]);
            double upper = Double.parseDouble(rangeSplit[1]);
            probs[li][0] = lower;
            probs[li][1] = upper;
          }
          else {
            double prob = Double.parseDouble(labelSplit[1]);
            probs[li][0] = prob;
            probs[li][1] = prob;
          }
        }
        constraints.put(featureIndex, probs);
        line = reader.readLine();
      }
    }
    catch (Exception e) {  
      e.printStackTrace();
      System.exit(1);
    }
    return constraints;
  }

  /**
   * Reads feature constraints from a file, whether they are stored
   * using Strings or indices.
   *
   * @param file File with feature constraints.
   * @param data InstanceList used for alphabets.
   * @return Constraints.
   */
  public static HashMap<Integer,double[]> readConstraintsFromFile(File file, ExampleSet data) {
    if (testConstraintsFileIndexBased(file)) {
      return readConstraintsFromFileIndex(file,data);
    }
    return readConstraintsFromFileString(file,data);
  }

  private static boolean testConstraintsFileIndexBased(File file) {
    String firstLine = "";
    try {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      firstLine = reader.readLine();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return !firstLine.contains(":");
  }

  /**
   * Reads feature constraints stored using strings from a file.
   *
   * feature_index label_0_prob label_1_prob ... label_n_prob
   *
   * Here each label must appear.
   *
   * @param file File with feature constraints.
   * @param data InstanceList used for alphabets.
   * @return Constraints.
   */
  public static HashMap<Integer,double[]> readConstraintsFromFileIndex(File file, ExampleSet data) {
    HashMap<Integer,double[]> constraints = new HashMap<Integer,double[]>();
    try {
      BufferedReader reader = new BufferedReader(new FileReader(file));

      String line = reader.readLine();
      while (line != null) {
        String[] split = line.split("\\s+");
        int featureIndex = Integer.parseInt(split[0]);
        assert(split.length - 1 == data.getAttributes().getLabel().getMapping().size());
        double[] probs = new double[split.length - 1];
        for (int index = 1; index < split.length; index++) {
          double prob = Double.parseDouble(split[index]);
          probs[index-1] = prob;
        }
        constraints.put(featureIndex, probs);
        line = reader.readLine();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return constraints;
  }
  /**
   * Reads feature constraints stored using strings from a file.
   *
   * feature_name (label_name:probability)+
   *
   * Labels that do appear get probability 0.
   *
   * @param file File with feature constraints.
   * @param data InstanceList used for alphabets.
   * @return Constraints.
   */
  public static HashMap<Integer,double[]> readConstraintsFromFileString(File file, ExampleSet data) {
    HashMap<Integer,double[]> constraints = new HashMap<Integer,double[]>();

    try {
      BufferedReader reader = new BufferedReader(new FileReader(file));

      String line = reader.readLine();
      while (line != null) {
        String[] split = line.split("\\s+");

        // assume the feature name has no spaces
        String featureName = split[0];
        List<Attribute> regularAttributes = Arrays.asList(data.getAttributes().createRegularAttributeArray());
        int featureIndex = regularAttributes.indexOf(data.getAttributes().get(featureName));

        assert(split.length - 1 == data.getAttributes().getLabel().getMapping().size()) : split.length + " " + data.getAttributes().getLabel().getMapping().size();
        double[] probs = new double[split.length - 1];
        for (int index = 1; index < split.length; index++) {
          String[] labelSplit = split[index].split(":");
          int li = data.getAttributes().getLabel().getMapping().getIndex(labelSplit[0]);
          assert(li != -1) : "Label " + labelSplit[0] + " not found";
          double prob = Double.parseDouble(labelSplit[1]);
          probs[li] = prob;
        }
        constraints.put(featureIndex, probs);
        line = reader.readLine();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return constraints;
  }


}
