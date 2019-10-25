package base.operators.operator.nlp.segment.kshortsegment.predict.segment;

import base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils.*;
import base.operators.operator.nlp.segment.kshortsegment.predict.model.BiGramModel;
import base.operators.operator.nlp.segment.kshortsegment.predict.model.TransformMatrixModel;
import base.operators.operator.nlp.segment.kshortsegment.predict.tagger.POS;
import idsw.nlp.read.ReadFileAsStream;

import java.util.*;

public class WordGraphFactory {

//	private final static String trMatrixPath = "/nlp/segment/model/CoreDict.tr.txt";
//	private final static String bigramPath = "/nlp/segment/model/CoreDict.ngram.txt";

	static TransformMatrix transformMatrix = null;
	static BiGramModel biGramModel = null;

	public WordGraphFactory(){
		if (transformMatrix == null) {
			//transformMatrix = new TransformMatrixModel<POS>(POS.class).load(trMatrixPath);
			transformMatrix = new TransformMatrixModel<POS>(POS.class).load(ReadFileAsStream.readSegmentModelTr());

		}
		if(biGramModel == null){
			//biGramModel = BiGramModel.getInstance(bigramPath);
			biGramModel = BiGramModel.getInstance(ReadFileAsStream.readSegmentModelNgram());
		}
	}

	public WordGraphFactory(List<String>transData, List<String>gramData){
		if (transformMatrix == null) {
			transformMatrix = new TransformMatrixModel<POS>(POS.class)
					.load(transData);
		}
		if(biGramModel == null){
			biGramModel = BiGramModel.getIstance(gramData);
		}
	}
	
	//public static long calweitime = 0;

	private static double getWordTransFrequency(Word from, Word to) {
		double wordTransFrq = biGramModel.getBiFrequency(
				from.getRealWord(), to.getRealWord());// 统计实际词的转移次数
		// System.out.println("RealWord From:"+from.getRealWord()+"\t"+from.getFrequency()+"\tTo:"+to.getRealWord()+"\t"+to.getFrequency()+"\tTran:"+wordTransFrq);

		if (wordTransFrq == 0) {// 如果实际二元词组在二元词典中不存在，则统计等效词的二元词组的概率
			wordTransFrq = biGramModel.getBiFrequency(from.getWord(),
					to.getWord());// 统计等效词的转移次数：这里只考虑两个均为等效词的转移次数
			// System.out.println("Word From:"+from.getRealWord()+"\t"+from.getFrequency()+"\tTo:"+to.getRealWord()+"\t"+to.getFrequency()+"\tTran:"+wordTransFrq);
		}
		return wordTransFrq;
	}

	// 统计词与词之间的不同词性的转移概率
	private static double getWordTransFrequency(BaseVertex srcVertex,
												BaseVertex desVertex) {

		Word from = (Word) srcVertex.getObject();
		Word to = (Word) desVertex.getObject();
		String fromPos = ((POS) srcVertex.getRefObject()).toString();
		String toPos = ((POS) desVertex.getRefObject()).toString();
		double wordTransFrq = biGramModel.getBiFrequency(
				from.getRealWord(), fromPos, to.getRealWord(), toPos);// 统计实际词的转移次数

		if (wordTransFrq == 0)
			wordTransFrq = biGramModel.getBiFrequency(from.getWord(),
					fromPos, to.getWord(), toPos);// 实际词概率为0就统计虚拟词的转移次数
		if (wordTransFrq == 0) {
			// 对于语料中低频次的电话号码 邮件等转换概率按照基础词性统计
			if (fromPos.startsWith(POS.n.toString())
					&& fromPos.length() >= 2
					|| (toPos.startsWith(POS.n.toString()) && toPos.length() >= 2)) {
				wordTransFrq = getPosTransFrequency(srcVertex, desVertex) + 1;
			}
		}
		return wordTransFrq;
	}

