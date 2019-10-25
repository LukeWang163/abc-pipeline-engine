package base.operators.operator.nlp.ner.location;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * 检测地址集合中是否存在同地址名称不同地址编码的歧义词并进行歧义消解
 * 
 * @author 王建华
 *
 */
public class LocationClustering {

	static NSDictionary DICT;;

	/**
	 * 存储分词算法传递过来的经过连续地址合并后去除被长串覆盖的短串的结果
	 */
	private HashSet<LocationWrapper> locationSet;
	/**
	 * 存储不存在歧义的词（原则上不要再经过排除歧义计算）
	 */
	private HashSet<LocationWrapper> unambiguousSet = new HashSet();
	/**
	 * 歧义词集合
	 */
	private HashSet<LocationWrapper> ambiguousSet = new HashSet();
	/**
	 * 存储以同字符串为key的歧义词集合
	 */
	private HashMap<String, HashSet<LocationWrapper>> ambiguousWordMap = new HashMap();
	/**
	 * 歧义词特征
	 */
	private HashMap<String, LocationFV> ambiguousFV = new HashMap();
	/**
	 * 集合是否存在歧义的标志，true存在歧义，false不存在歧义
	 */
	private boolean isAmbiguous = false;

	/**
	 * 构造函数
	 * 
	 * @param locationSet
	 */
	public LocationClustering(HashSet<LocationWrapper> locationSet, int type, InputStream stream) {
		this.locationSet = locationSet;
		if (this.locationSet == null)
			locationSet = new HashSet<LocationWrapper>();
		DICT = NSDictionary.getIstance(type, stream);
	}

	/**
	 * 歧义消解计算
	 * 
	 * @return
	 */
	public ArrayList<LocationWrapper> process() {
		// 识别歧义词
		identifyingAmbiguity();
		// System.out.println("===== process  unambiguousSet:" + unambiguousSet
		// + "\tambiguousSet:" + ambiguousSet);
		// 如果不存在歧义词
		if (isAmbiguous) {
			// 词义消解
			ambiguousProcess();
		}
		// System.out.println("===== process  unambiguousSet:" + unambiguousSet
		// + "\tambiguousSet:" + ambiguousSet);
		// 处理非歧义词中的个别特殊情况（剔除异常低频词）
		return removeBadCase();
	}

	/**
	 * 对同字符的歧义地址按照最高地址级别形成特征
	 */
	private void generateTopCodeFeature() {
		Iterator it = ambiguousWordMap.keySet().iterator();
		while (it.hasNext()) {
			String word = (String) it.next();
			HashSet<LocationWrapper> wordSet = ambiguousWordMap.get(word);
			Object wordArray[] = wordSet.toArray();
			// if (wordArray.length <= 1)
			// return;
			int topLevel = 999;
			// int topLevelCount = 0;
			ArrayList<LocationWrapper> topLocList = new ArrayList();
			// LocationWrapper defaultLoc = null;
			// 取对应字符在地址编码中最高层级的词(如果存在两个以上最高级别的词则不做歧义排除)
			for (int i = 0; i < wordArray.length; i++) {
				LocationWrapper thisLoc = (LocationWrapper) wordArray[i];
				int thisLevel = thisLoc.getLocation().getLevel();
				if (thisLevel < topLevel) {
					topLevel = thisLevel;
					// topLevelCount = 1;
					topLocList.clear();
					topLocList.add(thisLoc);
				} else if (thisLevel == topLevel) {
					topLocList.add(thisLoc);
				}
			}
			// 如果本词最高层级的词只有一个则默认为该级别最高的词表达的概念
			LocationFV locationFv = ambiguousFV.get(word);
			int MAX_LEVEL_DISTANCE = 4;// 定义以高层概念开头匹配底层概念的最大测层级差(比如中国湖南，广东中山能成功,中华人民共和国治安应该不允许成功)
			if (locationFv == null)
				locationFv = new LocationFV(word, wordSet);
			if (topLevel != 999 && topLocList.size() == 1) {
				LocationWrapper defaultLoc = topLocList.get(0);
				if (defaultLoc.getLevelDistance() > MAX_LEVEL_DISTANCE) {// 超过层级差的合并地址不视为正常地址
					locationFv.setFeatureToFail(FV.TOP_CODE);
					ambiguousFV.put(word, locationFv);
					continue;
				}
				ItemFeature itemF = new ItemFeature(word, defaultLoc,
						FV.TOP_CODE, "111");
				locationFv.addItemFeature(itemF);
				ambiguousFV.put(word, locationFv);
			} else if (topLocList.size() > 1) {
				HashSet<String> tempSet = new HashSet();
				for (int k = 0; k < topLocList.size(); k++) {
					LocationWrapper tempWord = topLocList.get(k);
					if (!tempSet.contains(tempWord.getLocation().getCode()
							+ tempWord.getWord())) {
						createItemFeature(topLocList.get(k), FV.TOP_CODE,
								wordSet, "118");
						tempSet.add(tempWord.getLocation().getCode()
								+ tempWord.getWord());
					}

				}
			}
		}
	}

