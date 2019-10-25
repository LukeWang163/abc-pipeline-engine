package base.operators.operator.nlp.segment.kshortsegment.predict.algorithm;

import base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils.BaseGraph;
import base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils.BaseVertex;
import base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils.Graph;
import base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils.Path;

import java.util.*;


public class DijkstraShortestPathAlg {
	// Input
	private final BaseGraph graph;

	// Intermediate variables
	private Set<BaseVertex> determinedVertexSet = new HashSet<BaseVertex>();
	private PriorityQueue<BaseVertex> vertexCandidateQueue = new PriorityQueue<BaseVertex>();
	private Map<BaseVertex, Double> startVertexDistanceIndex = new HashMap<BaseVertex, Double>();
	private Map<BaseVertex, BaseVertex> predecessorIndex = new HashMap<BaseVertex, BaseVertex>();

	/**
	 * Default constructor.
	 * 
	 * @param graph
	 */
	public DijkstraShortestPathAlg(final BaseGraph graph) {
		this.graph = graph;
	}

	/**
	 * Clear intermediate variables.
	 */
	public void clear() {
		determinedVertexSet.clear();
		vertexCandidateQueue.clear();
		startVertexDistanceIndex.clear();
		predecessorIndex.clear();
	}

	/**
	 * Getter for the distance in terms of the start vertex
	 * 
	 * @return
	 */
	public Map<BaseVertex, Double> getStartVertexDistanceIndex() {
		return startVertexDistanceIndex;
	}

	/**
	 * Getter for the index of the predecessors of vertices
	 * 
	 * @return
	 */
	public Map<BaseVertex, BaseVertex> getPredecessorIndex() {
		return predecessorIndex;
	}

	/**
	 * Construct a tree rooted at "root" with the shortest paths to the other
	 * vertices.
	 * 
	 * @param root
	 */
	public void getShortestPathTree(BaseVertex root) {
		determineShortestPaths(root, null, true);
	}

	/**
	 * Construct a flower rooted at "root" with the shortest paths from the
	 * other vertices.
	 * 
	 * @param root
	 */
	public void getShortestPathFlower(BaseVertex root) {
		determineShortestPaths(null, root, false);
	}

	/**
	 * Do the work
	 */
	protected void determineShortestPaths(BaseVertex sourceVertex, BaseVertex sinkVertex, boolean isSource2sink) {
	//	System.out.println("===  determineShortestPaths begin 1 sourceVertex:"+sourceVertex+"\tsinkVertex:"+sinkVertex+"\tisSource2sink"+isSource2sink);
		
		// 0. clean up variables
		clear();

		// 1. initialize members
		BaseVertex endVertex = isSource2sink ? sinkVertex : sourceVertex;
		BaseVertex startVertex = isSource2sink ? sourceVertex : sinkVertex;
		startVertexDistanceIndex.put(startVertex, 0d);
		startVertex.setWeight(0d);
		vertexCandidateQueue.add(startVertex);
		
	//	System.out.println("===  determineShortestPaths  2 startVertex:"+startVertex+"\tendVertex:"+endVertex+"\tisSource2sink"+isSource2sink);
		
	//	System.out.println("===  determineShortestPaths  3 vertexCandidateQueue:"+vertexCandidateQueue+"\tstartVertexDistanceIndex:"+startVertexDistanceIndex);

		// 2. start searching for the shortest path
		while (!vertexCandidateQueue.isEmpty()) {
			BaseVertex curCandidate = vertexCandidateQueue.poll();
	//		System.out.println("===  determineShortestPaths  4 while  curCandidate:"+curCandidate+"\tvertexCandidateQueue:"+vertexCandidateQueue);

			if (curCandidate.equals(endVertex)) {
				break;
			}

			determinedVertexSet.add(curCandidate);
			
	//		System.out.println("===  determineShortestPaths  4 while  determinedVertexSet:"+determinedVertexSet);

			updateVertex(curCandidate, isSource2sink);
		}
	}

