package base.operators.operator.nlp.ner.location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class LocationFV {
	public static String featureLabels[] = { "TOP_CODE", "SAME_CODE",
			"PARENT_CODE", "AM_PARENT_CODE", "SAME_FATHER", "MIN_DISTANCE",
			"MIN_PARENT_DISTANCE" };

	protected double values[]; // the feature value
	protected boolean isSet[]; // whether the feature is set
	/**
	 * 词是否删除标志
	 */
	private boolean deleteFlag = false;
	/**
	 * 歧义词消解匹配成功的特征
	 */
	private ArrayList<ItemFeature> featureList = new ArrayList();
	/**
	 * 歧义词
	 */
	private String word;
	/**
	 * 对于符合上下级及存在同上级的特别加权系数
	 */
	private double SPC_WEIGHT=0.5d;
	/**
	 * 歧义词对应的地址集合
	 */
	private HashSet<LocationWrapper> ambiguousWords;

	public static int featureIndex(String label) {
		int idx = ArrayUtils.indexOf(featureLabels, label);
		if (idx == -1) {
			throw new ArrayIndexOutOfBoundsException(label);
		}
		return idx;
	}

	/**
	 * Return a list of values. Note that for model input, you should rather use
	 * getFV().
	 */
	public double[] getValues() {
		return values;
	}

	public boolean[] getIsSet() {
		return isSet;
	}

	public static String[] getFeatures() {
		return featureLabels;
	}

	public double getFeatureValue(String f) {
		return values[featureIndex(f)];
	}

	public boolean isFeatureSet(String f) {
		return isSet[featureIndex(f)];
	}
	/**
	 * 统计各特征出现的次数
	 * @return
	 */
	private HashMap<String,Integer> getFvCountMap(){
		HashMap<String,Integer> map=new HashMap();
		for (int i = 0; i < featureList.size(); i++) {
			ItemFeature feature = featureList.get(i);
			String label=feature.getFeatureLable();
			Integer val=map.get(label);
			if(val==null){
				map.put(label, 1);
			}else{
				map.put(label, val+1);
			}
		}
		return map;
	}

	public LocationFV(String word, HashSet<LocationWrapper> ambiguousWords) {
		this.word = word;
		this.ambiguousWords = ambiguousWords;
		values = new double[featureLabels.length];
		isSet = new boolean[featureLabels.length];
	}

	/**
	 * 添加特征项
	 * 
	 * @param itemFeature
	 */
	public void addItemFeature(ItemFeature itemFeature) {
		String lable = itemFeature.getFeatureLable();

		int index = featureIndex(lable);
		assert (index >= 0);

		boolean hasBeenSet = isSet[index];

		values[index] = values[index] + 1;
		isSet[index] = true;
		featureList.add(itemFeature);
	}

	/**
	 * 设置特征项已经经过计算但该项特征不匹配的标志
	 * 
	 * @param lable
	 */
	public void setFeatureToNull(String lable) {
		int index = featureIndex(lable);
		assert (index >= 0);

		isSet[index] = true;
		values[index] = 0;
	}

	/**
	 * 设置经过特征计算该项特征判别认为该词不应该独立成词标志
	 * 
	 * @param lable
	 */
	public void setFeatureToFail(String lable) {
		int index = featureIndex(lable);
		assert (index >= 0);
		isSet[index] = true;// 特征已经设置过
		values[index] = -999;// 特征不能成词
		deleteFlag = true;
	}

	/**
	 * 词是否设置过删除标志
	 * 
	 * @return
	 */
	public boolean isDelete() {
		return deleteFlag;
	}

	public  LocationWrapper getMostLikelyResult() {
		if (isDelete())
			return null;
		if (featureList.size() == 0)
			return null;
		if (featureList.size() == 1) {
			return featureList.get(0).getLocation();
		}
		HashMap<String, Double> countMap = new HashMap();
		for (int i = 0; i < featureList.size(); i++) {
			ItemFeature feature = featureList.get(i);
			Double val = countMap.get(feature.getCode());
			double level=feature.getLocation().getLocation().getLevel();
			double tempV =1/level;
			String label=feature.getFeatureLable();
			HashMap<String,Integer>  fvCountMap=getFvCountMap();
			Integer labelCount=fvCountMap.get(label);
			if(labelCount!=null && labelCount==1){//独一无二的特征项进行加倍奖励
				tempV=tempV*2;
			}		
			if(label.equals(FV.AM_PARENT_CODE)|| label.equals(FV.PARENT_CODE) || label.equals(FV.SAME_FATHER))
				tempV=tempV+SPC_WEIGHT;

			//System.out.println("feature.getCode():"+feature.getCode()+"  tempV:"+tempV+"  vaL:"+val);
			if (val == null) {
				countMap.put(feature.getCode(), tempV);
			} else {
				tempV=tempV+val;
				countMap.put(feature.getCode(), tempV);
			}
		}
		
		//System.out.println("======getMostLikelyResult word:"+word+"  fList:"+getFeatureList());
		//System.out.println("======getMostLikelyResult:"+countMap);
		if (countMap.size() == 1) {
			return featureList.get(0).getLocation();
		}
		Iterator it = countMap.keySet().iterator();
		double maxValue = -1;
		int maxValueNumber = 1;
		String maxValueCode = null;
		while (it.hasNext()) {
			String code = (String) it.next();
			double val = countMap.get(code);
			if (val > maxValue) {
				maxValue = val;
				maxValueCode = code;
				maxValueNumber = 1;
			} else if (val == maxValue) {
				maxValueNumber++;
			}
		}
		if (maxValueNumber == 1) {
			return getLocationByCode(maxValueCode);
		} else {
			return getTopLevelLocation();
		}

	}

	private LocationWrapper getLocationByCode(String code) {
		if (featureList.size() == 0 || code == null)
			return null;
		for (int i = 0; i < featureList.size(); i++) {
			ItemFeature feature = featureList.get(i);
			LocationWrapper loc = feature.getLocation();
			if (loc.getLocation().getCode().equals(code))
				return loc;
		}
		return null;
	}

	private LocationWrapper getTopLevelLocation() {
		if (featureList.size() == 0)
			return null;
		if (featureList.size() == 1) {
			return featureList.get(0).getLocation();
		}
		int minTopLevel = 9999999;
		LocationWrapper topLoc = null;
		for (int i = 0; i < featureList.size(); i++) {
			ItemFeature feature = featureList.get(i);
			LocationWrapper loc = feature.getLocation();
			int myLevel = loc.getLocation().getLevel();
			if (myLevel < minTopLevel) {
				minTopLevel = myLevel;
				topLoc = loc;
			}

		}
		return topLoc;
	}

	public ArrayList<ItemFeature> getFeatureList() {
		return featureList;
	}
	
	

}
