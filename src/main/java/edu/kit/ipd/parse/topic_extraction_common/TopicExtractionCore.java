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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.INodeType;
import edu.kit.ipd.parse.luna.graph.Pair;
import edu.kit.ipd.parse.topic_extraction_common.graph.TopicGraph;
import edu.kit.ipd.parse.topic_extraction_common.graph.WikiVertex;
import edu.kit.ipd.parse.topic_extraction_common.ontology.CachedResourceConnector;
import edu.kit.ipd.parse.topic_extraction_common.ontology.DBPediaConnector;
import edu.kit.ipd.parse.topic_extraction_common.ontology.ResourceConnector;

/**
 * @author Jan Keim
 *
 */
public class TopicExtractionCore {
	private static final String ERR_NO_TOPICS_IN_GRAPH = "Topics are not annotated to the graph.";
	private static final Logger logger = LoggerFactory.getLogger(TopicExtractionCore.class);
	private static final String TOPIC_ATTRIBUTE = "topic";
	private static final String TOPICS_NODE_TYPE = "topics";

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

	private final ResourceConnector resourceConnector;

	public TopicExtractionCore() {
		this(DBPediaConnector.DEFAULT_SERVICE_URL);
	}

	public TopicExtractionCore(String url) {
		this(new CachedResourceConnector(new DBPediaConnector(url)));
	}

	public TopicExtractionCore(ResourceConnector ressourceConnector) {
		graphCache = new HashMap<>();
		numTopics = -1;
		resourceConnector = ressourceConnector;
	}

	/**
	 * Gets the timeout (for creating the sense graphs). Note, that for the first
	 * hop the maximum allowed time will be one quarter of the overall timeout
	 *
	 * @return the timeout.
	 */
	public int getTimeout() {
		return timeoutSecondHop;
	}

	/**
	 * Sets the timeout (for creating the sense graphs). Note, that for the first
	 * hop the maximum allowed time will be one quarter of the overall timeout
	 *
	 * @param timeout
	 *            the timeout to set
	 */
	public void setTimeout(int timeout) {
		timeoutFirstHop = timeout / 4;
		timeoutSecondHop = timeout;
	}

	/**
	 * Sets the number of topics allowed. If a negative number is set, the algorithm
	 * will decide a proper ammount of topics
	 *
	 * @param n
	 *            number of topics
	 */
	public void setNumTopics(int n) {
		numTopics = n;
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
		topicSelectionMethod = tsm;
	}

	public TopicSelectionMethod getTopicSelectionMethod() {
		return topicSelectionMethod;
	}

	private static void prepareGraph(IGraph graph) {
		// add graph attribute
		INodeType tokenType;
		if (graph.hasNodeType(TOPICS_NODE_TYPE)) {
			tokenType = graph.getNodeType(TOPICS_NODE_TYPE);
		} else {
			tokenType = graph.createNodeType(TOPICS_NODE_TYPE);
		}
		if (!tokenType.containsAttribute(TOPIC_ATTRIBUTE, "java.util.List")) {
			tokenType.addAttributeToType("java.util.List", TOPIC_ATTRIBUTE);
		}
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
		final Object attributeObject = node.getAttributeValue(TopicExtractionCore.TOPIC_ATTRIBUTE);
		if (attributeObject == null) {
			throw new IllegalArgumentException(ERR_NO_TOPICS_IN_GRAPH);
		}

		return unserializeTopicList(attributeObject);
	}

	private static List<Topic> unserializeTopicList(final Object attributeObject) {
		final List<Topic> retrievedTopics = new ArrayList<>();
		if (attributeObject instanceof List) {
			final List<?> list = (List<?>) attributeObject;
			for (final Object serializedTopic : list) {
				final Topic topic = unserializeTopicFromGraph(serializedTopic);
				if (topic != null) {
					retrievedTopics.add(topic);
				}
			}
		}
		return retrievedTopics;
	}

	/**
	 * TODO
	 *
	 * @param topics
	 * @param graph
	 */
	public static void setTopicsToInputGraph(List<Topic> topics, IGraph graph) {
		prepareGraph(graph);
		final INodeType nodeType = graph.getNodeType(TopicExtractionCore.TOPICS_NODE_TYPE);
		for (final INode node : graph.getNodesOfType(nodeType)) {
			graph.deleteNode(node);
		}
		final INode node = graph.createNode(nodeType);
		final List<Pair<String, Double>> serializedTopics = transformTopicListForSerialization(topics);
		node.setAttributeValue(TopicExtractionCore.TOPIC_ATTRIBUTE, serializedTopics);
	}

	private static List<Pair<String, Double>> transformTopicListForSerialization(List<Topic> topics) {
		return topics.stream().map(TopicExtractionCore::serializeTopicForGraph).collect(Collectors.toUnmodifiableList());
	}

	private static Pair<String, Double> serializeTopicForGraph(Topic topic) {
		return new Pair<>(topic.getLabel(), topic.getScore());
	}

	private static Topic unserializeTopicFromGraph(Object serializedTopic) {
		if (Objects.isNull(serializedTopic) || !(serializedTopic instanceof Pair)) {
			return null;
		}
		String label = null;
		double score = Double.NaN;
		final Pair<?, ?> topicPair = (Pair<?, ?>) serializedTopic;
		final Object left = topicPair.getLeft();
		final Object right = topicPair.getRight();
		if (left instanceof String) {
			label = (String) left;
		}
		if (right instanceof Double) {
			score = (Double) right;
		}
		if (label == null || Double.isNaN(score)) {
			return null;
		}
		return new Topic(label, score);
	}

