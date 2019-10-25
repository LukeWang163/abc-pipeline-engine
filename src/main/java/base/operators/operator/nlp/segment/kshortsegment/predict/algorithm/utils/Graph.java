package base.operators.operator.nlp.segment.kshortsegment.predict.algorithm.utils;

import java.util.*;


public class Graph implements BaseGraph {

	public static final double DISCONNECTED = Double.MAX_VALUE;

	// index of fan-outs of one vertex
	protected Map<Integer, Set<BaseVertex>> fanoutVerticesIndex = new HashMap<Integer, Set<BaseVertex>>();

	// index for fan-ins of one vertex
	protected Map<Integer, Set<BaseVertex>> faninVerticesIndex = new HashMap<Integer, Set<BaseVertex>>();

	// index for edge weights in the graph
	protected Map<Pair<Integer, Integer>, Double> vertexPairWeightIndex = new HashMap<Pair<Integer, Integer>, Double>();

	// index for vertices in the graph
	protected Map<Integer, BaseVertex> idVertexIndex = new HashMap<Integer, BaseVertex>();

	// list of vertices in the graph
	protected List<BaseVertex> vertexList = new Vector<BaseVertex>();

	// the number of vertices in the graph
	protected int vertexNum = 0;

	// the number of arcs in the graph
	protected int edgeNum = 0;
	
	private BaseVertex sourceVertex;
	
	private BaseVertex sinkVertex;


	/**
	 * Constructor 2
	 * 
	 * @param graph
	 */
	public Graph(final Graph graph) {
		vertexNum = graph.vertexNum;
		edgeNum = graph.edgeNum;
		vertexList.addAll(graph.vertexList);
		idVertexIndex.putAll(graph.idVertexIndex);
		faninVerticesIndex.putAll(graph.faninVerticesIndex);
		fanoutVerticesIndex.putAll(graph.fanoutVerticesIndex);
		vertexPairWeightIndex.putAll(graph.vertexPairWeightIndex);
	}

	/**
	 * Default constructor
	 */
	public Graph(List<BaseVertex> vertexList) {
		this.vertexList=vertexList;
		for(int i=0;i<vertexList.size();i++){
			Vertex v=(Vertex) vertexList.get(i);
			idVertexIndex.put(v.getId(), v);		
		}
	}

	/**
	 * Clear members of the graph.
	 */
	public void clear() {
		Vertex.reset();
		vertexNum = 0;
		edgeNum = 0;
		vertexList.clear();
		idVertexIndex.clear();
		faninVerticesIndex.clear();
		fanoutVerticesIndex.clear();
		vertexPairWeightIndex.clear();
	}

	/**
	 * Note that this may not be used externally, because some other members in
	 * the class should be updated at the same time.
	 * 
	 * @param startVertexId
	 * @param endVertexId
	 * @param weight
	 */
	public void addEdge(int startVertexId, int endVertexId, double weight) {
		// actually, we should make sure all vertices ids must be correct.
		if (!idVertexIndex.containsKey(startVertexId) || !idVertexIndex.containsKey(endVertexId)
				|| startVertexId == endVertexId) {
			throw new IllegalArgumentException(
					"The edge from " + startVertexId + " to " + endVertexId + " does not exist in the graph.");
		}

		// update the adjacent-list of the graph
		Set<BaseVertex> fanoutVertexSet = new HashSet<BaseVertex>();
		if (fanoutVerticesIndex.containsKey(startVertexId)) {
			fanoutVertexSet = fanoutVerticesIndex.get(startVertexId);
		}
		fanoutVertexSet.add(idVertexIndex.get(endVertexId));
		fanoutVerticesIndex.put(startVertexId, fanoutVertexSet);
		//
		Set<BaseVertex> faninVertexSet = new HashSet<BaseVertex>();
		if (faninVerticesIndex.containsKey(endVertexId)) {
			faninVertexSet = faninVerticesIndex.get(endVertexId);
		}
		faninVertexSet.add(idVertexIndex.get(startVertexId));
		faninVerticesIndex.put(endVertexId, faninVertexSet);
		// store the new edge
		vertexPairWeightIndex.put(new Pair<Integer, Integer>(startVertexId, endVertexId), weight);
		++edgeNum;
	}
	/**
	protected void addVertex(List<Vertex>vertexList){
		
	}
*/

	public Set<BaseVertex> getAdjacentVertices(BaseVertex vertex) {
		return fanoutVerticesIndex.containsKey(vertex.getId()) ? fanoutVerticesIndex.get(vertex.getId())
				: new HashSet<BaseVertex>();
	}

	public Set<BaseVertex> getPrecedentVertices(BaseVertex vertex) {
		return faninVerticesIndex.containsKey(vertex.getId()) ? faninVerticesIndex.get(vertex.getId())
				: new HashSet<BaseVertex>();
	}

	public double getEdgeWeight(BaseVertex source, BaseVertex sink) {
		return vertexPairWeightIndex.containsKey(new Pair<Integer, Integer>(source.getId(), sink.getId()))
				? vertexPairWeightIndex.get(new Pair<Integer, Integer>(source.getId(), sink.getId())) : DISCONNECTED;
	}

	/**
	 * Set the number of vertices in the graph
	 * 
	 * @param num
	 */
	public void setVertexNum(int num) {
		vertexNum = num;
	}

	/**
	 * Return the vertex list in the graph.
	 */
	public List<BaseVertex> getVertexList() {
		return vertexList;
	}

	/**
	 * Get the vertex with the input id.
	 * 
	 * @param id
	 * @return
	 */
	public BaseVertex getVertex(int id) {
		return idVertexIndex.get(id);
	}

	public int getEdgeNum() {
		return edgeNum;
	}
	
	public String toString(){
		String edgeStr="";
	 Iterator it=	vertexPairWeightIndex.keySet().iterator();
	 while(it.hasNext()){
		 Pair<Integer, Integer> pair=(Pair<Integer, Integer>) it.next();
		 Double weight=vertexPairWeightIndex.get(pair);
		Integer first= pair.first();
		Integer second= pair.second();
		edgeStr=edgeStr+"[pair first:"+idVertexIndex.get(first)+",second:"+idVertexIndex.get(second)+",weight:"+weight+"]\n";
	 }
		return "Graph Vertex:"+ vertexList.toString()+"\n"+edgeStr;
	}

	
	@Override
	public BaseVertex getSourceVertex() {
		// TODO Auto-generated method stub
		return sourceVertex;
	}

	@Override
	public BaseVertex getSinkVertex() {
		// TODO Auto-generated method stub
		return sinkVertex;
	}

	@Override
	public void setSourceVertex(BaseVertex source) {
		sourceVertex=source;
		
	}

	@Override
	public void setSinkVertex(BaseVertex sink) {
		sinkVertex=sink;		
	}
	
	
}
