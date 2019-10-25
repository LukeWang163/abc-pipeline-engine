package base.operators.operator.nlp.ner.location;

import java.io.InputStream;
import java.util.*;

public class LocAnnotate {

	static NSDictionary DICT;;
	/**
	 * 所有匹配好的节点，
	 */
	private HashSet<LocationWrapper> locations[];

	private ArrayList<List<Integer>> prexIdArrayList;

	private int length;

	private boolean hasExtendedCharset;

	public LocAnnotate(ArrayList<List<Integer>> prexIdArrayList, int length,
			boolean hasExtendedCharset, int type, InputStream stream) {
		locations = new HashSet[length];
		this.prexIdArrayList = prexIdArrayList;
		this.length = length;
		this.hasExtendedCharset = hasExtendedCharset;
		DICT = NSDictionary.getIstance(type, stream);
		initLocations(type, stream);
	}

	private void initLocations(int type, InputStream stream) {
		for (int i = 0; i < length; i++) {
			List<Integer> commonPrefixList = prexIdArrayList.get(i);
			locations[i] = new HashSet();
			for (int wIndex = 0; wIndex < commonPrefixList.size(); wIndex++) {

				String locWord = DICT.getLocationWords().get(
						commonPrefixList.get(wIndex));
				Set<Location> locWordSet = DICT.getLocationWordMap().get(
						locWord);
			//	System.out.println("   word:"+locWord);
				Iterator it = locWordSet.iterator();
				while (it.hasNext()) {
					Location location = (Location) it.next();
					
					LocationWrapper locWrapper = new LocationWrapper(locWord,
							location, i, type, stream);
					locations[i].add(locWrapper);
				}
			}
		}
	}

	private void mergeLocation(LocationWrapper prevLoc, LocationWrapper thisLoc) {
		LocationWrapper mergedLocation = prevLoc.mergeLocation(thisLoc);
		locations[prevLoc.getStartIndex()].add(mergedLocation);
	}

	private void mergeLocations(HashSet<LocationWrapper> prevLocation,
			HashSet<LocationWrapper> thisLocation) {
		if (prevLocation == null || prevLocation.size() == 0
				|| thisLocation == null || thisLocation.size() == 0)
			return;
		Object[] preLocArray = prevLocation.toArray();
		Object[] thisLocArray = thisLocation.toArray();
		// System.out.println("Annotate mergeLocations() method  preLocArray:"+preLocArray.length+"\tthisLocArray:"+thisLocArray.length);
		for (int i = 0; i < preLocArray.length; i++) {
			LocationWrapper prevLoc = (LocationWrapper) preLocArray[i];
			// System.out.println("Annotate mergeLocations() method  loop i:"+i+"\tprevLoc"+prevLoc);
			for (int j = 0; j < thisLocArray.length; j++) {
				LocationWrapper thisLoc = (LocationWrapper) thisLocArray[j];
				// System.out.println("Annotate mergeLocations() method  loop i:"+i+"\tj:"+j+"\tthisLoc"+thisLoc);
				if (isSubLocation(prevLoc, thisLoc)
						&& prevLoc.getEndIndex() == (thisLoc.getStartIndex() - 1)) {
					// System.out.println("Annotate mergeLocations() method  loop i:"+i+"\tj:"+j+"\tmergeLocation");
					if (prevLoc.getWord().length() != 1
							&& !nameHashCodeParentBrother(prevLoc, thisLoc)) // 防止出现京、沪、鲁等地址简称往下匹配干扰常用词成词
						mergeLocation(prevLoc, thisLoc);
				}

			}
		}
	}

	/**
	 * 判断下级地址名称是否存在与上级地址的同级地址同名的地址 防止出现：天津石家庄,code:120119103231
	 * 
	 * @param prevLoc
	 * @param thisLoc
	 * @return
	 */
	private boolean nameHashCodeParentBrother(LocationWrapper prevLoc,
			LocationWrapper thisLoc) {
		if (thisLoc.getLocation().getLevel() <= 3)
			return false;// 对于县级以上节点直接默认可以上下匹配，如中国湖南，中国青岛
		Set<Location> wordLocSet = DICT.getLocationWordMap().get(
				thisLoc.getWord());
		if (wordLocSet.size() <= 1)
			return false;

		Object wordObj[] = wordLocSet.toArray();
		int prevLevel = prevLoc.getLocation().getLevel();
		for (int i = 0; i < wordObj.length; i++) {
			Location loc = (Location) wordObj[i];
			if (loc.getLevel() <= 2)
				return true;
			/*
			 * int thisLevel = loc.getLevel(); if (thisLevel == prevLevel) {
			 * return true; } if (thisLevel == prevLevel + 1) return true; if
			 * (thisLevel == prevLevel - 1) return true;
			 */
		}
		return false;
	}