	/**
	 * Creates a list of {@link Topic}s for the input senses. The methods determines
	 * the amount of topics itself.
	 *
	 * @param wordSenses
	 *            Collection of input senses
	 * @return List of {@link Topic}s
	 */
	public List<Topic> getTopicsForSenses(Collection<String> wordSenses) {
		return getTopicsForSenses(wordSenses, numTopics);
	}

	/**
	 * Creates a list of {@link Topic}s for the input senses.
	 *
	 * @param wordSenses
	 *            Collection of input word senses
	 * @param amountOfTopics
	 *            Amount of topics you want. If <= 0, the method selects the amount
	 *            itself.
	 * @return List of {@link Topic}s
	 */
	public synchronized List<Topic> getTopicsForSenses(Collection<String> wordSenses, int amountOfTopics) {
		final TopicGraph topicGraph = getTopicGraphForSenses(wordSenses);
		return getTopicsForTopicGraph(topicGraph, amountOfTopics);
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
			final TopicGraph senseGraph = createSenseGraphFor(sense);
			final boolean senseGraphExistsAlready = checkIfSenseGraphExistsAlready(senseGraphs, senseGraph);
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
	 *            Amount of topics you want. If <= 0, the methods selects the amount
	 *            itself.
	 * @return List of {@link Topic}s
	 */
	public synchronized List<Topic> getTopicsForTopicGraph(TopicGraph topicGraph, int amountOfTopics) {
		final List<VertexScoreTuple> verticesWithScores = getVerticesSortedByCentralityScore(topicGraph);

		if (amountOfTopics <= 0) {
			amountOfTopics = 2 * topicGraph.getSenses().size();
		}
		amountOfTopics = Math.min(amountOfTopics, maxTopics);

		// get the correct connectivity processor
		final VertexScoreProcessor vsProcessor = topicSelectionMethod
				.getProcessor(topicGraph, amountOfTopics);

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
		final Optional<String> optWordDBResource = resourceConnector.getResourceStringFor(word);
		if (!optWordDBResource.isPresent()) {
			return retGraph;
		}
		// get the main meaning
		final String wordDBResource = optWordDBResource.get();
		final String wordFromResource = createLabelFromResource(wordDBResource);
		if (graphCache.containsKey(wordFromResource)) {
			return graphCache.get(wordFromResource);
		}

		final WikiVertex mainVertex = new WikiVertex(wordFromResource, wordDBResource);
		retGraph.addVertex(mainVertex);
		retGraph.addSense(mainVertex);

		// start a timer to be able to trigger a timeout
		final Instant start = Instant.now();

		// get hops, add wikiPageRedirects to first hop and add real first hop
		final Set<String> firstHopSet = resourceConnector.getEquivalentResources(wordDBResource);
		firstHopSet.addAll(resourceConnector.getRelatedFor(wordDBResource));
		// first add all first hops; save a map with url to vertex
		final Map<String, WikiVertex> urlToVertex = new HashMap<>();
		for (final String url : firstHopSet) {
			final String name = createLabelFromResource(url);
			final WikiVertex vertex = new WikiVertex(name, url);
			retGraph.addVertex(vertex);
			retGraph.addEdge(mainVertex, vertex);
			urlToVertex.put(url, vertex);
		}

		// check for timeout now
		if (Duration.between(start, Instant.now()).getSeconds() > timeoutFirstHop) {
			return handleTimeout(wordFromResource, retGraph, word);
		}

		// then add all 2nd hops
		for (final Entry<String, WikiVertex> entry : urlToVertex.entrySet()) {
			final String url = entry.getKey();

			final Set<String> secondHopSet = resourceConnector.getRelatedFor(url);
			for (final String url2 : secondHopSet) {
				final String name2 = createLabelFromResource(url2);
				final WikiVertex vertex2 = new WikiVertex(name2, url2);
				retGraph.addVertex(vertex2);
				retGraph.addEdge(entry.getValue(), vertex2);
				if (Duration.between(start, Instant.now()).getSeconds() > timeoutSecondHop) {
					return handleTimeout(wordFromResource, retGraph, word);
				}
			}
		}

		// all went fine, save the graph in the cache and return
		graphCache.put(wordFromResource, retGraph);
		if (logger.isDebugEnabled()) {
			logger.debug("SenseGraph for {} has {} nodes and {} vertices", word, retGraph.getVerticesSize(), retGraph.getEdgesSize());
		}
		return retGraph;
	}

	private TopicGraph handleTimeout(String wordFromResource, TopicGraph tg, String word) {
		logger.warn("Stopped creation of the SenseGraph for '{}' early due to a timeout", word);
		graphCache.put(wordFromResource, tg);
		if (logger.isDebugEnabled()) {
			logger.debug("SenseGraph for {} has {} nodes and {} vertices", word, tg.getVerticesSize(), tg.getEdgesSize());
		}
		return tg;
	}

	private String createLabelFromResource(String url) {
		return resourceConnector.getLabelForResourceSimple(url);
	}

	private Map<WikiVertex, Double> getSortedCentralityScores(TopicGraph tGraph) {
		return tGraph.getCentralityScores().entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	private List<VertexScoreTuple> getVerticesSortedByCentralityScore(TopicGraph tGraph) {
		final List<VertexScoreTuple> retList = new ArrayList<>();
		final Map<WikiVertex, Double> scores = getSortedCentralityScores(tGraph);
		final Iterator<Entry<WikiVertex, Double>> entryIterator = scores.entrySet().iterator();
		while (entryIterator.hasNext()) {
			final Entry<WikiVertex, Double> entry = entryIterator.next();
			retList.add(new VertexScoreTuple(entry.getKey(), entry.getValue()));
		}
		return retList;
	}

}
