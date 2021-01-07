package edu.kit.ipd.parse.topic_extraction_common.graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.ext.CSVExporter;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.IntegerComponentNameProvider;
import org.jgrapht.ext.StringComponentNameProvider;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.WeightedPseudograph;
import org.jgrapht.graph.builder.UndirectedWeightedGraphBuilder;
import org.jgrapht.traverse.DepthFirstIterator;

/**
 * @author Jan Keim
 *
 */
public class TopicGraph implements Serializable {
	// private static final Logger logger =
	// LoggerFactory.getLogger(TopicGraph.class);

	private static final long serialVersionUID = 6011335886418476642L;

	private WeightedPseudograph<WikiVertex, DefaultWeightedEdge> graph;
	private List<WikiVertex> senses = new ArrayList<>();
	private HashMap<WikiVertex, List<WikiVertex>> vertexToConnectedSenseVertices = new HashMap<>();
	private HashMap<Integer, Set<WikiVertex>> cachedVerticesConnectivity = new HashMap<>();

	private PageRank<WikiVertex, DefaultWeightedEdge> pageRank = null;

	public TopicGraph() {
		graph = new WeightedPseudograph<>(DefaultWeightedEdge.class);
	}

	public TopicGraph(WikiVertex sense) {
		graph = new WeightedPseudograph<>(DefaultWeightedEdge.class);
		senses.add(sense);
	}

	public static TopicGraph createTopicGraph(List<TopicGraph> senseGraphs) {
		if ((senseGraphs == null) || (senseGraphs.size() == 0)) {
			throw new IllegalArgumentException("List of sense graphs is empty or null!");
		}

		TopicGraph topicGraph = new TopicGraph();

		// fill HashMap (vertexToConnectedSenseVertices) with Vertex and a List
		// of SenseVertices
		for (TopicGraph tg : senseGraphs) {
			for (WikiVertex v : tg.getVertexSet()) {
				List<WikiVertex> list = topicGraph.vertexToConnectedSenseVertices.get(v);
				if (list == null) {
					list = new ArrayList<>();
				}
				list.addAll(tg.getSenses());
				topicGraph.vertexToConnectedSenseVertices.put(v, list);
			}
		}

		// add the sense graphs to the topic graph (merging)
		for (int i = 0; i < senseGraphs.size(); i++) {
			topicGraph.addSenseGraph(senseGraphs.get(i));
		}

		return topicGraph;
	}

	private static WeightedPseudograph<WikiVertex, DefaultWeightedEdge> mergeUnderlyingGraphs(
			WeightedPseudograph<WikiVertex, DefaultWeightedEdge> g1, WeightedPseudograph<WikiVertex, DefaultWeightedEdge> g2) {
		UndirectedWeightedGraphBuilder<WikiVertex, DefaultWeightedEdge, WeightedPseudograph<WikiVertex, DefaultWeightedEdge>> graphBuilder = new UndirectedWeightedGraphBuilder<>(
				new WeightedPseudograph<WikiVertex, DefaultWeightedEdge>(DefaultWeightedEdge.class));

		graphBuilder.addGraph(g1);
		graphBuilder.addGraph(g2);

		return graphBuilder.build();
	}

	/**
	 * Merges two graphs and returns the (new) resulting graph.
	 *
	 * @param g1
	 *            first graph
	 * @param g2
	 *            second graph
	 * @return merge of the two graphs
	 */
	public static TopicGraph mergeGraphs(TopicGraph g1, TopicGraph g2) {
		TopicGraph retTG = new TopicGraph();
		retTG.graph = TopicGraph.mergeUnderlyingGraphs(g1.graph, g2.graph);
		retTG.senses.addAll(g1.senses);
		retTG.senses.addAll(g2.senses);
		return retTG;
	}

	protected Graph<WikiVertex, DefaultWeightedEdge> getUnderlyingGraph() {
		return graph;
	}

	public void addSenseGraph(TopicGraph senseGraph, WikiVertex sense) {
		senses.add(sense);
		graph = TopicGraph.mergeUnderlyingGraphs(graph, senseGraph.graph);
	}

