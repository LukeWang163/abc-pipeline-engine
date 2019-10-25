package base.operators.operator.nlp.word2vec;


import base.operators.example.Attribute;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities;
import base.operators.example.set.HeaderExampleSet;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.*;
import base.operators.operator.nlp.word2vec.core.Learn;
import base.operators.operator.nlp.word2vec.core.domain.Neuron;
import base.operators.operator.nlp.word2vec.core.domain.WordEntry;
import base.operators.operator.nlp.word2vec.core.domain.WordNeuron;
import base.operators.operator.ports.metadata.ExampleSetMetaData;
import base.operators.operator.ports.metadata.ModelMetaData;
import base.operators.tools.Ontology;
import base.operators.tools.Tools;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;


public class Word2Vec extends ResultObjectAdapter implements Model{

	private ExampleSet exampleSet;
	private HashMap<String, float[]> wordMap = new HashMap<String, float[]>();
	private int words;
	private int size;
	private int topNSize;
    private ModelMetaData modelMetaData;

	public Word2Vec(ExampleSet exampleSet, Learn learn){
		this.exampleSet = exampleSet;
		this.topNSize = 40;
		this.modelMetaData = new ModelMetaData(new ExampleSetMetaData(exampleSet));
		loadLearnModel(learn);
	}

	/**
	 * 将训练的Learn模型的结果赋值给Word2Vec类
	 *
	 * @param learn
	 * @throws IOException
	 */
	public void loadLearnModel(Learn learn){
		words = learn.wordMap.size();
		size = learn.getLayerSize();

		double[] syn0 = null;
		float vector = 0;
		for (Entry<String, Neuron> element : learn.wordMap.entrySet()) {
			syn0 = ((WordNeuron) element.getValue()).syn0;
			float[] value = new float[size];
			double len = 0;
			for (int j = 0; j < size; j++) {
				vector = ((Double) syn0[j]).floatValue();
				len += vector * vector;
				value[j] = vector;
			}
			len = Math.sqrt(len);
			for (int u = 0; u < size; u++) {
				value[u] /= len;
			}
			wordMap.put(element.getKey(), value);
		}
	}

	/**
	 * This parameter specifies the data types at which the model can be applied on.
	 */
	private ExampleSetUtilities.TypesCompareOption compareDataType;

	/**
	 * This parameter specifies the relation between the training {@link ExampleSet} and the input
	 * {@link ExampleSet} which is needed to apply the model on the input {@link ExampleSet}.
	 */
	private ExampleSetUtilities.SetsCompareOption compareSetSize;

    @Override
    public HeaderExampleSet getTrainingHeader(){
		return new HeaderExampleSet(exampleSet);
	}

	@Override
	public ModelMetaData getModelMetaData() {
		return modelMetaData;
	}


	public ExampleSet performPrediction (ExampleSet exampleSet, Attribute[] predictedLabel) throws OperatorException{
		List<String> predictNames = new ArrayList<>();
    	for (int ii = 0; ii < predictedLabel.length; ii++) {
			predictNames.add(predictedLabel[ii].getName());
		}
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		Attribute targetAttribute = null;
		for (int ll = 0; ll < regularAttributes.length; ll++) {
			if(!predictNames.contains(regularAttributes[ll].getName())){
				targetAttribute = regularAttributes[ll];
			}
		}
		for (int jj = 0; jj < exampleSet.size(); jj++) {
			Example example = exampleSet.getExample(jj);
			String word = example.getValueAsString(targetAttribute);
			float[] vec = getWordVector(word);
			for (int k = 0; k < predictedLabel.length; k++) {
				example.setValue(predictedLabel[k],vec[k]);
			}
		}
		// Create and return a Classification object
		return exampleSet;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		ExampleSet mappedExampleSet = (ExampleSet) exampleSet.clone();
		// create and add prediction attribute
		Attribute[] predcitAttributes = new Attribute[size];
		ExampleTable table = mappedExampleSet.getExampleTable();
		for (int i = 0; i < size; i++) {
			Attribute vec_attribute = AttributeFactory.createAttribute("vec"+i, Ontology.NUMERICAL);
			vec_attribute.clearTransformations();
			table.addAttribute(vec_attribute);
			mappedExampleSet.getAttributes().addRegular(vec_attribute);
			predcitAttributes[i] = vec_attribute;
		}
		ExampleSet result = performPrediction(mappedExampleSet, predcitAttributes);

		// Copy in order to avoid RemappedExampleSets wrapped around each other accumulating over
		// time
		// exampleSet = (ExampleSet) result.clone();
		return result;
	}

