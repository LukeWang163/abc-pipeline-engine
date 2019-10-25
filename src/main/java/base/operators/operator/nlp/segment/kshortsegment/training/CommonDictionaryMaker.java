package base.operators.operator.nlp.segment.kshortsegment.training;

import base.operators.example.ExampleSet;
import base.operators.example.set.HeaderExampleSet;
import base.operators.operator.*;
import base.operators.operator.nlp.segment.kshortsegment.training.document.IWord;
import base.operators.operator.ports.metadata.ModelMetaData;

import java.util.List;

public abstract class CommonDictionaryMaker extends ResultObjectAdapter implements Model {
	static boolean verbose = false;
	public ExampleSet exampleSet;
	private HeaderExampleSet headerExampleSet;
	private ModelMetaData modelMetaData;
	private Operator operator = null;

	/**
	 * 输出词典
	 */
	public DictionaryMaker dictionaryMaker;
	/**
	 * 2元文法词典
	 */
	public NGramDictionaryMaker nGramDictionaryMaker;

	public CommonDictionaryMaker(ExampleSet exampleSet) {
		this.exampleSet = exampleSet;
		if (this.exampleSet != null) {
			this.headerExampleSet = new HeaderExampleSet(this.exampleSet);
			this.modelMetaData = new ModelMetaData(this, true);
		}
		nGramDictionaryMaker = new NGramDictionaryMaker();
		dictionaryMaker = new DictionaryMaker();
	}

	/**
	 * 处理语料，准备词典
	 */
	public void compute(List<List<IWord>> sentenceList) {
		roleTag(sentenceList);
		addToDictionary(sentenceList);
	}

	/**
	 * 加入到词典中，允许子类自定义过滤等等，这样比较灵活
	 *
	 * @param sentenceList
	 */
	abstract protected void addToDictionary(List<List<IWord>> sentenceList);

	/**
	 * 角色标注，如果子类要进行label的调整或增加新的首尾等等，可以在此进行
	 */
	abstract protected void roleTag(List<List<IWord>> sentenceList);

	/**
	 * delivers the set Operator or null if no Operator was set.
	 */
	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}
	@Override
    public HeaderExampleSet getTrainingHeader() {
		return this.headerExampleSet;
	}

    @Override
    public ModelMetaData getModelMetaData() {
        return this.modelMetaData;
    }
	/**
	 * This default implementation returns false. Note that subclasses overriding this method should
	 * also override the method {@link #updateModel(ExampleSet)}.
	 */
	@Override
	public boolean isUpdatable() {
		return false;
	}

	/**
	 * This default implementation throws an {@link UserError}. Subclasses overriding this method to
	 * update the model according to the given example set should also override the method
	 * {@link #isUpdatable()} by delivering true.
	 */
	@Override
	public void updateModel(ExampleSet updateExampleSet) throws OperatorException {
		throw new UserError(null, 135, getClass().getName());
	}

	/**
	 * Throws a UserError since most models should not allow additional parameters during
	 * application. However, subclasses may overwrite this method.
	 */
	@Override
	public void setParameter(String key, Object value) throws OperatorException {
		throw new UnsupportedApplicationParameterError(null, getName(), key);
	}

	/**
	 * The default implementation returns the result of the super class. If the string ends with
	 * model, the substring &quot;model&quot; is removed.
	 */
	@Override
	public String getName() {
		String result = super.getName();
		if (result.toLowerCase().endsWith("model")) {
			result = result.substring(0, result.length() - "model".length());
		}
		return result;
	}

	@Override
	public boolean isInTargetEncoding() {
		return false;
	}
}
