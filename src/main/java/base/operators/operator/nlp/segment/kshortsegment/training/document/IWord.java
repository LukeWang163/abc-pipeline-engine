package base.operators.operator.nlp.segment.kshortsegment.training.document;

import java.io.Serializable;

public interface IWord extends Serializable {
	String getValue();

	String getLabel();
	
	String getShadowWord();

	void setLabel(String label);

	void setValue(String val);
	
}