	/**
	 * Update the distance from the source to the concerned vertex.
	 * 
	 * @param vertex
	 */
	private void updateVertex(BaseVertex vertex, boolean isSource2sink) {
	//	System.out.println("===  updateVertex begin  vertex:"+vertex+"\tisSource2sink"+isSource2sink);
		// 1. get the neighboring vertices
		Set<BaseVertex> neighborVertexList = isSource2sink ? graph.getAdjacentVertices(vertex)
				: graph.getPrecedentVertices(vertex);
		
	//	System.out.println("===  updateVertex 2  neighborVertexList:"+neighborVertexList);

		// 2. update the distance passing on current vertex
		for (BaseVertex curAdjacentVertex : neighborVertexList) {
	//		System.out.println("===  updateVertex 3 for   curAdjacentVertex:"+curAdjacentVertex);
			
			// 2.1 skip if visited before
			if (determinedVertexSet.contains(curAdjacentVertex)) {
				continue;
			}

			// 2.2 calculate the new distance
			double distance = startVertexDistanceIndex.containsKey(vertex) ? startVertexDistanceIndex.get(vertex)
					: Graph.DISCONNECTED;
			
	//		System.out.println("===  updateVertex  for 3  distance:"+distance);

			distance += isSource2sink ? graph.getEdgeWeight(vertex, curAdjacentVertex)
					: graph.getEdgeWeight(curAdjacentVertex, vertex);
			
	//		System.out.println("===  updateVertex  for 4  distance:"+distance);

			// 2.3 update the distance if necessary
			if (!startVertexDistanceIndex.containsKey(curAdjacentVertex)
					|| startVertexDistanceIndex.get(curAdjacentVertex) > distance) {
				startVertexDistanceIndex.put(curAdjacentVertex, distance);
		//		System.out.println("===  updateVertex  for if 5  startVertexDistanceIndex.put:curAdjacentVertex:"+curAdjacentVertex+"\tdistance:"+distance);

				predecessorIndex.put(curAdjacentVertex, vertex);
				
		//		System.out.println("===  updateVertex  for if 6  predecessorIndex.put:curAdjacentVertex:"+curAdjacentVertex+"\tvertex:"+vertex);

				curAdjacentVertex.setWeight(distance);
		//		System.out.println("===  updateVertex  for if 7  curAdjacentVertex.setWeight:distance:"+distance);
				vertexCandidateQueue.add(curAdjacentVertex);
		//		System.out.println("===  updateVertex  for if 8  vertexCandidateQueue.add:dcurAdjacentVertex:"+curAdjacentVertex+"\tvertexCandidateQueue:"+vertexCandidateQueue);
				
				
				
			}
		}
	}

	/**
	 * Note that, the source should not be as same as the sink! (we could extend
	 * this later on)
	 * 
	 * @param sourceVertex
	 * @param sinkVertex
	 * @return
	 */
	public Path getShortestPath(BaseVertex sourceVertex, BaseVertex sinkVertex) {
	//	System.out.println("===  getShortestPath begin 1 sourceVertex:"+sourceVertex+"\tsinkVertex:"+sinkVertex);
		determineShortestPaths(sourceVertex, sinkVertex, true);
		//
		List<BaseVertex> vertexList = new Vector<BaseVertex>();
		double weight = startVertexDistanceIndex.containsKey(sinkVertex) ? startVertexDistanceIndex.get(sinkVertex)
				: Graph.DISCONNECTED;
	//	System.out.println("===  getShortestPath  2 weight:"+weight);
		if (weight != Graph.DISCONNECTED) {
	//		System.out.println("===  getShortestPath 3   weight != Graph.DISCONNECTED   weight="+weight);
			BaseVertex curVertex = sinkVertex;
	//		System.out.println("===  getShortestPath 4   curVertex="+curVertex);
			do {
				vertexList.add(curVertex);
				curVertex = predecessorIndex.get(curVertex);
		//		System.out.println("===  getShortestPath 4 do   curVertex="+curVertex+"\tvertexList:"+vertexList);
			} while (curVertex != null && curVertex != sourceVertex);
			vertexList.add(sourceVertex);
			Collections.reverse(vertexList);
		}
		return new Path(vertexList, weight);
	}

