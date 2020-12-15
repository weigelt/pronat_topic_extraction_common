/**
 *
 */
package edu.kit.ipd.parse.topic_extraction_common;

import edu.kit.ipd.parse.topic_extraction_common.graph.TopicGraph;

/**
 * @author Jan Keim
 *
 */
public enum TopicSelectionMethod {
	MaxConnectivity, CombinedConnectivity;

	protected VertexScoreProcessor getProcessor(TopicGraph topicGraph, int num_topics) {
		switch (this) {
		case CombinedConnectivity:
			return new CombinedConnectivityProcessor(topicGraph, num_topics);
		case MaxConnectivity:
			return new MaxConnectivityProcessor(topicGraph, num_topics);
		default:
			return new CombinedConnectivityProcessor(topicGraph, num_topics);
		}
	}

	protected VertexScoreProcessor getProcessor(TopicGraph topicGraph) {
		switch (this) {
		case CombinedConnectivity:
			return new CombinedConnectivityProcessor(topicGraph);
		case MaxConnectivity:
			return new MaxConnectivityProcessor(topicGraph);
		default:
			return new CombinedConnectivityProcessor(topicGraph);
		}
	}
}
