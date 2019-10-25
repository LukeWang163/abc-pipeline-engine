package base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils;

public interface BaseVertex {

	public int getId();

	public void setId(int id);

	double getWeight();

	void setWeight(double weight);

	Object getObject();

	void setObject(Object o);
	
	Object getRefObject();
	
	void setRefObject(Object o);

}
