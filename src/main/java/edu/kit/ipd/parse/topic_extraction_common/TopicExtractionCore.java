package edu.kit.ipd.parse.topic_extraction_common;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.topic_extraction_common.graph.TopicGraph;
import edu.kit.ipd.parse.topic_extraction_common.graph.WikiVertex;
import edu.kit.ipd.parse.topic_extraction_common.ontology.DBPediaConnector;
import edu.kit.ipd.parse.topic_extraction_common.ontology.ResourceConnector;

/**
 * @author Jan Keim
 *
 */
public class TopicExtractionCore {
	private static final String ERR_NO_TOPICS_IN_GRAPH = "Topics are not annotated to the graph.";
	private static final Logger logger = LoggerFactory.getLogger(TopicExtractionCore.class);
	public static final String TOPIC_ATTRIBUTE = "topic";
	public static final String TOPICS_NODE_TYPE = "topics";

	private int numTopics = -1;
	private int maxTopics = 8;
	private TopicSelectionMethod topicSelectionMethod = TopicSelectionMethod.CombinedConnectivity;

	/**
	 * Timeouts are handed that 1/5th of the time is allowed for the first hop
	 */
	private int timeoutFirstHop = 60 * 1; // 1 Minute
	private int timeoutSecondHop = 60 * 4; // 4 Minutes

	/** Cache to speed up creating sense graphs */
	private final HashMap<String, TopicGraph> graphCache;

	private ResourceConnector resourceConnector;

	public TopicExtractionCore() {
		this.graphCache = new HashMap<>();
		this.numTopics = -1;
		this.resourceConnector = new DBPediaConnector(DBPediaConnector.DEFAULT_SERVICE_URL);
	}

	public TopicExtractionCore(String url) {
		this();
		this.resourceConnector = new DBPediaConnector(url);
	}

	public TopicExtractionCore(ResourceConnector ressourceConnector) {
		this();
		this.resourceConnector = ressourceConnector;
	}

	/**
	 * Gets the timeout (for creating the sense graphs). Note, that for the
	 * first hop the maximum allowed time will be one quarter of the overall
	 * timeout
	 *
	 * @return the timeout.
	 */
	public int getTimeout() {
		return this.timeoutSecondHop;
	}

	/**
	 * Sets the timeout (for creating the sense graphs). Note, that for the
	 * first hop the maximum allowed time will be one quarter of the overall
	 * timeout
	 *
	 * @param timeout
	 *            the timeout to set
	 */
	public void setTimeout(int timeout) {
		this.timeoutFirstHop = timeout / 4;
		this.timeoutSecondHop = timeout;
	}

	/**
	 * Sets the number of topics allowed. If a negative number is set, the
	 * algorithm will decide a proper ammount of topics
	 *
	 * @param n
	 *            number of topics
	 */
	public void setNumTopics(int n) {
		this.numTopics = n;
	}

	/**
	 * Sets the maximum number of topics allowed.
	 *
	 * @param maxTopics
	 *            max number of topics
	 */
	public void setMaxNumTopics(int maxTopics) {
		if (maxTopics < 1) {
			throw new IllegalArgumentException("Tried to set an invalid maximum amount of topics. Must be >0");
		}
		this.maxTopics = maxTopics;
	}

	public void setTopicSelectionMethod(TopicSelectionMethod tsm) {
		this.topicSelectionMethod = tsm;
	}

	public TopicSelectionMethod getTopicSelectionMethod() {
		return this.topicSelectionMethod;
	}

	/**
	 * Extracts topics out of the provided {@link IGraph}. Throws an
	 * {@link IllegalArgumentException} if no topics are annotated
	 *
	 * @param inputGraph
	 *            Graph the topics shall be extracted from
	 * @return List of {@link Topic}s that were annotated to the graph
	 */
	public static List<Topic> getTopicsFromIGraph(IGraph inputGraph) {
		if (!inputGraph.hasNodeType(TopicExtractionCore.TOPICS_NODE_TYPE)) {
			throw new IllegalArgumentException(ERR_NO_TOPICS_IN_GRAPH);
		}
		final List<INode> nodesList = inputGraph.getNodesOfType(inputGraph.getNodeType(TopicExtractionCore.TOPICS_NODE_TYPE));
		if ((nodesList == null) || nodesList.isEmpty()) {
			throw new IllegalArgumentException(ERR_NO_TOPICS_IN_GRAPH);
		}
		final INode node = nodesList.get(0);
		if (node == null) {
			throw new IllegalArgumentException(ERR_NO_TOPICS_IN_GRAPH);
		}
		final Object o = node.getAttributeValue(TopicExtractionCore.TOPIC_ATTRIBUTE);
		if (o == null) {
			throw new IllegalArgumentException(ERR_NO_TOPICS_IN_GRAPH);
		}

		final List<Topic> retrievedTopics = new ArrayList<>();
		if (o instanceof List) {
			final List<?> list = (List<?>) o;
			for (final Object obj : list) {
				if (obj instanceof Topic) {
					final Topic t = (Topic) obj;
					retrievedTopics.add(t);
				}
			}
		}
		return retrievedTopics;
	}