	private static void addVertex(List<BaseVertex> vertexList, BaseVertex vertex) {
		boolean vertexExist = false;
		for (int i = 0; i < vertexList.size(); i++) {
			BaseVertex thisVertex = vertexList.get(i);
			if (thisVertex.getId() == vertex.getId()) {
				vertexExist = true;
				break;
			}
		}
		if (!vertexExist)
			vertexList.add(vertex);
	}

	public static Graph generateGraph(WordNet wordNet) {

		ArrayList edgeList = new ArrayList();

		LinkedList<Word> words[] = wordNet.getWords();
		// System.out.println("GenerateGraph:generateGraph:words[].length===="+words.length);
		List<BaseVertex> vertexList = new Vector<BaseVertex>();
		Word begin = wordNet.getB();
		BaseVertex beginVertex = new Vertex();
		beginVertex.setId(begin.getId());
		beginVertex.setObject(begin);
		beginVertex.setRefObject(begin.getPosTag());
		vertexList.add(beginVertex);

		// addVertex(vertexList, beginVertex);
		// vertexList.add(beginVertex);

		Word end = wordNet.getE();
		BaseVertex endVertex = new Vertex();
		endVertex.setId(end.getId());
		endVertex.setObject(end);
		endVertex.setRefObject(end.getPosTag());
		vertexList.add(endVertex);
		// addVertex(vertexList, endVertex);

		for (int row = 0; row < words.length - 1; ++row) {
			List<Word> vertexListFrom = words[row];
			// System.out.println("row=="+row);
			for (Word from : vertexListFrom) {
				assert from.getRealWord().length() > 0 : "空节点会导致死循环！";
				// System.out.println("from.getRealWord().length()===="+from.getRealWord().length());
				// int toIndex = row + from.getLength();
				int toIndex = row + from.getRealWord().length();
				// +++++++++++++++++++++++++++++++++++++++++
				if (toIndex > words.length - 1) {
					continue;
				}

				TreeMap<POS, Double> preWordPosMap = from.getPosMap();
				Iterator preIt = preWordPosMap.keySet().iterator();
				int VERTEX_FROM_ID_OFFSET = 0;
				while (preIt.hasNext()) {
					// 取得前词词性
					POS preWordPOS = (POS) preIt.next();
					// 取得前词词频
					double preWordFrq = preWordPosMap.get(preWordPOS);
					BaseVertex preVertex = new Vertex();
					preVertex.setId(from.getId() + VERTEX_FROM_ID_OFFSET);
					int startVertexId = preVertex.getId();
					preVertex.setObject(from);
					preVertex.setRefObject(preWordPOS);// 解决一个Word中在最终只能存储一个确定的词性的问题，解决一词多词性多节点的问题
					// vertexList.add(preVertex);
					addVertex(vertexList, preVertex);
					VERTEX_FROM_ID_OFFSET++;

					// ++++++++++++++++++++++++++++++++++++++++++
					for (Word to : words[toIndex]) {

						TreeMap<POS, Double> toWordPosMap = to.getPosMap();
						Iterator toIt = toWordPosMap.keySet().iterator();

						int VERTEX_TO_ID_OFFSET = 0;
						while (toIt.hasNext()) {
							// 取得后词词性
							POS toWordPOS = (POS) toIt.next();
							// 取得前词词频
							double toWordFrq = toWordPosMap.get(toWordPOS);

							BaseVertex toVertex = new Vertex();
							toVertex.setId(to.getId() + VERTEX_TO_ID_OFFSET);
							int endVertexId = toVertex.getId();
							toVertex.setObject(to);
							toVertex.setRefObject(toWordPOS);// 解决一个Word中在最终只能存储一个确定的词性的问题，解决一词多词性多节点的问题
							// vertexList.add(preVertex);
							addVertex(vertexList, toVertex);
							VERTEX_TO_ID_OFFSET++;
							BaseVertex edge[] = new BaseVertex[2];
							edge[0] = preVertex;
							edge[1] = toVertex;
							edgeList.add(edge);

						}

					}

				}

			}
		}
		Graph graph = new VariableGraph(vertexList);
		graph.setSourceVertex(beginVertex);
		graph.setSinkVertex(endVertex);

		for (int i = 0; i < edgeList.size(); i++) {
			BaseVertex edge[] = (BaseVertex[]) edgeList.get(i);
			BaseVertex preVertex = edge[0];
			BaseVertex toVertex = edge[1];
			
			//long startTimeCALW = System.currentTimeMillis();    //获取开始时间
			double weight = calWeight(preVertex, toVertex);
			//long endTimeCALW = System.currentTimeMillis();    //获取结束时间
			//calweitime += endTimeCALW-startTimeCALW;
			
			graph.addEdge(preVertex.getId(), toVertex.getId(), weight);

		}

		// System.out.println("====Graph:" + graph);
		return graph;
	}