	/**
	 * 歧义词上级在无歧义地址涵盖的字面编码上下范围
	 */
	private void generateUnAmParentCodeFeature() {
		// 如果无歧义词不存在则方法不执行
		if (unambiguousSet.size() == 0)
			return;
		Iterator it = ambiguousWordMap.keySet().iterator();
		while (it.hasNext()) {
			String word = (String) it.next();
			HashSet<LocationWrapper> wordSet = ambiguousWordMap.get(word);
			Object wordArray[] = wordSet.toArray();
			for (int i = 0; i < wordArray.length; i++) {
				LocationWrapper thisLoc = (LocationWrapper) wordArray[i];
				ArrayList<LocationWrapper> fatherList = findFatherList(thisLoc,
						unambiguousSet);
				if (fatherList.size() > 0) {// 找到上级，创建特征
					createItemFeature(thisLoc, FV.PARENT_CODE, wordSet, "170");
				}
			}

		}
	}

	/**
	 * 创建特征
	 * 
	 * @param loc
	 * @param feature
	 * @param wordSet
	 */
	private void createItemFeature(LocationWrapper loc, String feature,
			HashSet<LocationWrapper> wordSet, String ruleCode) {
		LocationFV locationFv = ambiguousFV.get(loc.getWord());
		if (locationFv == null)
			locationFv = new LocationFV(loc.getWord(), wordSet);
		ItemFeature itemF = new ItemFeature(loc.getWord(), loc, feature,
				ruleCode);
		locationFv.addItemFeature(itemF);
		ambiguousFV.put(loc.getWord(), locationFv);
	}

	/**
	 * 无歧义词上级在歧义地址涵盖的字面编码上下范围内
	 */
	private void generateAmParentCodeFeature() {
		// 如果无歧义词不存在则方法不执行
		if (unambiguousSet.size() == 0 || ambiguousWordMap.size() == 0)
			return;
		Object unAmArray[] = unambiguousSet.toArray();
		for (int i = 0; i < unAmArray.length; i++) {
			LocationWrapper unAmLoc = (LocationWrapper) unAmArray[i];
			Iterator it = ambiguousWordMap.keySet().iterator();
			while (it.hasNext()) {
				String word = (String) it.next();
				HashSet<LocationWrapper> wordSet = ambiguousWordMap.get(word);
				ArrayList<LocationWrapper> fatherList = findFatherList(unAmLoc,
						wordSet);
				if (fatherList.size() > 0) {// 找到上级，创建特征
					for (int k = 0; k < fatherList.size(); k++) {
						LocationWrapper loc = fatherList.get(k);
						createItemFeature(loc, FV.PARENT_CODE, wordSet, "213");
					}
				}
			}

		}

	}

