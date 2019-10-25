package base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils;

import java.util.List;
import java.util.Vector;


public class Path implements BaseElementWithWeight {

	private List<BaseVertex> vertexList = new Vector<BaseVertex>();
	private double weight = -1;

	public Path() {
	}

	public Path(List<BaseVertex> vertexList, double weight) {
		this.vertexList = vertexList;
		this.weight = weight;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public List<BaseVertex> getVertexList() {
		return vertexList;
	}

	@Override
	public boolean equals(Object right) {

		if (right instanceof Path) {
			Path rPath = (Path) right;
			return vertexList.equals(rPath.vertexList);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return vertexList.hashCode();
	}

	public String toString() {
		return vertexList.toString() + ":" + weight;
	}
}