	public void addSenseGraph(TopicGraph senseGraph) {
		senses.addAll(senseGraph.senses);
		graph = TopicGraph.mergeUnderlyingGraphs(graph, senseGraph.graph);
	}

	public void addSense(WikiVertex sense) {
		senses.add(sense);
	}

	public List<WikiVertex> getSenses() {
		return senses;
	}

	public void increaseWeights() {
		for (DefaultWeightedEdge e : graph.edgeSet()) {
			double weight = graph.getEdgeWeight(e);
			graph.setEdgeWeight(e, weight + 1.0);
		}
	}

	/**
	 * Adds a vertex to the graph
	 *
	 * @param s
	 *            the Vertex
	 * @return whether the operation succeeded
	 */
	public synchronized boolean addVertex(WikiVertex v) {
		return graph.addVertex(v);
	}

	/**
	 * Removes a vertex to the graph
	 *
	 * @param s
	 *            the Vertex
	 * @return whether the operation succeeded
	 */
	public synchronized boolean removeVertex(WikiVertex v) {
		return graph.removeVertex(v);
	}

	public synchronized Optional<WikiVertex> getVertex(String label) {
		return graph.vertexSet().stream().filter(v -> v.getLabel().equals(label)).findFirst();
	}

	public synchronized double getAvgVertexWeight(WikiVertex v) {
		return graph.edgesOf(v).stream().mapToDouble(e -> graph.getEdgeWeight(e)).average().orElse(1.0);
	}

	/**
	 * Adds an edge to the graph
	 *
	 * @param v1
	 *            Vertex one
	 * @param v2
	 *            Vertex two
	 * @return The resulting {@link DefaultEdge}
	 */
	public synchronized DefaultWeightedEdge addEdge(WikiVertex v1, WikiVertex v2) {
		DefaultWeightedEdge e = graph.addEdge(v1, v2);
		graph.setEdgeWeight(e, 1.0);
		return e;
	}

	/**
	 * Returns the initial vertices (initial senses) for a given Vertex
	 *
	 * @param v
	 *            given vertex
	 * @return initial vertices
	 */
	public List<WikiVertex> getInitialVerticesFor(WikiVertex v) {
		return vertexToConnectedSenseVertices.get(v);
	}

	/**
	 * Returns the maximum of connectivity, which is the highest number of initial
	 * vertices for any vertex.
	 *
	 * @return maximum of connectivity
	 */
	public int getMaxSenseConnectivity() {
		if (vertexToConnectedSenseVertices != null) {
			return vertexToConnectedSenseVertices.values().stream().map(Collection::size).max(Integer::compareTo).orElse(-1);
		}
		return -1;
	}

	/**
	 * Returns vertices that have the given connectivity
	 *
	 * @param connectivity
	 *            wanted connectivity
	 * @return Set of vertices that have the given connectivity
	 */
	public Set<WikiVertex> getVerticesWithSenseConnectivity(int connectivity) {
		// check if cache is already created. If not, create it
		if (cachedVerticesConnectivity.isEmpty()) {
			for (Entry<WikiVertex, List<WikiVertex>> entry : vertexToConnectedSenseVertices.entrySet()) {
				int entryConnectivity = entry.getValue().size();
				Set<WikiVertex> connSet = cachedVerticesConnectivity.get(entryConnectivity);
				if (connSet == null) {
					connSet = new HashSet<>();
					cachedVerticesConnectivity.put(entryConnectivity, connSet);
				}
				connSet.add(entry.getKey());
			}
		}
		// get the vertices with the wanted connectivity
		Set<WikiVertex> maxConnectedVertices = cachedVerticesConnectivity.get(connectivity);
		if (maxConnectedVertices == null) {
			maxConnectedVertices = new HashSet<>();
		}
		return maxConnectedVertices;
	}

	/**
	 * Returns the degree of the provided Vertex
	 *
	 * @param v
	 *            (Name of the) Vertex
	 * @return degree of the vertex
	 */
	public int degreeOf(WikiVertex v) {
		return graph.degreeOf(v);
	}

