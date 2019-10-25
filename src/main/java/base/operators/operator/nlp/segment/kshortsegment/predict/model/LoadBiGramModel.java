package base.operators.operator.nlp.segment.kshortsegment.predict.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.TreeMap;

public class LoadBiGramModel {
	public TreeMap<String, TreeMap<String, Double>>[] load(InputStream inputStream) {
		TreeMap<String, TreeMap<String, Double>>[] resultMap = new TreeMap[2];
		TreeMap<String, TreeMap<String, Double>> biGramMap = new TreeMap<String, TreeMap<String, Double>>();// 词与词转移概率
		TreeMap<String, TreeMap<String, Double>> biPosGramMap = new TreeMap<String, TreeMap<String, Double>>();// 词与词间不同词性转移概率
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					inputStream, "UTF-8"));
			String row;
			//int q = 0;
			while ((row = reader.readLine()) != null) {
//				q++;
//				if(q%1000 == 0){
//					System.out.print(q+";");
//				}
				String[] params = row.split("\\s");
				if (params.length >= 3) {
					String LA = params[0];
					String LB = params[1];
					Double count = Double.parseDouble(params[2]);
					TreeMap<String, Double> bMap = biGramMap.get(LA);
					if (bMap == null) {
						bMap = new TreeMap<String, Double>();
					}
					bMap.put(LB, count);
					biGramMap.put(LA, bMap);

					String posKey = LA + "\t" + LB;
					if (params.length > 3) {
						TreeMap<String, Double> posTransMap = new TreeMap();
						for (int i = 3; i < params.length; i++) {
							String posTransFrqTxt = params[i];
							String[] transFrq = posTransFrqTxt.split("##");
							if (transFrq.length == 3) {
								String srcPos = transFrq[0];
								String destPos = transFrq[1];
								Double posFrq = Double.parseDouble(transFrq[2]);
								posTransMap
										.put(srcPos + "##" + destPos, posFrq);
							}
						}
						biPosGramMap.put(posKey, posTransMap);
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		resultMap[0] = biGramMap;
		resultMap[1] = biPosGramMap;
		return resultMap;
	}

	public TreeMap<String, TreeMap<String, Double>>[] load(List<String> data) {
		TreeMap<String, TreeMap<String, Double>>[] resultMap = new TreeMap[2];
		TreeMap<String, TreeMap<String, Double>> biGramMap = new TreeMap<String, TreeMap<String, Double>>();// 词与词转移概率
		TreeMap<String, TreeMap<String, Double>> biPosGramMap = new TreeMap<String, TreeMap<String, Double>>();// 词与词间不同词性转移概率



		for (String row:data) {
			String[] params = row.split("\\s");
			if (params.length >= 3) {
				String LA = params[0];
				String LB = params[1];
				Double count = Double.parseDouble(params[2]);
				TreeMap<String, Double> bMap = biGramMap.get(LA);
				if (bMap == null) {
					bMap = new TreeMap<String, Double>();
				}
				bMap.put(LB, count);
				biGramMap.put(LA, bMap);

				String posKey = LA + "\t" + LB;
				if (params.length > 3) {
					TreeMap<String, Double> posTransMap = new TreeMap();
					for (int i = 3; i < params.length; i++) {
						String posTransFrqTxt = params[i];
						String[] transFrq = posTransFrqTxt.split("##");
						if (transFrq.length == 3) {
							String srcPos = transFrq[0];
							String destPos = transFrq[1];
							Double posFrq = Double.parseDouble(transFrq[2]);
							posTransMap
									.put(srcPos + "##" + destPos, posFrq);
						}
					}
					biPosGramMap.put(posKey, posTransMap);
				}
			}
		}

		resultMap[0] = biGramMap;
		resultMap[1] = biPosGramMap;
		return resultMap;
	}
}
