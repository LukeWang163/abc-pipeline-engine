package base.operators.operator.nlp.segment.kshortsegment.predict.model;

import base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils.TransformMatrix;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

public class TransformMatrixModel<E extends Enum<E>> {

	//public final static String path = "/model/CoreDict.tr.txt";
//	public static String path = "/model/CoreDict.tr.txt";

	Class<E> enumType;
	/**
	 * 内部标签下标最大值不超过这个值，用于矩阵创建
	 */
	private int ordinaryMax;

	/**
     * 储存每个标签出现的次数
     */
	private double total[];

    /**
     * 所有标签出现的总次数
     */
	private double totalFrequency;

	private double[][] transititon_probability;

	public TransformMatrixModel(Class<E> enumType) {
		this.enumType = enumType;
	}

	//++++++++++++++++++++++++++实例集合(根据文件的HashCode创建的)+++++++++++++++++++++++++++++++++++++++++++++++++++++
	static HashMap<String,TransformMatrix> transformMatrixMap = new HashMap<String,TransformMatrix>();
	public TransformMatrix getInstance(String hashTrans,List<String> data){
		if(transformMatrixMap.get(hashTrans)==null){
			transformMatrixMap.put(hashTrans,load(data));
		}
		return transformMatrixMap.get(hashTrans);
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	protected E convert(String label) {
		return Enum.valueOf(enumType, label);
	}
	
	/**
     * 获取e的总频次
     *
     * @param e
     * @return
     */
    public double getTotalFrequency(E e)
    {
        return total[e.ordinal()];
    }

    /**
     * 获取所有标签的总频次
     *
     * @return
     */
    public double getTotalFrequency()
    {
        return totalFrequency;
    }
    
    public double[][] getTransititonProbability(){
    	return transititon_probability;
    }
    
    
	public TransformMatrix load(List<String> data) {
		TransformMatrix transformMatrix = null;


		String line = data.get(0);
		String[] _param = line.split(",");

		// 为了制表方便，第一个label是废物，所以要抹掉它
		String[] labels = new String[_param.length - 1];
		System.arraycopy(_param, 1, labels, 0, labels.length);

		int[] ordinaryArray = new int[labels.length];
		ordinaryMax = 0;
		for (int i = 0; i < ordinaryArray.length; ++i) {
			ordinaryArray[i] = convert(labels[i]).ordinal();
			ordinaryMax = Math.max(ordinaryMax, ordinaryArray[i]);
		}
		++ordinaryMax;

		int matrix[][] = new int[ordinaryMax][ordinaryMax];
		for (int i = 0; i < ordinaryMax; ++i) {
			for (int j = 0; j < ordinaryMax; ++j) {
				matrix[i][j] = 0;
			}
		}

		//System.out.println("======================ordinaryMax="+ordinaryMax);

		// 之后就描述了矩阵
		for (int m=1; m<data.size(); m++) {
			line = data.get(m);
			String[] paramArray = line.split(",");
			int currentOrdinary = convert(paramArray[0]).ordinal();
			for (int i = 0; i < ordinaryArray.length; ++i) {
				matrix[currentOrdinary][ordinaryArray[i]] = Integer.valueOf(paramArray[1 + i]);
			}
		}


		// 需要统计一下每个标签出现的次数
		//double total[] = new double[ordinaryMax];
		total = new double[ordinaryMax];
		for (int j = 0; j < ordinaryMax; ++j) {
			total[j] = 0;
			for (int i = 0; i < ordinaryMax; ++i) {
				total[j] += matrix[i][j];
				total[j] += matrix[j][i];//此处代码可能有问题？？？怎么计算两次
			}
		}
		for (int j = 0; j < ordinaryMax; ++j) {
			total[j] -= matrix[j][j];
		}
		double totalFrequency = 0;
		for (int j = 0; j < ordinaryMax; ++j) {
			totalFrequency += total[j];
		}

		// 下面计算HMM四元组
		int[] states = ordinaryArray;
		double[] start_probability = new double[ordinaryMax];
		for (int s : states) {
			double frequency = total[s] + 1e-8;
			start_probability[s] = -Math.log(frequency / totalFrequency);
		}

		//double[][] transititon_probability = new double[ordinaryMax][ordinaryMax];
		transititon_probability = new double[ordinaryMax][ordinaryMax];
		for (int from : states) {
			for (int to : states) {
				double frequency = matrix[from][to] + 1e-8;
				transititon_probability[from][to] = -Math.log(frequency / total[from]);
			}
		}

		transformMatrix = new TransformMatrix(enumType, matrix, states, start_probability, transititon_probability,
				total, totalFrequency, labels);


		return transformMatrix;
	}

	public TransformMatrix load(InputStream inputStream) {
		TransformMatrix transformMatrix = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			String line = reader.readLine();
			String[] _param = line.split(",");

			// 为了制表方便，第一个label是废物，所以要抹掉它
			String[] labels = new String[_param.length - 1];
			System.arraycopy(_param, 1, labels, 0, labels.length);

			int[] ordinaryArray = new int[labels.length];
			ordinaryMax = 0;
			for (int i = 0; i < ordinaryArray.length; ++i) {
				ordinaryArray[i] = convert(labels[i]).ordinal();
				ordinaryMax = Math.max(ordinaryMax, ordinaryArray[i]);
			}
			++ordinaryMax;

			int matrix[][] = new int[ordinaryMax][ordinaryMax];
			for (int i = 0; i < ordinaryMax; ++i) {
				for (int j = 0; j < ordinaryMax; ++j) {
					matrix[i][j] = 0;
				}
			}

			//System.out.println("======================ordinaryMax="+ordinaryMax);

			// 之后就描述了矩阵
			while ((line = reader.readLine()) != null) {
				String[] paramArray = line.split(",");
				int currentOrdinary = convert(paramArray[0]).ordinal();
				for (int i = 0; i < ordinaryArray.length; ++i) {
					matrix[currentOrdinary][ordinaryArray[i]] = Integer.valueOf(paramArray[1 + i]);
				}
			}
			reader.close();

			// 需要统计一下每个标签出现的次数
			//double total[] = new double[ordinaryMax];
			total = new double[ordinaryMax];
			for (int j = 0; j < ordinaryMax; ++j) {
				total[j] = 0;
				for (int i = 0; i < ordinaryMax; ++i) {
					total[j] += matrix[i][j];
					total[j] += matrix[j][i];//此处代码可能有问题？？？怎么计算两次
				}
			}
			for (int j = 0; j < ordinaryMax; ++j) {
				total[j] -= matrix[j][j];
			}
			double totalFrequency = 0;
			for (int j = 0; j < ordinaryMax; ++j) {
				totalFrequency += total[j];
			}

			// 下面计算HMM四元组
			int[] states = ordinaryArray;
			double[] start_probability = new double[ordinaryMax];
			for (int s : states) {
				double frequency = total[s] + 1e-8;
				start_probability[s] = -Math.log(frequency / totalFrequency);
			}

			//double[][] transititon_probability = new double[ordinaryMax][ordinaryMax];
			transititon_probability = new double[ordinaryMax][ordinaryMax];
			for (int from : states) {
				for (int to : states) {
					double frequency = matrix[from][to] + 1e-8;
					transititon_probability[from][to] = -Math.log(frequency / total[from]);
				}
			}

			transformMatrix = new TransformMatrix(enumType, matrix, states, start_probability, transititon_probability,
					total, totalFrequency, labels);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return transformMatrix;
	}

}
