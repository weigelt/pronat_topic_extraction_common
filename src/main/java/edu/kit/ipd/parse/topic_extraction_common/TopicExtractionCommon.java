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
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.agent.AbstractAgent;
import edu.kit.ipd.parse.luna.data.MissingDataException;
import edu.kit.ipd.parse.luna.data.PrePipelineData;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.INodeType;
import edu.kit.ipd.parse.luna.tools.ConfigManager;
import edu.kit.ipd.parse.topic_extraction_common.graph.TopicGraph;
import edu.kit.ipd.parse.topic_extraction_common.graph.WikiVertex;
import edu.kit.ipd.parse.topic_extraction_common.ontology.DBPediaConnector;
import edu.kit.ipd.parse.topic_extraction_common.ontology.OfflineDBPediaConnector;
import edu.kit.ipd.parse.topic_extraction_common.ontology.ResourceConnector;

/**
 * @author Jan Keim
 *
 */
@MetaInfServices(AbstractAgent.class)
public class TopicExtractionCommon extends AbstractAgent {
	private static final Logger logger = LoggerFactory.getLogger(TopicExtractionCommon.class);
	private static final String ID = "TopicExtraction";
	private static final String TOKEN_NODE_TYPE = "token";
	private static final String POS_ATTRIBUTE = "pos";
	private static final String NER_ATTRIBUTE = "ner";
	private static final String WSD_ATTRIBUTE = "wsd";
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
	private HashMap<String, TopicGraph> graphCache;

	private ResourceConnector resourceConnector;