	public static void addTopicsToInputGraph(List<Topic> topics, IGraph graph) {
		final INode node = graph.createNode(graph.getNodeType(TopicExtractionCore.TOPICS_NODE_TYPE));
		node.setAttributeValue(TopicExtractionCore.TOPIC_ATTRIBUTE, topics);
	}

	/**
	 * Creates a list of {@link Topic}s for the input senses. The methods
	 * determines the amount of topics itself.
	 *
	 * @param wordSenses
	 *            Collection of input senses
	 * @return List of {@link Topic}s
	 */
	public List<Topic> getTopicsForSenses(Collection<String> wordSenses) {
		return this.getTopicsForSenses(wordSenses, this.numTopics);
	}

	/**
	 * Creates a list of {@link Topic}s for the input senses.
	 *
	 * @param wordSenses
	 *            Collection of input word senses
	 * @param amountOfTopics
	 *            Amount of topics you want. If <= 0, the method selects the
	 *            amount itself.
	 * @return List of {@link Topic}s
	 */
	public synchronized List<Topic> getTopicsForSenses(Collection<String> wordSenses, int amountOfTopics) {
		final TopicGraph topicGraph = this.getTopicGraphForSenses(wordSenses);
		return this.getTopicsForTopicGraph(topicGraph, amountOfTopics);
	}

	/**
	 * Creates a {@link TopicGraph} for the input senses.
	 *
	 * @param wordSenses
	 *            Collection of input word senses
	 * @return a {@link TopicGraph} for the word senses
	 */
	public synchronized TopicGraph getTopicGraphForSenses(Collection<String> wordSenses) {
		final List<TopicGraph> senseGraphs = new ArrayList<>();
		// create Topic Graph
		if (logger.isDebugEnabled()) {
			final String senses = String.join(", ", wordSenses);
			logger.debug("Start creating Graphs for {}", senses);
		}

		for (final String sense : wordSenses) {
			final TopicGraph senseGraph = this.createSenseGraphFor(sense);
			final boolean senseGraphExistsAlready = this.checkIfSenseGraphExistsAlready(senseGraphs, senseGraph);
			if (!senseGraphExistsAlready) {
				senseGraphs.add(senseGraph);
			}
		}
		final TopicGraph topicGraph = TopicGraph.createTopicGraph(senseGraphs);
		if (logger.isDebugEnabled()) {
			logger.debug("Graph has {} nodes and {} vertices", topicGraph.getVerticesSize(), topicGraph.getEdgesSize());
		}
		return topicGraph;
	}