	/**
	 * 查找一个地址在对应的地址集合中是否能找到自己的上级
	 * 
	 * @param thisLoc
	 * @param wordSet
	 * @return
	 */
	private ArrayList<LocationWrapper> findFatherList(LocationWrapper thisLoc,
			HashSet<LocationWrapper> wordSet) {
		ArrayList<LocationWrapper> fatherList = new ArrayList();
		Object wordArray[] = wordSet.toArray();
		for (int i = 0; i < wordArray.length; i++) {
			LocationWrapper tLoc = (LocationWrapper) wordArray[i];
			// if(thisLoc.getWord().equals("江苏昆山周庄") &&
			// tLoc.getLocation().getCode().startsWith("32058")){
			// System.out.println("========findFaherlist:thisLoc:"+thisLoc+"\ttLoc:"+tLoc+"\t outcome:"+thisLoc.isFather(tLoc));
			// }
			if (thisLoc.isFather(tLoc)) {
				fatherList.add(tLoc);
			}
		}
		// if(thisLoc.getWord().equals("江苏昆山周庄"))
		// System.out.println("========findFaherlist:thisLoc:"+thisLoc+" fatherList:"+fatherList);
		return fatherList;
	}

	/**
	 * 歧义词集合中存在直接上下级情况特征抽取 例如：广西 桂林 资源
	 */
	private void generateAmParentChildFeature() {
		// 如果歧义词不存在则方法不执行
		if (ambiguousSet.size() == 0)
			return;
		Object amArray[] = ambiguousSet.toArray();
		for (int i = 0; i < amArray.length - 1; i++) {
			LocationWrapper pLoc = (LocationWrapper) amArray[i];
			for (int j = i + 1; j < amArray.length; j++) {
				LocationWrapper rLoc = (LocationWrapper) amArray[j];
				// System.out.println("======genAmChild: pLoc:"+pLoc+"\trLoc:"+rLoc);
				LocationWrapper parent = null, child = null;
				if (rLoc.getLocation().getLevel() == pLoc.getLocation()
						.getLevel()) {// 存在相同地址编码提取特征
					if (pLoc.getLocation().getCode()
							.equals(rLoc.getLocation().getCode())
							&& !pLoc.getLocation().getName()
									.equals(rLoc.getLocation().getName())) {
						// if(pLoc.getWord().equals("周庄"))
						// System.out.println("========SAMECODE  pLoc:"+pLoc+"\trLoc:"+rLoc);
						// if(!rLoc.getWord().equals(pLoc.getWord()))
						createItemFeature(pLoc, FV.SAME_CODE, ambiguousSet,
								"260");
						// createItemFeature(rLoc, FV.SAME_CODE,
						// ambiguousSet,"261");
						continue;
					} else {
						continue;
					}
				} else if (rLoc.getLocation().getLevel() < pLoc.getLocation()
						.getLevel()) {
					parent = rLoc;
					child = pLoc;
					// System.out.println("======genAmChild: else if 1 pLoc:"+pLoc+"\trLoc:"+rLoc);
				} else if (rLoc.getLocation().getLevel() > pLoc.getLocation()
						.getLevel()) {
					parent = pLoc;
					child = rLoc;
					// System.out.println("======genAmChild: else if 2 pLoc:"+pLoc+"\trLoc:"+rLoc);
				}
				if (parent != null && parent.isFather(child)) {
					if (!parent.getWord().equals(child.getWord())) {// 对于上下地址重名的不作为上下级特征处理，防止歧义中的歧义
						createItemFeature(parent, FV.AM_PARENT_CODE,
								ambiguousSet, "279");
						createItemFeature(child, FV.AM_PARENT_CODE,
								ambiguousSet, "280");
					}

				}

			}
		}

	}

	/**
	 * 歧义词的直接上级存在两个及以上直接上级相同（将无歧义词也考虑在内增加证据可信度）
	 */
	private void generateSameFeature() {
		// 如果歧义词不存在则方法不执行
		if (ambiguousSet.size() == 0)
			return;
		Object amArray[] = ambiguousSet.toArray();
		Object allLocArray[] = locationSet.toArray();
		for (int i = 0; i < amArray.length - 1; i++) {
			LocationWrapper pLoc = (LocationWrapper) amArray[i];
			int sameFatherCount = -1;// 因为有跟自己比较，所以基数为-1，这样加1后等于0
			for (int j = 0; j < allLocArray.length; j++) {
				LocationWrapper rLoc = (LocationWrapper) allLocArray[j];
				if (pLoc.isSameFather(rLoc))
					sameFatherCount++;
			}
			if (sameFatherCount > 0) {// 存在共同上级
				createItemFeature(pLoc, FV.SAME_FATHER, ambiguousSet, "306");
			}
		}
	}

