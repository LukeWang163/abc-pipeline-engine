package base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils;

public class Vertex implements BaseVertex, Comparable<Vertex> {

	private static int currentVertexNum = 0; // Uniquely identify each vertex
	private int id = currentVertexNum++;
	private double weight = 0;

	private Object object;

	private Object refObject;

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double status) {
		weight = status;
	}

	public int compareTo(Vertex rVertex) {
		double diff = this.weight - rVertex.weight;
		if (diff > 0) {
			return 1;
		} else if (diff < 0) {
			return -1;
		} else {
			return 0;
		}
	}

	public static void reset() {
		currentVertexNum = 0;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object o) {
		object = o;

	}

	public String toString() {
		return "[id:" + id + ",weight:" + weight + ",object:"
				+ object.toString() + "]";
	}

	public Object getRefObject() {
		return refObject;
	}

	public void setRefObject(Object o) {
		refObject = o;

	}
}
