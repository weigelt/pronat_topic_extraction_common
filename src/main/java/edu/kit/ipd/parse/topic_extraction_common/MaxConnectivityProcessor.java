package edu.kit.ipd.parse.topic_extraction_common;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.topic_extraction_common.graph.TopicGraph;
import edu.kit.ipd.parse.topic_extraction_common.graph.WikiVertex;

/**
 * @author Jan Keim
 *
 */
public class MaxConnectivityProcessor implements VertexScoreProcessor {
	private static final Logger logger = LoggerFactory.getLogger(MaxConnectivityProcessor.class);

	private final TopicGraph topicGraph;
	private final int topics;

	public MaxConnectivityProcessor(TopicGraph topicGraph) {
		this(topicGraph, 8);
	}

	public MaxConnectivityProcessor(TopicGraph topicGraph, int topics) {
		this.topicGraph = topicGraph;
		this.topics = topics;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * im.janke.topicExtraction.VertexScoreProcessor#processCentralityScores(
	 * java.util.List, im.janke.topicExtraction.graph.TopicGraph)
	 */
	@Override
	public Set<VertexScoreTuple> processCentralityScores(List<VertexScoreTuple> centralityScoresTuples) {
		Objects.requireNonNull(this.topicGraph);
		Objects.requireNonNull(centralityScoresTuples);
		centralityScoresTuples.sort(Collections.reverseOrder());

		int remainingTopicsToSet = this.topics;

		// get vertices, that were in most sense graphs
		final int maxConnectivity = this.topicGraph.getMaxSenseConnectivity();
		logger.debug("Max Sense Connectivity = " + maxConnectivity);

		final Set<VertexScoreTuple> retSet = new HashSet<>();

		// assign vertices with max connectivity to their corresponding
		// sense-vertices
		int currentConnectivity = maxConnectivity;
		while ((remainingTopicsToSet > 0) && (currentConnectivity > 0)) {
			for (final VertexScoreTuple v : centralityScoresTuples) {
				final List<WikiVertex> vertexConnList = this.topicGraph.getInitialVerticesFor(v.vertex);
				if (vertexConnList.size() == currentConnectivity) {
					retSet.add(v);
					remainingTopicsToSet--;
				}
				if (remainingTopicsToSet <= 0) {
					break;
				}
			}
			currentConnectivity--;
		}

		return retSet;
	}
}