	/**
	 * 词与确定无歧义词所有词间的最短路径（只通过根节点以及直接上下级路径）
	 */
	private void generateDistanceFeature() {
		// 如果无歧义词不存在则方法不执行
		if (unambiguousSet.size() == 0 || ambiguousWordMap.size() == 0)
			return;
		Iterator it = ambiguousWordMap.keySet().iterator();
		while (it.hasNext()) {
			String word = (String) it.next();
			HashSet<LocationWrapper> wordSet = ambiguousWordMap.get(word);
			Object wordArray[] = wordSet.toArray();
			int minDistance = 99999999;
			LocationWrapper defaultLoc = null;
			for (int i = 0; i < wordArray.length; i++) {
				LocationWrapper thisLoc = (LocationWrapper) wordArray[i];
				int distance = getDistance(thisLoc, unambiguousSet);
				if (distance < minDistance) {
					minDistance = distance;
					defaultLoc = thisLoc;
				}
			}
			if (defaultLoc != null) {// 找到最短逻辑距离，创建特征
				createItemFeature(defaultLoc, FV.MIN_DISTANCE, wordSet, "334");
			}
		}
	}

	/**
	 * 计算地址与集合中所有地址的逻辑距离求和
	 * 
	 * @param thisLoc
	 * @return
	 */
	private int getDistance(LocationWrapper thisLoc,
			HashSet<LocationWrapper> wordSet) {
		Object locArray[] = wordSet.toArray();
		int distance = 0;
		for (int i = 0; i < locArray.length; i++) {
			LocationWrapper otherLoc = (LocationWrapper) locArray[i];
			distance += thisLoc.getDistance(otherLoc);
		}
		return distance;
	}

	/**
	 * 计算地址与集合中所有地址的逻辑距离求和
	 * 
	 * @param thisLoc
	 * @return
	 */
	private int getDistance(Location thisLoc, HashSet<LocationWrapper> wordSet) {
		Object locArray[] = wordSet.toArray();
		int distance = 0;
		for (int i = 0; i < locArray.length; i++) {
			LocationWrapper otherLoc = (LocationWrapper) locArray[i];
			distance += otherLoc.getDistance(otherLoc);
		}
		return distance;
	}

	/**
	 * 从所有地址的上级中找出一个到所有节点的逻辑距离和最短的上级节点
	 * 
	 * @return
	 */
	private void generateMinDistanceFeature() {
		if (locationSet.size() == 0)
			return;
		if (unambiguousSet.size() == 0 || ambiguousWordMap.size() == 0)
			return;

		HashSet<Location> parentLocationSet = new HashSet();
		Object locArray[] = locationSet.toArray();
		for (int i = 0; i < locArray.length; i++) {
			LocationWrapper loc = (LocationWrapper) locArray[i];
			ArrayList<Location> parentPath = loc.getLocationPath();
			parentLocationSet.addAll(parentPath);
		}
		Object parentArray[] = parentLocationSet.toArray();
		int distanceArray[] = new int[parentArray.length];

		int minDistance = 99999999;
		Location defaultLoc = null;
		for (int k = 0; k < parentArray.length; k++) {
			Location prLoc = (Location) parentArray[k];
			int distance = getDistance(prLoc, locationSet);
			distanceArray[k] = distance;
			if (distance < minDistance) {
				minDistance = distance;
				defaultLoc = prLoc;
			}
		}
		if (defaultLoc != null) {// 找到最短逻辑距离，创建特征
			Iterator it = ambiguousWordMap.keySet().iterator();
			while (it.hasNext()) {
				String word = (String) it.next();
				HashSet<LocationWrapper> wordSet = ambiguousWordMap.get(word);
				Object wordArray[] = wordSet.toArray();
				int minWordDistance = 99999999;
				LocationWrapper minDistLoc = null;
				for (int i = 0; i < wordArray.length; i++) {
					LocationWrapper thisLoc = (LocationWrapper) wordArray[i];
					int distance = thisLoc.getDistance(defaultLoc);
					if (distance < minWordDistance) {
						minWordDistance = distance;
						minDistLoc = thisLoc;
					}
				}
				if (minDistLoc != null) {// 找到距离全局最短路径的上级节点的逻辑距离，创建特征
					createItemFeature(minDistLoc, FV.MIN_PARENT_DISTANCE,
							wordSet, "422");
				}
			}
		}
	}

