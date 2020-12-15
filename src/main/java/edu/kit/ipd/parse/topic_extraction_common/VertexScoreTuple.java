package edu.kit.ipd.parse.topic_extraction_common;

import java.util.Objects;

import edu.kit.ipd.parse.topic_extraction_common.graph.WikiVertex;

/**
 * @author Jan Keim
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
		return Objects.hash(this.score, this.vertex);
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
		return Objects.equals(this.score, other.score) && Objects.equals(this.vertex, other.vertex);
	}

	@Override
	public int compareTo(VertexScoreTuple o) {
		return Double.compare(this.score, o.score);
	}

}
