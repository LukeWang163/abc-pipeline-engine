package base.operators.operator.nlp.segment.kshortsegment.training;

import base.operators.example.Attribute;
import base.operators.example.Attributes;
import base.operators.example.Example;
import base.operators.example.ExampleSet;
import base.operators.example.set.ExampleSetUtilities;
import base.operators.example.table.AttributeFactory;
import base.operators.example.table.ExampleTable;
import base.operators.operator.OperatorException;
import base.operators.operator.nlp.segment.kshortsegment.predict.segment.Segment;
import base.operators.operator.nlp.segment.kshortsegment.predict.segment.TokenizeAnnotator;
import base.operators.operator.nlp.segment.kshortsegment.training.document.CompoundWord;
import base.operators.operator.nlp.segment.kshortsegment.training.document.IWord;
import base.operators.operator.nlp.segment.kshortsegment.training.document.Word;
import base.operators.operator.ports.metadata.ModelMetaData;
import base.operators.tools.Ontology;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class NatureDictionaryMaker extends CommonDictionaryMaker{
	/**
	 * This parameter specifies the data types at which the model can be applied on.
	 */
	private ExampleSetUtilities.TypesCompareOption compareDataType;

	/**
	 * This parameter specifies the relation between the training {@link ExampleSet} and the input
	 * {@link ExampleSet} which is needed to apply the model on the input {@link ExampleSet}.
	 */
	private ExampleSetUtilities.SetsCompareOption compareSetSize;

	public NatureDictionaryMaker(ExampleSet exampleSet) {
		super(exampleSet);
	}

	@Override
	public ModelMetaData getModelMetaData() {
		return new ModelMetaData(this, true);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
//        ExampleSet mappedExampleSet = RemappedExampleSet.create(exampleSet, getTrainingHeader(), false, true);
//        checkCompatibility(mappedExampleSet);
		ExampleSet exampleSetCopy = (ExampleSet) exampleSet.clone();
		Attribute predictedLabel = createPredictionAttributes(exampleSetCopy);
		ExampleSet result = performPrediction(exampleSetCopy, predictedLabel);

//        // Copy in order to avoid RemappedExampleSets wrapped around each other accumulating over
//        // time
//        exampleSet = (ExampleSet) exampleSet.clone();
//        copyPredictedLabel(result, exampleSet);

		return result;
	}

	public ExampleSet performPrediction (ExampleSet examples, Attribute predictedAttribute){
		if(examples.getAttributes().createRegularAttributeArray().length!=1){
			throw new IllegalArgumentException("Only allowed one regular attribute!");
		}
		Segment segment = new TokenizeAnnotator(this);
		for (int jj = 0; jj < examples.size(); jj++) {
			Example example = examples.getExample(jj);
			for (Attribute attribute : example.getAttributes()) {
				String docContent = example.getValueAsString(attribute);
				String result = segment.segment(docContent, true);
				example.setPredictedLabel(predictedAttribute.getMapping().mapString(result));
			}
		}
		// Create and return a Classification object
		return examples;
	}

	protected void checkCompatibility(ExampleSet exampleSet) throws OperatorException {
		ExampleSet trainingHeaderSet = getTrainingHeader();
		// check given constraints (might throw an UserError)
		ExampleSetUtilities.checkAttributesMatching(getOperator(), trainingHeaderSet.getAttributes(),
				exampleSet.getAttributes(), compareSetSize, compareDataType);
		// check number of attributes
		if (exampleSet.getAttributes().size() != trainingHeaderSet.getAttributes().size()) {
			logWarning("The number of regular attributes of the given example set does not fit the number of attributes of the training example set, training: "
					+ trainingHeaderSet.getAttributes().size() + ", application: " + exampleSet.getAttributes().size());
		} else {
			// check order of attributes
			Iterator<Attribute> trainingIt = trainingHeaderSet.getAttributes().iterator();
			Iterator<Attribute> applyIt = exampleSet.getAttributes().iterator();
			while (trainingIt.hasNext() && applyIt.hasNext()) {
				if (!trainingIt.next().getName().equals(applyIt.next().getName())) {
					logWarning("The order of attributes is not equal for the training and the application example set. This might lead to problems for some models.");
					break;
				}
			}
		}

		// check if all training attributes are part of the example set and have the same value
		// types and values
		for (Attribute trainingAttribute : trainingHeaderSet.getAttributes()) {
			String name = trainingAttribute.getName();
			Attribute attribute = exampleSet.getAttributes().getRegular(name);
			if (attribute == null) {
				logWarning("The given example set does not contain a regular attribute with name '" + name
						+ "'. This might cause problems for some models depending on this particular attribute.");
			} else {
				if (trainingAttribute.getValueType() != attribute.getValueType()) {
					logWarning("The value types between training and application differ for attribute '" + name
							+ "', training: " + Ontology.VALUE_TYPE_NAMES[trainingAttribute.getValueType()]
							+ ", application: " + Ontology.VALUE_TYPE_NAMES[attribute.getValueType()]);
				} else {
					// check nominal values
					if (trainingAttribute.isNominal()) {
						if (trainingAttribute.getMapping().size() != attribute.getMapping().size()) {
							logWarning("The number of nominal values is not the same for training and application for attribute '"
									+ name
									+ "', training: "
									+ trainingAttribute.getMapping().size()
									+ ", application: "
									+ attribute.getMapping().size());
						} else {
							for (String v : trainingAttribute.getMapping().getValues()) {
								int trainingIndex = trainingAttribute.getMapping().getIndex(v);
								int applicationIndex = attribute.getMapping().getIndex(v);
								if (trainingIndex != applicationIndex) {
									logWarning("The internal nominal mappings are not the same between training and application for attribute '"
											+ name + "'. This will probably lead to wrong results during model application.");
									break;
								}
							}
						}
					}
				}
			}
		}
	}
	/**
	 * This method creates prediction attributes like the predicted label and confidences if needed.
	 */
	protected Attribute createPredictionAttributes(ExampleSet exampleSet) {
		// create and add prediction attribute
		Attribute predictedLabel = AttributeFactory.createAttribute(Attributes.PREDICTION_NAME, Ontology.STRING);
		predictedLabel.clearTransformations();
		ExampleTable table = exampleSet.getExampleTable();
		table.addAttribute(predictedLabel);
		exampleSet.getAttributes().setPredictedLabel(predictedLabel);

		return predictedLabel;
	}


	@Override
	protected void addToDictionary(List<List<IWord>> sentenceList) {
		// 制作NGram词典
		for (List<IWord> wordList : sentenceList) {

			boolean isSingleTag = false;
			for (IWord word : wordList) {
				if (word instanceof CompoundWord)
					isSingleTag = true;
			}
			if (!isSingleTag) {
				IWord pre = null;
				for (IWord word : wordList) {
					// 制作词性词频词典
					dictionaryMaker.add(word);
					if (pre != null) {
						nGramDictionaryMaker.addPair(pre, word);
					}
					pre = word;
				}
			} else {
				//else会走到？ zls20190320
				IWord pre = null;
				IWord preCOmpound = null;
				for (IWord word : wordList) {
					dictionaryMaker.add(word);
					if (word instanceof CompoundWord) {
						CompoundWord comWord = (CompoundWord) word;
						List<Word> innerList = comWord.innerList;
						if (pre != null) {
							nGramDictionaryMaker.addPair(pre, comWord);
						}
						if (preCOmpound != null) {
							nGramDictionaryMaker.addPair(preCOmpound, comWord);
						}
						for (int i = 0; i < innerList.size(); i++) {
							Word innerWord = innerList.get(i);
							if (pre != null) {
								nGramDictionaryMaker.addPair(pre, innerWord);
							}
							pre = innerWord;
						}
					} else {
						if (pre != null) {
							nGramDictionaryMaker.addPair(pre, word);
						}
						if (preCOmpound != null) {
							nGramDictionaryMaker.addPair(preCOmpound, word);
						}
						pre = word;
						preCOmpound = null;
					}
				}
			}
		}
	}

	@Override
	protected void roleTag(List<List<IWord>> sentenceList) {
		int i = 0;
		for (List<IWord> wordList : sentenceList) {
			LinkedList<IWord> wordLinkedList = (LinkedList<IWord>) wordList;
			wordLinkedList.addFirst(new Word(Predefine.TAG_BIGIN, Nature.begin.toString()));
			wordLinkedList.addLast(new Word(Predefine.TAG_END, Nature.end.toString()));
		}
	}
}