	/**
	 * 根据特征集合排除歧义并将结果归入无歧义词集合
	 */
	private void removeAmbiguous() {
		if (ambiguousWordMap.size() == 0)
			return;
		// System.out.println("===== removeAmbiguous begin unambiguousSet:"+unambiguousSet);
		// System.out.println("===== removeAmbiguous begin ambiguousSet:"+ambiguousSet);
		Iterator it = ambiguousWordMap.keySet().iterator();
		while (it.hasNext()) {
			String word = (String) it.next();
			LocationFV locationFv = ambiguousFV.get(word);
			// System.out.println("======removeAmbiguous  word："+word+"  fList:"+locationFv.getFeatureList());
			LocationWrapper loc = locationFv.getMostLikelyResult();
			if (loc != null) {
				unambiguousSet.add(loc);
			} else {

			}
		}

		// System.out.println("===== removeAmbiguous end unambiguousSet:"+unambiguousSet);
	}

	/**
	 * 歧义消解计算
	 */
	private void ambiguousProcess() {
		// 对于同义词中存在最高顶层唯一编码的进行特征识别
		generateTopCodeFeature();
		// 歧义词上级在无歧义地址涵盖的字面编码上下范围
		generateUnAmParentCodeFeature();
		// 无歧义词上级在歧义地址涵盖的字面编码上下范围内
		generateAmParentCodeFeature();
		// 歧义词集合中存在直接上下级情况特征抽取 例如：广西 桂林 资源
		generateAmParentChildFeature();
		// 从所有地址的上级中找出一个到所有节点的逻辑距离和最短的上级节点
		generateMinDistanceFeature();
		// 根据特征集合排除歧义并将结果归入无歧义词集合
		removeAmbiguous();
	}