	@Override
	public void init() {
		this.setId(TopicExtractionCommon.ID);

		this.graphCache = new HashMap<>();

		final Properties props = ConfigManager.getConfiguration(this.getClass());
		final String offline = props.getProperty("OFFLINE_USE");
		if ((offline == null) || offline.isEmpty() || offline.equals("N")) {
			String url = props.getProperty("URL", DBPediaConnector.DEFAULT_SERVICE_URL);
			if (url.isEmpty()) {
				url = DBPediaConnector.DEFAULT_SERVICE_URL;
			}
			this.resourceConnector = new DBPediaConnector(url);
		} else {
			if (TopicExtractionCommon.logger.isInfoEnabled()) {
				TopicExtractionCommon.logger.info("Using offline version for resource connection.");
			}
			final String owl = props.getProperty("OWL");
			if ((owl == null) || owl.isEmpty()) {
				throw new IllegalArgumentException("Could not load proper owl file from properties configuration.");
			}
			final String turtle1 = props.getProperty("TURTLE1");
			final String turtle2 = props.getProperty("TURTLE2");
			final String turtle3 = props.getProperty("TURTLE3");
			final String turtle4 = props.getProperty("TURTLE4");
			this.resourceConnector = new OfflineDBPediaConnector(owl, turtle1, turtle2, turtle3, turtle4);
		}
		try {
			this.numTopics = Integer.parseInt(props.getProperty("TOPICS", "-1"));
		} catch (final NumberFormatException e) {
			TopicExtractionCommon.logger.warn("Could not parse provided number for amount of topics. Use default instead.");
			this.numTopics = -1;
		}
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

	private void prepareGraph() {
		// add graph attribute
		INodeType tokenType;
		if (this.graph.hasNodeType(TopicExtractionCommon.TOPICS_NODE_TYPE)) {
			tokenType = this.graph.getNodeType(TopicExtractionCommon.TOPICS_NODE_TYPE);
		} else {
			tokenType = this.graph.createNodeType(TopicExtractionCommon.TOPICS_NODE_TYPE);
		}
		if (!tokenType.containsAttribute(TopicExtractionCommon.TOPIC_ATTRIBUTE, "java.util.List")) {
			tokenType.addAttributeToType("java.util.List", TopicExtractionCommon.TOPIC_ATTRIBUTE);
		}
	}

	@Override
	protected synchronized void exec() {
		this.prepareGraph();
		final List<INode> nodes = this.getNodes();
		final Map<INode, String> nodeToWSD = new HashMap<>();
		for (final INode node : nodes) {
			final String pos = node.getAttributeValue(TopicExtractionCommon.POS_ATTRIBUTE).toString();
			if (pos.startsWith("NN")) {
				if (this.nodeIsNamedEntity(node)) {
					continue;
				}
				final String wsd = Objects.toString(node.getAttributeValue(TopicExtractionCommon.WSD_ATTRIBUTE));
				if (!wsd.equals("null")) {
					nodeToWSD.put(node, wsd);
				}
			}
		}
		final List<Topic> topics = this.getTopicsForSenses(nodeToWSD.values());

		this.addTopicsToInputGraph(topics);

		if (TopicExtractionCommon.logger.isDebugEnabled()) {
			final List<Topic> retrievedTopics = TopicExtractionCommon.getTopicsFromIGraph(this.graph);
			TopicExtractionCommon.logger.debug("Retrieved " + retrievedTopics.size() + " topics:");
			for (final Topic t : retrievedTopics) {
				TopicExtractionCommon.logger.debug(t.getLabel() + " (" + String.join(", ", t.getRelatedSenses()) + ")");
			}
		}
	}

	public void exec(PrePipelineData ppd) {
		try {
			this.graph = ppd.getGraph();
		} catch (final MissingDataException e) {
			e.printStackTrace();
		}
		this.exec();
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
		if (!inputGraph.hasNodeType(TopicExtractionCommon.TOPICS_NODE_TYPE)) {
			throw new IllegalArgumentException("Topics are not annotated to the graph.");
		}
		final List<INode> nodesList = inputGraph.getNodesOfType(inputGraph.getNodeType(TopicExtractionCommon.TOPICS_NODE_TYPE));
		if ((nodesList == null) || nodesList.isEmpty()) {
			throw new IllegalArgumentException("Topics are not annotated to the graph.");
		}
		final INode node = nodesList.get(0);
		if (node == null) {
			throw new IllegalArgumentException("Topics are not annotated to the graph.");
		}
		final Object o = node.getAttributeValue(TopicExtractionCommon.TOPIC_ATTRIBUTE);
		if (o == null) {
			throw new IllegalArgumentException("Topics are not annotated to the graph.");
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

	private void addTopicsToInputGraph(List<Topic> topics) {
		final INode node = this.graph.createNode(this.graph.getNodeType(TopicExtractionCommon.TOPICS_NODE_TYPE));
		node.setAttributeValue(TopicExtractionCommon.TOPIC_ATTRIBUTE, topics);
	}

	private List<INode> getNodes() {
		return this.graph.getNodesOfType(this.graph.getNodeType(TopicExtractionCommon.TOKEN_NODE_TYPE));
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
		logger.debug("Start creating Graphs for " + String.join(", ", wordSenses));
		outer: for (final String sense : wordSenses) {
			final TopicGraph senseGraph = this.createSenseGraphFor(sense);
			for (final TopicGraph otherSenseGraph : senseGraphs) {
				if (otherSenseGraph.equals(senseGraph)) {
					otherSenseGraph.increaseWeights();
					continue outer;
				}
			}
			senseGraphs.add(senseGraph);
		}
		final TopicGraph topicGraph = TopicGraph.createTopicGraph(senseGraphs);
		if (logger.isDebugEnabled()) {
			logger.debug("Graph has " + topicGraph.getVerticesSize() + " nodes and " + topicGraph.getEdgesSize() + " vertices");
		}
		return topicGraph;
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
				logger.debug(topic.getScore() + "   " + topic.getLabel());
			}
		}

		return topicList;
	}

	private boolean nodeIsNamedEntity(INode node) {
		// use NER from parse
		final String nodeString = node.getAttributeValue(TopicExtractionCommon.NER_ATTRIBUTE).toString();
		return (!nodeString.equals("O") || nodeString.toLowerCase().equals("armar"));
	}

	private TopicGraph createSenseGraphFor(String word) {
		logger.debug("Creating SenseGraph for " + word);
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
			logger.debug("SenseGraph for " + word + " has " + retGraph.getVerticesSize() + " nodes and " + retGraph.getEdgesSize()
					+ " vertices");
		}
		return retGraph;
	}

	private TopicGraph handleTimeout(String wordFromResource, TopicGraph tg, String word) {
		logger.warn("Stopped creation of the SenseGraph for '" + word + "' early due to a timeout");
		this.graphCache.put(wordFromResource, tg);
		if (logger.isDebugEnabled()) {
			logger.debug("SenseGraph for " + word + " has " + tg.getVerticesSize() + " nodes and " + tg.getEdgesSize() + " vertices");
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