	private boolean checkIfSenseGraphExistsAlready(final List<TopicGraph> senseGraphs, final TopicGraph senseGraph) {
		for (final TopicGraph existingSenseGraph : senseGraphs) {
			if (existingSenseGraph.equals(senseGraph)) {
				existingSenseGraph.increaseWeights();
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates a list of {@link Topic}s for the provided {@link TopicGraph}.
	 *
	 * @param topicGraph
	 *            input topic Graph
	 * @param amountOfTopics
	 *            Amount of topics you want. If <= 0, the methods selects the
	 *            amount itself.
	 * @return List of {@link Topic}s
	 */
	public synchronized List<Topic> getTopicsForTopicGraph(TopicGraph topicGraph, int amountOfTopics) {
		final List<VertexScoreTuple> verticesWithScores = this.getVerticesSortedByCentralityScore(topicGraph);

		if (amountOfTopics <= 0) {
			amountOfTopics = 2 * topicGraph.getSenses().size();
		}
		amountOfTopics = Math.min(amountOfTopics, this.maxTopics);

		// get the correct connectivity processor
		final VertexScoreProcessor vsProcessor = this.topicSelectionMethod.getProcessor(topicGraph, amountOfTopics);

		// process scores with processor and sort selected vst in descending order
		logger.debug("Start further processing of scores");
		final List<VertexScoreTuple> vstList = new ArrayList<>(vsProcessor.processCentralityScores(verticesWithScores));

		// create final topic list
		final List<Topic> topicList = new ArrayList<>();
		for (final VertexScoreTuple vst : vstList) {
			final Topic topic = new Topic(vst.vertex.getLabel(), vst.score);
			final List<WikiVertex> relSenses = topicGraph.getInitialVerticesFor(vst.vertex);
			topic.addRelatedSenses(relSenses);
			topicList.add(topic);
		}

		final Comparator<Topic> byConnectivity = (t1, t2) -> Integer.compare(t2.getRelatedSenses().size(), t1.getRelatedSenses().size());
		final Comparator<Topic> byScore = (t1, t2) -> Double.compare(t2.getScore(), t1.getScore());

		Collections.sort(topicList, byConnectivity.thenComparing(byScore));

		// debug logging
		if (logger.isDebugEnabled()) {
			logger.debug("Processed Topics: ");
			for (final Topic topic : topicList) {
				logger.debug("{}   {}", topic.getScore(), topic.getLabel());
			}
		}

		return topicList;
	}

	private TopicGraph createSenseGraphFor(String word) {
		logger.debug("Creating SenseGraph for {}", word);
		final TopicGraph retGraph = new TopicGraph();
		final Optional<String> optWordDBResource = this.resourceConnector.getResourceStringFor(word);
		if (!optWordDBResource.isPresent()) {
			return retGraph;
		}
		// get the main meaning
		final String wordDBResource = optWordDBResource.get();
		final String wordFromResource = this.createLabelFromResource(wordDBResource);
		if (this.graphCache.containsKey(wordFromResource)) {
			return this.graphCache.get(wordFromResource);
		}

		final WikiVertex mainVertex = new WikiVertex(wordFromResource, wordDBResource);
		retGraph.addVertex(mainVertex);
		retGraph.addSense(mainVertex);

		// start a timer to be able to trigger a timeout
		final Instant start = Instant.now();

		// get hops, add wikiPageRedirects to first hop and add real first hop
		final Set<String> firstHopSet = this.resourceConnector.getEquivalentResources(wordDBResource);
		firstHopSet.addAll(this.resourceConnector.getRelatedFor(wordDBResource));
		// first add all first hops; save a map with url to vertex
		final Map<String, WikiVertex> urlToVertex = new HashMap<>();
		for (final String url : firstHopSet) {
			final String name = this.createLabelFromResource(url);
			final WikiVertex vertex = new WikiVertex(name, url);
			retGraph.addVertex(vertex);
			retGraph.addEdge(mainVertex, vertex);
			urlToVertex.put(url, vertex);
		}

		// check for timeout now
		if (Duration.between(start, Instant.now()).getSeconds() > this.timeoutFirstHop) {
			return this.handleTimeout(wordFromResource, retGraph, word);
		}

		// then add all 2nd hops
		for (final Entry<String, WikiVertex> entry : urlToVertex.entrySet()) {
			final String url = entry.getKey();

			final Set<String> secondHopSet = this.resourceConnector.getRelatedFor(url);
			for (final String url2 : secondHopSet) {
				final String name2 = this.createLabelFromResource(url2);
				final WikiVertex vertex2 = new WikiVertex(name2, url2);
				retGraph.addVertex(vertex2);
				retGraph.addEdge(entry.getValue(), vertex2);
				if (Duration.between(start, Instant.now()).getSeconds() > this.timeoutSecondHop) {
					return this.handleTimeout(wordFromResource, retGraph, word);
				}
			}
		}

		// all went fine, save the graph in the cache and return
		this.graphCache.put(wordFromResource, retGraph);
		if (logger.isDebugEnabled()) {
			logger.debug("SenseGraph for {} has {} nodes and {} vertices", word, retGraph.getVerticesSize(), retGraph.getEdgesSize());
		}
		return retGraph;
	}

	private TopicGraph handleTimeout(String wordFromResource, TopicGraph tg, String word) {
		logger.warn("Stopped creation of the SenseGraph for '{}' early due to a timeout", word);
		this.graphCache.put(wordFromResource, tg);
		if (logger.isDebugEnabled()) {
			logger.debug("SenseGraph for {} has {} nodes and {} vertices", word, tg.getVerticesSize(), tg.getEdgesSize());
		}
		return tg;
	}

	private String createLabelFromResource(String url) {
		return this.resourceConnector.getLabelForResourceSimple(url);
	}

	private Map<WikiVertex, Double> getSortedCentralityScores(TopicGraph tGraph) {
		return tGraph.getCentralityScores().entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	private List<VertexScoreTuple> getVerticesSortedByCentralityScore(TopicGraph tGraph) {
		final List<VertexScoreTuple> retList = new ArrayList<>();
		final Map<WikiVertex, Double> scores = this.getSortedCentralityScores(tGraph);
		final Iterator<Entry<WikiVertex, Double>> entryIterator = scores.entrySet().iterator();
		while (entryIterator.hasNext()) {
			final Entry<WikiVertex, Double> entry = entryIterator.next();
			retList.add(new VertexScoreTuple(entry.getKey(), entry.getValue()));
		}
		return retList;
	}

}