	/**
	 * 将经过歧义消解后的词集合去除BadCase后按照此在原始句子中的位置先后顺序转成ArrayList后返回
	 * 
	 * @return
	 */
	private ArrayList<LocationWrapper> removeBadCase() {
		// System.out.println("===== removeBadCase before:unambiguousSet:"+unambiguousSet);

		// System.out.println("===== removeBadCase unambiguousSet:"+unambiguousSet);
		int MIN_LOW_LEVEL = 3;
		if (unambiguousSet == null || unambiguousSet.size() == 0)
			return null;

		// 根据规则去除交叉歧义（组合与原子的保留组合，类型相同的保留高级别地址，如果级别相同保留地址下标靠前的词）
		HashSet<LocationWrapper> deleteSet = new HashSet();
		Object[] locRetainArray = unambiguousSet.toArray();

		// 按词在句子中的下标从小到大排序
		for (int i = 0; i < locRetainArray.length - 1; i++) {
			LocationWrapper thisWord = (LocationWrapper) locRetainArray[i];
			for (int j = i + 1; j < locRetainArray.length; j++) {
				LocationWrapper anotherWord = (LocationWrapper) locRetainArray[j];
				if (thisWord.getStartIndex() > anotherWord.getStartIndex()) {
					Object tempObj = locRetainArray[i];
					locRetainArray[i] = locRetainArray[j];
					locRetainArray[j] = tempObj;
				}
			}
		}

		for (int i = 0; i < locRetainArray.length; i++) {
			LocationWrapper pLoc = (LocationWrapper) locRetainArray[i];
			int pLocStartIndex = pLoc.getStartIndex();
			int pLocEndIndex = pLoc.getEndIndex();
			for (int j = 0; j < locRetainArray.length; j++) {
				LocationWrapper rLoc = (LocationWrapper) locRetainArray[j];
				if (i == j)
					continue;
				int rLocStartIndex = rLoc.getStartIndex();
				int rLocEndIndex = rLoc.getEndIndex();
				// 相比较的词中有一方属于合并词检测两个词是否存在边界交集，有交集的优先保留合并词，放弃非合并词(用来解决地名重叠歧义给上层应用带来的困扰)
				if ((pLocStartIndex <= rLocStartIndex && pLocEndIndex >= rLocStartIndex)
						|| (pLocStartIndex <= rLocEndIndex && pLocEndIndex >= rLocEndIndex)) {

					if ((pLoc.isMergeFlag() && !rLoc.isMergeFlag())
							|| (!pLoc.isMergeFlag() && rLoc.isMergeFlag())) {
						LocationWrapper mergeLoc = null;
						LocationWrapper atomLoc = null;
						if (pLoc.isMergeFlag()) {
							mergeLoc = pLoc;
							atomLoc = rLoc;
						} else {
							mergeLoc = rLoc;
							atomLoc = pLoc;
						}
						// if(atomLoc.getWord().equals("海南"))
						// System.out.println("=========removeBadCase   1 add Delete atomLoc:"+atomLoc);
						deleteSet.add(atomLoc);
					} else {
						if ((!deleteSet.contains(pLoc))
								&& pLoc.getLocation().getLevel() < rLoc
										.getLocation().getLevel()) {

							// if(rLoc.getWord().equals("海南"))
							// System.out.println("=========removeBadCase   2 add Delete rLoc"+rLoc);

							deleteSet.add(rLoc);
						} else if (pLoc.getLocation().getLevel() > rLoc
								.getLocation().getLevel()
								&& (!deleteSet.contains(rLoc))) {

							// if(pLoc.getWord().equals("海南"))
							// System.out.println("=========removeBadCase   3 add Delete pLoc"+pLoc);

							deleteSet.add(pLoc);
						} else if (pLoc.getLocation().getLevel() == rLoc
								.getLocation().getLevel()) {
							if ((!deleteSet.contains(pLoc))
									&& pLoc.getStartIndex() < rLoc
											.getStartIndex()) {

								// if(rLoc.getWord().equals("海南"))
								// System.out.println("=========removeBadCase   4 add Delete rLoc"+rLoc);

								deleteSet.add(rLoc);
							} else if (pLoc.getStartIndex() >= rLoc
									.getStartIndex()
									&& (!deleteSet.contains(rLoc))) {

								// if(pLoc.getWord().equals("海南"))
								// System.out.println("=========removeBadCase   5 add Delete pLoc"+pLoc);

								deleteSet.add(pLoc);
							}
						}
					}
				}
			}
		}

		// System.out.println("===== removeBadCase deleteSet:"+deleteSet);

		HashSet<LocationWrapper> outWordSet = new HashSet();
		Object wordArray[] = unambiguousSet.toArray();
		for (int i = 0; i < wordArray.length; i++) {
			LocationWrapper thisWord = (LocationWrapper) wordArray[i];

			if (deleteSet.contains(thisWord))
				continue;

			if (thisWord.isMergeFlag()) {
				outWordSet.add(thisWord);
				continue;
			}
			if (thisWord.getLocation().getLevel() <= MIN_LOW_LEVEL) {
				outWordSet.add(thisWord);
				continue;
			}
			
			if (thisWord.getLocation().getLevel() >MIN_LOW_LEVEL && thisWord.getWord().length()>=4) {//对于基层街道企事业单位等层级比较低但地址文字长度较长的认为是确切的地址
				outWordSet.add(thisWord);
				continue;
			}

			boolean hasFather = false;
			for (int j = 0; j < wordArray.length; j++) {
				if (i == j)
					continue;

				LocationWrapper anotherWord = (LocationWrapper) wordArray[j];
				if ((anotherWord.isFather(thisWord) || thisWord
						.isFather(anotherWord))
						&& (!thisWord.getLocation().getCode()
								.equals(anotherWord.getLocation().getCode()))) {
					hasFather = true;
					break;
				}
			}

			if (hasFather) {// 低级别地址在集合中找到上级地址则作为输出，否则认为是错误识别结果
				outWordSet.add(thisWord);
			}
		}
		// 将歧义地址中跟已经消歧的词同词但下标位置不同的添加进去
		Object outArray[] = outWordSet.toArray();
		Iterator it = ambiguousSet.iterator();
		while (it.hasNext()) {
			LocationWrapper amLoc = (LocationWrapper) it.next();
			boolean isSameWord = false;
			for (int i = 0; i < outArray.length; i++) {
				LocationWrapper outLoc = (LocationWrapper) outArray[i];
				if (amLoc.getWord().equals(outLoc.getWord())
						&& amLoc.getLocation().getCode()
								.equals(outLoc.getLocation().getCode())
						&& (amLoc.getStartIndex() != outLoc.getStartIndex())) {
					isSameWord = true;
				}
			}
			if (isSameWord)
				outWordSet.add(amLoc);
		}

		ArrayList<LocationWrapper> outList = new ArrayList();
		if (outWordSet.size() == 0)
			return outList;
		if (outWordSet.size() == 1) {
			outList.addAll(outWordSet);
			return outList;
		}
		Object locArray[] = outWordSet.toArray();
		// 按词在句子中的下标从小到大排序
		for (int i = 0; i < (locArray.length - 1); i++) {
			LocationWrapper thisWord = (LocationWrapper) locArray[i];
			for (int j = i + 1; j < locArray.length; j++) {
				LocationWrapper anotherWord = (LocationWrapper) locArray[j];
				if (((LocationWrapper) locArray[i]).getStartIndex() > ((LocationWrapper) locArray[j])
						.getStartIndex()) {
					Object tempObj = locArray[i];
					locArray[i] = locArray[j];
					locArray[j] = tempObj;
				}
			}
		}
		for (int k = 0; k < locArray.length; k++) {
			outList.add((LocationWrapper) locArray[k]);
		}

		return outList;
	}