	/**
	 * Calculate the distance from the target vertex to the input vertex using
	 * forward star form. (FLOWER)
	 * 
	 * @param vertex
	 */
	public Path updateCostForward(BaseVertex vertex) {
		double cost = Graph.DISCONNECTED;
		// System.out.println("=====updateCostForward begin
		// cost="+cost+"\tvertex="+vertex.getId());

		// 1. get the set of successors of the input vertex
		Set<BaseVertex> adjVertexSet = graph.getAdjacentVertices(vertex);

		// System.out.println("=====updateCostForward
		// adjVertexSet="+adjVertexSet);

		// 2. make sure the input vertex exists in the index
		if (!startVertexDistanceIndex.containsKey(vertex)) {
			startVertexDistanceIndex.put(vertex, Graph.DISCONNECTED);
			// System.out.println("=====updateCostForward not contains=");
		}

		// 3. update the distance from the root to the input vertex if necessary
		for (BaseVertex curVertex : adjVertexSet) {
			// 3.1 get the distance from the root to one successor of the input
			// vertex
			double distance = startVertexDistanceIndex.containsKey(curVertex) ? startVertexDistanceIndex.get(curVertex)
					: Graph.DISCONNECTED;

			// System.out.println("=====updateCostForward for 3.1
			// curVertex="+curVertex.getId()+"\tdistance="+distance);

			// 3.2 calculate the distance from the root to the input vertex
			distance += graph.getEdgeWeight(vertex, curVertex);

			// System.out.println("=====updateCostForward for 3.2
			// distance="+distance);
			// distance +=
			// ((VariableGraph)graph).get_edge_weight_of_graph(vertex,
			// curVertex);

			// 3.3 update the distance if necessary
			double costOfVertex = startVertexDistanceIndex.get(vertex);

			// System.out.println("=====updateCostForward for 3.3
			// vertex="+vertex.getId()+" costOfVertex="+costOfVertex);
			if (costOfVertex > distance) {
				startVertexDistanceIndex.put(vertex, distance);
				predecessorIndex.put(vertex, curVertex);
				cost = distance;
				// System.out.println("=====updateCostForward for 3.3
				// cost="+cost+"\tvertex="+vertex.getId()+"\tcurVertext="+curVertex.getId());
			}
		}

		// System.out.println("=====updateCostForward for 3.4 cost="+cost);

		// 4. create the subPath if exists
		Path subPath = null;
		if (cost < Graph.DISCONNECTED) {
			subPath = new Path();
			subPath.setWeight(cost);
			List<BaseVertex> vertexList = subPath.getVertexList();
			vertexList.add(vertex);

			BaseVertex selVertex = predecessorIndex.get(vertex);
			while (selVertex != null) {
				vertexList.add(selVertex);
				selVertex = predecessorIndex.get(selVertex);
			}
		}
		// if(subPath!=null)System.out.println("=====updateCostForward for 4
		// subPath="+subPath.getVertexList());
		return subPath;
	}

	/**
	 * Correct costs of successors of the input vertex using backward star form.
	 * (FLOWER)
	 * 
	 * @param vertex
	 */
	public void correctCostBackward(BaseVertex vertex) {
		// System.out.println("=====correctCostBackward begin
		// vertex="+vertex.getId());
		// 1. initialize the list of vertex to be updated
		List<BaseVertex> vertexList = new LinkedList<BaseVertex>();
		vertexList.add(vertex);

		// 2. update the cost of relevant precedents of the input vertex
		while (!vertexList.isEmpty()) {
			BaseVertex curVertex = vertexList.remove(0);
			// System.out.println("=====correctCostBackward while
			// curVertex="+curVertex.getId());
			double costOfCurVertex = startVertexDistanceIndex.get(curVertex);
			// System.out.println("=====correctCostBackward while
			// costOfCurVertex="+costOfCurVertex);
			Set<BaseVertex> preVertexSet = graph.getPrecedentVertices(curVertex);
			// System.out.println("=====correctCostBackward while
			// preVertexSet="+preVertexSet);
			for (BaseVertex preVertex : preVertexSet) {
				double costOfPreVertex = startVertexDistanceIndex.containsKey(preVertex)
						? startVertexDistanceIndex.get(preVertex) : Graph.DISCONNECTED;
				// System.out.println("=====correctCostBackward while for
				// preVertex="+preVertex.getId()+"\tcostOfPreVertex="+costOfPreVertex);
				double freshCost = costOfCurVertex + graph.getEdgeWeight(preVertex, curVertex);
				// System.out.println("=====correctCostBackward while for
				// freshCost="+freshCost);
				if (costOfPreVertex > freshCost) {
					startVertexDistanceIndex.put(preVertex, freshCost);
					predecessorIndex.put(preVertex, curVertex);
					vertexList.add(preVertex);

					// System.out.println("=====correctCostBackward while for if
					// preVertex="+preVertex.getId()+"\t
					// curVertex="+curVertex.getId()+"\t freshCost="+freshCost);

				}
			}
		}
	}

}