	public HashSet<LocationWrapper> process() {
		HashSet<LocationWrapper> tempSet = new HashSet();
		HashSet<LocationWrapper> deleteLocSet = new HashSet();
		HashSet<LocationWrapper> locationSet = new HashSet();
		for (int i = 0; i < locations.length; i++) {
			tempSet.addAll(locations[i]);
		}
		//System.out.println("TempSet:" + tempSet);

		Object[] locArray = tempSet.toArray();
		// System.out.println("=====tempSet:"+tempSet.size());
		for (int i = 0; i < locArray.length; i++) {
			LocationWrapper pLoc = (LocationWrapper) locArray[i];
			// boolean pMergeFlag=pLoc.isMergeFlag();
			int pLocStartIndex = pLoc.getStartIndex();
			int pLocEndIndex = pLoc.getEndIndex();
			boolean isSubString = false;
			for (int j = 0; j < locArray.length; j++) {
				LocationWrapper rLoc = (LocationWrapper) locArray[j];
				if (pLoc != rLoc) {
					int rLocStartIndex = rLoc.getStartIndex();
					int rLocEndIndex = rLoc.getEndIndex();
					if (pLocStartIndex >= rLocStartIndex
							&& pLocEndIndex <= rLocEndIndex) {
						if (pLocStartIndex == rLocStartIndex
								&& pLocEndIndex == rLocEndIndex) {
							isSubString = false;
						} else {
							isSubString = true;
							break;
						}
					}
				}
			}

			if (!isSubString) {
				locationSet.add(pLoc);
			}
		}
		//System.out.println("locationSet:" + locationSet);
		Object[] locRetainArray = locationSet.toArray();
		for (int i = 0; i < locRetainArray.length; i++) {
			LocationWrapper pLoc = (LocationWrapper) locRetainArray[i];
			int pLocStartIndex = pLoc.getStartIndex();
			int pLocEndIndex = pLoc.getEndIndex();
			for (int j = 0; j < locRetainArray.length; j++) {// j原来开始于1改成开始于0
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
						deleteLocSet.add(atomLoc);
					}
				}
			}
		}

		locationSet.removeAll(deleteLocSet);
		return locationSet;
	}

	/**
	 * 判断前后地址是否构成上下级关系
	 * 
	 * @param prevLoc
	 * @param thisLoc
	 * @return
	 */
	private boolean isSubLocation(LocationWrapper prevLoc,
			LocationWrapper thisLoc) {
		String prevCode = prevLoc.getLocation().getCode();
		String parentCode = thisLoc.getLocation().getParentCode();
		boolean loopFlag = true;
		// System.out.println("Annotate isSubLocation  begin prevLoc:"+prevLoc+"\tthisLoc:"+thisLoc);
		while (loopFlag) {
			// System.out.println("Annotate isSubLocation  while loop loopFlag:"+loopFlag+"\tprevCode:"+prevCode+"\tparentCode:"+parentCode+"\tthisLoc:"+thisLoc);
			if (prevCode.equals(parentCode))
				return true;
			if (parentCode == null || parentCode.equals(DICT.TOPCODE)
					|| parentCode.equals("-1") || parentCode.equals(""))
				return false;
			Location parentLoc = DICT.getLocMap().get(parentCode);
			if (parentLoc == null)
				return false;
			parentCode = parentLoc.getParentCode();
		}
		return false;
	}

	/**
	 * 标注算法入口
	 * 
	 * @return
	 */
	public ArrayList<LocationWrapper> annotate(int type, InputStream stream) {
		// System.out.println("Annotate merge  begin.....");
		// 词合并（对于中国湖南，中华人民共和国治安等可能存在合并后多编码或者合并后与常用概念存在冲突相悖的地方暂时不做处理）
		merge();
		// System.out.println("Annotate merge  end.....");
		// 去除被长串覆盖的短串，但依然存在一词多编码的歧义现象
		// return process();
		HashSet<LocationWrapper> locationSet = process();
		// System.out.println("=============annotateNew Process:" +
		 //locationSet);
		LocationClustering clustering = new LocationClustering(locationSet, type, stream);

		// 通过层次聚类的算法进行歧义消解，并返回结果
		return clustering.process();

	}

	public void merge() {
		for (int i = 0; i < this.length; i++) {
			// System.out.println("Annotate merge() method  loop:"+i);
			HashSet<LocationWrapper> prevLocation = getPrevLocations(i);
			// System.out.println("Annotate merge() method  loop:"+i+"  has Get Prev");
			if (prevLocation != null && prevLocation.size() != 0) {
				HashSet<LocationWrapper> thisLocation = locations[i];
				if (thisLocation.size() != 0) {
					// System.out.println("Annotate merge() method  loop:"+i+"  Begin Merge  \nprevLocation:"+prevLocation+"\nthisLocation"+thisLocation);
					mergeLocations(prevLocation, thisLocation);
				}
			}
		}
	}

	private HashSet<LocationWrapper> getPrevLocations(int thisIndex) {
		HashSet<LocationWrapper> prevLocation = new HashSet();
		for (int i = 0; i < thisIndex; i++) {
			HashSet<LocationWrapper> lastLocation = locations[i];
			Iterator it = lastLocation.iterator();
			while (it.hasNext()) {
				LocationWrapper locWord = (LocationWrapper) it.next();
				if (locWord.getEndIndex() == (thisIndex - 1))
					prevLocation.add(locWord);
			}
		}
		return prevLocation;
	}

}