	private static double calWeight(BaseVertex srcVertex, BaseVertex desVertex) {
		Word from = (Word) srcVertex.getObject();
		Word to = (Word) desVertex.getObject();
		double dCurFreqency = from.getFrequency();
		if (dCurFreqency == 0) {
			dCurFreqency = 1; // 防止发生除零错误
		}
		double dCurFreqencyRate = dCurFreqency / Define.MAX_FREQUENCY;// 当前词存在的统计概率

		// double wordTransFrq = getWordTransFrequency(from, to);

		double wordTransFrq = getWordTransFrequency(srcVertex, desVertex);
		// if(wordTransFrq!=0)wordTransFrq+=1;
		wordTransFrq += 0.2;

		double wordTransFrqRate = wordTransFrq / Define.MAX_FREQUENCY;// 词与词间的转移概率

		double posTranfrqRate = getPosTransFrequency(srcVertex, desVertex);// 词性与词性间转移概率

		POS srcPos = (POS) srcVertex.getRefObject();

		double myPosFrq = from.getPOSFrequency(srcPos);// 本词的词性

		// System.out.println("srcPos:" + srcPos + "\tmyPosFrq" + myPosFrq);

		double totalPosFrq = transformMatrix.getTotalFrequency(srcPos);

		if (totalPosFrq == 0)
			totalPosFrq = Define.MAX_FREQUENCY / 10;// 防止发生词性除0错误

		double posRate = myPosFrq / totalPosFrq;// 本词的词性处于对应词性的词在语料中出现的总次数

		// double value = from.getWord().length() * to.getWord().length()
		// * dCurFreqencyRate * wordTransFrq * wordTransFrqRate * posRate
		// * posTranfrqRate;

		double value = from.getWord().length() * to.getWord().length()
				* dCurFreqencyRate * wordTransFrqRate * wordTransFrq * posRate
				* posTranfrqRate;

		// System.out.println("value:"+value+"\tweight:"+Math.log(value)+"\tweigth1:"+Math.log(1/value));

		double weight = Math.log(1 / value);
		/*
		 * System.out.println( from.getWord() + "\t" + to.getWord() + "\t" +
		 * "wordTransFrqRate\t" + wordTransFrqRate + "\tposTranfrqRate\t" +
		 * posTranfrqRate + "\twordTransFrq\t" + wordTransFrq + "\tposRate\t" +
		 * posRate +"\tdCurFreqency\t"+dCurFreqency+
		 * "\twordTransFrq\t"+wordTransFrq
		 * +"\tvalue\t"+value+"\tweight\t"+Math.log
		 * (value)+"\tweigth1\t"+Math.log(1/value));
		 */
		if (weight < 0)
			weight = -weight;
		return weight;

	}

	private static double getPosTransFrequency(BaseVertex from, BaseVertex to) {

		POS fromPos = (POS) from.getRefObject();
		POS toPos = (POS) to.getRefObject();

		double transFrq = transformMatrix.getFrequency(fromPos, toPos);

		if (transFrq == 0)
			transFrq = 10;

		double totalFrq = transformMatrix.getTotalFrequency();

		return transFrq / totalFrq;
	}



}
