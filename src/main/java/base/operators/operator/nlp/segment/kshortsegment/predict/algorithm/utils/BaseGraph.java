package base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils;

import java.util.List;
import java.util.Set;

public interface BaseGraph {

	List<BaseVertex> getVertexList();

	double getEdgeWeight(BaseVertex source, BaseVertex sink);

	Set<BaseVertex> getAdjacentVertices(BaseVertex vertex);

	Set<BaseVertex> getPrecedentVertices(BaseVertex vertex);

	BaseVertex getSourceVertex();

	BaseVertex getSinkVertex();

	void setSourceVertex(BaseVertex source);

	void setSinkVertex(BaseVertex sink);

}