	/**
	 * Throws a UserError since most models should not allow additional parameters during
	 * application. However, subclasses may overwrite this method.
	 */
	@Override
	public void setParameter(String key, Object value) throws OperatorException {
		throw new UnsupportedApplicationParameterError(null, getName(), key);
	}

	@Override
	public boolean isUpdatable() {
		return false;
	}

	@Override
	public void updateModel(ExampleSet updateExampleSet) throws OperatorException {
		throw new UserError(null, 135, getClass().getName());
	}

	@Override
	public boolean isInTargetEncoding() {
		return false;
	}

	public void loadJavaModel(byte[] in) {
		try {
			if (in != null) {
				modelLoadToMem(in, this);
				return;
			}
			throw new Exception("word2vec读取模型文件出错！");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 加载模型
	 *
	 * @param result
	 *            模型的
	 * @param
	 * @throws IOException
	 */

	public synchronized static void modelLoadToMem(byte[] result, Object model) {
		try {
			ByteArrayInputStream bi = new ByteArrayInputStream(result);
			ObjectInputStream oi = new ObjectInputStream(bi);
			Object dataObj = oi.readObject();
			Map<String, Object> data = (Map<String, Object>) dataObj;
			Field[] fields = model.getClass().getDeclaredFields();
			for (Entry<String, Object> entry : data.entrySet()) {
				for (Field field : fields) {
					if (field.getName().equals(entry.getKey())) {
						field.setAccessible(!field.isAccessible());
						if (field.getName().equals("wordMap")) {
							field.set(model, (Map<String, float[]>) entry.getValue());
						} else {
							field.set(model, entry.getValue());
						}
						field.setAccessible(!field.isAccessible());
						break;
					}
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final int MAX_SIZE = 50;

	/**
	 * 近义词
	 * 
	 * @return
	 */
	public TreeSet<WordEntry> analogy(String word0, String word1, String word2) {
		float[] wv0 = getWordVector(word0);
		float[] wv1 = getWordVector(word1);
		float[] wv2 = getWordVector(word2);

		if (wv1 == null || wv2 == null || wv0 == null) {
			return null;
		}
		float[] wordVector = new float[size];
		for (int i = 0; i < size; i++) {
			wordVector[i] = wv1[i] - wv0[i] + wv2[i];
		}
		float[] tempVector;
		String name;
		List<WordEntry> wordEntrys = new ArrayList<WordEntry>(topNSize);
		for (Entry<String, float[]> entry : wordMap.entrySet()) {
			name = entry.getKey();
			if (name.equals(word0) || name.equals(word1) || name.equals(word2)) {
				continue;
			}
			float dist = 0;
			tempVector = entry.getValue();
			for (int i = 0; i < wordVector.length; i++) {
				dist += wordVector[i] * tempVector[i];
			}
			insertTopN(name, dist, wordEntrys);
		}
		return new TreeSet<WordEntry>(wordEntrys);
	}

	private void insertTopN(String name, float score, List<WordEntry> wordsEntrys) {
		// TODO Auto-generated method stub
		if (wordsEntrys.size() < topNSize) {
			wordsEntrys.add(new WordEntry(name, score));
			return;
		}
		float min = Float.MAX_VALUE;
		int minOffe = 0;
		for (int i = 0; i < topNSize; i++) {
			WordEntry wordEntry = wordsEntrys.get(i);
			if (min > wordEntry.score) {
				min = wordEntry.score;
				minOffe = i;
			}
		}

		if (score > min) {
			wordsEntrys.set(minOffe, new WordEntry(name, score));
		}

	}

	public Set<WordEntry> distance(String queryWord) {

		float[] center = wordMap.get(queryWord);
		if (center == null) {
			return Collections.emptySet();
		}

		int resultSize = wordMap.size() < topNSize ? wordMap.size() : topNSize;
		TreeSet<WordEntry> result = new TreeSet<WordEntry>();

		double min = Float.MIN_VALUE;
		for (Entry<String, float[]> entry : wordMap.entrySet()) {
			float[] vector = entry.getValue();
			float dist = 0;
			for (int i = 0; i < vector.length; i++) {
				dist += center[i] * vector[i];
			}

			if (dist > min) {
				result.add(new WordEntry(entry.getKey(), dist));
				if (resultSize < result.size()) {
					result.pollLast();
				}
				min = result.last().score;
			}
		}
		result.pollFirst();

		return result;
	}

	public Set<WordEntry> distance(List<String> words) {

		float[] center = null;
		for (String word : words) {
			center = sum(center, wordMap.get(word));
		}

		if (center == null) {
			return Collections.emptySet();
		}

		int resultSize = wordMap.size() < topNSize ? wordMap.size() : topNSize;
		TreeSet<WordEntry> result = new TreeSet<WordEntry>();

		double min = Float.MIN_VALUE;
		for (Entry<String, float[]> entry : wordMap.entrySet()) {
			float[] vector = entry.getValue();
			float dist = 0;
			for (int i = 0; i < vector.length; i++) {
				dist += center[i] * vector[i];
			}

			if (dist > min) {
				result.add(new WordEntry(entry.getKey(), dist));
				if (resultSize < result.size()) {
					result.pollLast();
				}
				min = result.last().score;
			}
		}
		result.pollFirst();

		return result;
	}

	private float[] sum(float[] center, float[] fs) {
		// TODO Auto-generated method stub

		if (center == null && fs == null) {
			return null;
		}

		if (fs == null) {
			return center;
		}

		if (center == null) {
			return fs;
		}

		for (int i = 0; i < fs.length; i++) {
			center[i] += fs[i];
		}

		return center;
	}

	/**
	 * 得到词向量
	 * 
	 * @param word
	 * @return
	 */
	public float[] getWordVector(String word) {
		return wordMap.get(word);
	}

	public static float readFloat(InputStream is) throws IOException {
		byte[] bytes = new byte[4];
		is.read(bytes);
		return getFloat(bytes);
	}

	/**
	 * 读取一个float
	 * 
	 * @param b
	 * @return
	 */
	public static float getFloat(byte[] b) {
		int accum = 0;
		accum = accum | (b[0] & 0xff) << 0;
		accum = accum | (b[1] & 0xff) << 8;
		accum = accum | (b[2] & 0xff) << 16;
		accum = accum | (b[3] & 0xff) << 24;
		return Float.intBitsToFloat(accum);
	}

	/**
	 * 读取一个字符串
	 *
	 * @param dis
	 * @return
	 * @throws IOException
	 */
	private static String readString(DataInputStream dis) throws IOException {
		// TODO Auto-generated method stub
		byte[] bytes = new byte[MAX_SIZE];
		byte b = dis.readByte();
		int i = -1;
		StringBuilder sb = new StringBuilder();
		while (b != 32 && b != 10) {
			i++;
			bytes[i] = b;
			b = dis.readByte();
			if (i == 49) {
				sb.append(new String(bytes));
				i = -1;
				bytes = new byte[MAX_SIZE];
			}
		}
		sb.append(new String(bytes, 0, i + 1));
		return sb.toString();
	}

	public int getTopNSize() {
		return topNSize;
	}

	public void setTopNSize(int topNSize) {
		this.topNSize = topNSize;
	}

	public HashMap<String, float[]> getWordMap() {
		return wordMap;
	}

	public int getWords() {
		return words;
	}

	public int getSize() {
		return size;
	}

	@Override
	public String getName() {
		return "Word2Vec";
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();

		result.append("Total number of words: " + getWords() + Tools.getLineSeparator());
		result.append("The dimension of vector: " + getSize() + Tools.getLineSeparator());

		return result.toString();
	}


}
