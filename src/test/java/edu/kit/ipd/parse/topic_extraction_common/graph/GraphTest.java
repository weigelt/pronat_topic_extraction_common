package edu.kit.ipd.parse.topic_extraction_common.graph;

import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.topic_extraction_common.graph.TopicGraph;
import edu.kit.ipd.parse.topic_extraction_common.graph.WikiVertex;

/**
 * @author Jan Keim
 *
 */
@Ignore
public class GraphTest {
	private static final Logger	logger	= LoggerFactory.getLogger(GraphTest.class);
	TopicGraph					topicGraph;
	WikiVertex					a		= new WikiVertex("A", "a");
	WikiVertex					b		= new WikiVertex("B", "b");
	WikiVertex					c		= new WikiVertex("C", "c");

	@Before
	public void before() {
		topicGraph = new TopicGraph();

		// Vertices
		topicGraph.addVertex(a);
		topicGraph.addVertex(b);
		topicGraph.addVertex(c);

		// Edges
		topicGraph.addEdge(a, b);
		topicGraph.addEdge(b, c);
	}

	@Test
	public void testGraph() {
		logger.info(topicGraph.toString());
		Assert.assertEquals(3, topicGraph.getVerticesSize());
		Assert.assertEquals(2, topicGraph.getEdgesSize());
	}

	@Test
	public void testVertices() {
		Assert.assertEquals(1, topicGraph.degreeOf(a));
		Assert.assertEquals(2, topicGraph.degreeOf(b));
		Assert.assertEquals(1, topicGraph.degreeOf(c));
	}

	@Test
	public void testPaths() {
		Assert.assertEquals(2, topicGraph.shortestPathLength(a, c));
		Assert.assertEquals(1, topicGraph.shortestPathLength(a, b));
		Assert.assertEquals(1, topicGraph.shortestPathLength(c, b));

		Assert.assertEquals(topicGraph.shortestPathLength(a, c), topicGraph.shortestPathLength(c, a));
		Assert.assertEquals(topicGraph.shortestPathLength(a, b), topicGraph.shortestPathLength(b, a));
		Assert.assertEquals(topicGraph.shortestPathLength(b, c), topicGraph.shortestPathLength(c, b));
	}

	@Test
	public void testPageRank() {
		Map<WikiVertex, Double> scores = topicGraph.getPageRankScores();
		logger.info("PageRank for A: " + scores.get(a));
		logger.info("PageRank for B: " + scores.get(b));
		logger.info("PageRank for C: " + scores.get(c));
		Assert.assertTrue(scores.get(b) > scores.get(a));
		Assert.assertEquals(scores.get(a), scores.get(c), 0.0001);
	}

	@Test
	public void testBigGraph() {
		topicGraph = new TopicGraph();
		for (int i = 0; i < 1000; i++) {
			WikiVertex v = new WikiVertex("" + i, "" + i);
			topicGraph.addVertex(v);
		}

		Random r = new Random();
		for (int i = 0; i < 1000; i++) {
			String first = "" + r.nextInt(1000);
			WikiVertex v1 = new WikiVertex(first, first);
			String second = "" + r.nextInt(1000);
			WikiVertex v2 = new WikiVertex(second, second);
			topicGraph.addEdge(v1, v2);
		}

		Assert.assertEquals(1000, topicGraph.getVerticesSize());
		Assert.assertEquals(1000, topicGraph.getEdgesSize());
		/* Map<WikiVertex, Double> sortedScores = */topicGraph.getPageRankScores();
		/*
		 * .entrySet().stream() .sorted(Map.Entry.comparingByValue( Collections.reverseOrder()))
		 * .collect(Collectors.toMap(Map.Entry:: getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		 */
	}

	@Test
	public void testNotConnectedGraph() {
		topicGraph = new TopicGraph();

		topicGraph.addVertex(a);
		topicGraph.addVertex(b);
		topicGraph.addVertex(c);
		WikiVertex d = new WikiVertex("D", "d");
		topicGraph.addVertex(d);

		topicGraph.addEdge(a, b);
		topicGraph.addEdge(b, c);
		topicGraph.addEdge(a, c);

		logger.info(topicGraph.toString());
		logger.info("Length of Shortest Path from A to D: " + topicGraph.shortestPathLength(a, d));
	}
}