	/**
	 * Returns the length of the shortest path between the provided nodes, If there
	 * is no path, {@link Integer#MAX_VALUE} will be returned.
	 *
	 * @param v1
	 *            First node
	 * @param v2
	 *            Second node
	 * @return length of shortest path or {@link Integer#MAX_VALUE}, if not
	 *         connected by a path
	 */
	public int shortestPathLength(WikiVertex v1, WikiVertex v2) {
		DijkstraShortestPath<WikiVertex, DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(graph);
		GraphPath<WikiVertex, DefaultWeightedEdge> path = dijkstra.getPath(v1, v2);
		return path == null ? -99 : path.getVertexList().size();
	}

	/**
	 * Calculates the PageRank scores for the vertices
	 *
	 * @return Map of the PageRank scores
	 */
	private Map<WikiVertex, Double> calculatePageRankScores() {
		pageRank = new PageRank<>(graph);
		return pageRank.getScores();
	}

	/**
	 * Returns lazily the PageRank scores. If already computed before, scores won't
	 * be calculated again
	 *
	 * @return Map of the PageRank scores
	 */
	protected Map<WikiVertex, Double> getPageRankScores() {
		if (pageRank == null) {
			return calculatePageRankScores();
		}
		return pageRank.getScores();
	}

	/**
	 * Returns the amount of vertices in the graph
	 *
	 * @return the amount of vertices in the graph
	 */
	public int getVerticesSize() {
		return graph.vertexSet().size();
	}

	public Set<WikiVertex> getVertexSet() {
		return graph.vertexSet();
	}

	/**
	 * Returns the amount of edges in the graph
	 *
	 * @return the amount of edges in the graph
	 */
	public int getEdgesSize() {
		return graph.edgeSet().size();
	}

	public boolean containsVertex(WikiVertex v) {
		return graph.containsVertex(v);
	}

	public Map<WikiVertex, Double> getCentralityScores() {
		return getBiasedRanking();
	}

	private Map<WikiVertex, Double> getBiasedRanking() {
		Map<WikiVertex, Double> scores = new BiasedPageRank<>(graph, senses).getScores();
		return scores;
	}

	public boolean checkAdjacency(WikiVertex v1, WikiVertex v2) {
		return graph.containsEdge(v1, v2);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((senses == null) ? 0 : senses.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TopicGraph)) {
			return false;
		}
		TopicGraph other = (TopicGraph) obj;
		if (senses == null) {
			if (other.senses != null) {
				return false;
			}
		} else if (!senses.equals(other.senses)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("TopicGraph\n");
		s.append("Vertices: " + getVerticesSize() + "\n");
		s.append("Edges:    " + getEdgesSize() + "\n");
		Iterator<WikiVertex> iter = new DepthFirstIterator<>(graph);
		while (iter.hasNext()) {
			WikiVertex vertex = iter.next();
			s.append("Vertex " + vertex + " is connected to: " + graph.edgesOf(vertex).toString());
			s.append("\n");
		}
		return s.toString();
	}

	public static void exportGraphToDotFile(TopicGraph topicGraph, String directory) {
		Graph<WikiVertex, DefaultWeightedEdge> graph = topicGraph.graph;
		IntegerComponentNameProvider<WikiVertex> p1 = new IntegerComponentNameProvider<>();
		StringComponentNameProvider<WikiVertex> p2 = new StringComponentNameProvider<>();
		DOTExporter<WikiVertex, DefaultWeightedEdge> exporter = new DOTExporter<>(p1, p2, null);
		String targetDirectory = directory;
		new File(targetDirectory).mkdirs();
		try {
			exporter.exportGraph(graph, new FileWriter(targetDirectory + "graph.dot"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void exportGraphToCsvFile(TopicGraph topicGraph, String directory) {
		Graph<WikiVertex, DefaultWeightedEdge> graph = topicGraph.graph;
		CSVExporter<WikiVertex, DefaultWeightedEdge> exporter = new CSVExporter<>();
		String targetDirectory = directory;
		new File(targetDirectory).mkdirs();
		try {
			exporter.exportGraph(graph, new FileWriter(targetDirectory + "graph.csv"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
