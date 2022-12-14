package edu.kit.ipd.pronat.topic_extraction_common;

import java.util.Objects;

import edu.kit.ipd.pronat.topic_extraction_common.graph.WikiVertex;

/**
 * @author Jan Keim
 * @author Sebastian Weigelt
 *
 */
class VertexScoreTuple implements Comparable<VertexScoreTuple> {
	WikiVertex vertex;
	Double score;

	public VertexScoreTuple(WikiVertex vertex, Double score) {
		this.vertex = vertex;
		this.score = score;
	}

	@Override
	public int hashCode() {
		return Objects.hash(score, vertex);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof VertexScoreTuple)) {
			return false;
		}
		final VertexScoreTuple other = (VertexScoreTuple) obj;
		return Objects.equals(score, other.score) && Objects.equals(vertex, other.vertex);
	}

	@Override
	public int compareTo(VertexScoreTuple o) {
		return Double.compare(score, o.score);
	}

}