	/**
	 * 识别歧义词
	 */
	private void identifyingAmbiguity() {

		Object locArray[] = locationSet.toArray();
		for (int i = 0; i < locArray.length; i++) {
			LocationWrapper pLoc = (LocationWrapper) locArray[i];
			int wordExistCount = 1;
			for (int j = 0; j < locArray.length; j++) {
				// 除自己以外比较是否存在同字符不同地址编码的歧义
				if (i != j) {
					LocationWrapper rLoc = (LocationWrapper) locArray[j];
					// 根据前后字符位置完全一致测算歧义
					if (pLoc.getStartIndex() == rLoc.getStartIndex()
							&& pLoc.getEndIndex() == rLoc.getEndIndex()) {
						wordExistCount++;
					}
				}
			}
			// 词在列表中只存在一次则初步认为是无歧义词
			if (wordExistCount == 1) {
				unambiguousSet.add(pLoc);
			} else {// 次数大于1的歧义情形
				ambiguousSet.add(pLoc);
				HashSet wordSet = ambiguousWordMap.get(pLoc.getWord());
				if (wordSet == null)
					wordSet = new HashSet();
				wordSet.add(pLoc);
				ambiguousWordMap.put(pLoc.getWord(), wordSet);
			}
		}
		// 根据是否存在歧义词来判断原集合的歧义与否标志
		if (ambiguousSet.size() != 0) {
			isAmbiguous = true;
		} else {
			isAmbiguous = false;
		}

		// System.out.println("======== identifyingAmbiguity ambiguousSet:"+ambiguousSet);
		// System.out.println("========unambiguousSet:"+unambiguousSet);
	}

}
