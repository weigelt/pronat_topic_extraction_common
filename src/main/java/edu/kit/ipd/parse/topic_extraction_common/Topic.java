package edu.kit.ipd.parse.topic_extraction_common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import edu.kit.ipd.parse.topic_extraction_common.graph.WikiVertex;

/**
 * @author Jan Keim
 *
 */
public class Topic implements Serializable {

	private static final long serialVersionUID = -3799554324539563512L;

	private final String label;
	private final double score;

	private final List<String> relatedSenses;

	/**
	 * Create a new Topic with the provided label. The score for this topic will
	 * be set to {@link Double#NaN}!
	 *
	 * @param label
	 *            Label of the topic
	 */
	public Topic(String label) {
		this.label = label;
		this.score = Double.NaN;
		this.relatedSenses = new ArrayList<>();
	}

	/**
	 * Create a new Topic with the provided label and score
	 *
	 * @param label
	 *            Label of the topic
	 * @param score
	 *            Score for the topic
	 */
	public Topic(String label, double score) {
		this.label = label;
		this.score = score;
		this.relatedSenses = new ArrayList<>();
	}

	/**
	 * Add a related sense of this topic as a {@link String}
	 *
	 * @param relSense
	 *            related sense as {@link String}
	 * @return whether the related sense could be added
	 */
	public boolean addRelatedSense(String relSense) {
		return this.relatedSenses.add(relSense);
	}

	/**
	 * Add a related sense of this topic as a {@link WikiVertex}
	 *
	 * @param wv
	 *            {@link WikiVertex} that represents the related sense
	 * @return whether the related sense could be added
	 */
	public boolean addRelatedSense(WikiVertex wv) {
		return this.relatedSenses.add(wv.getLabel());
	}

	/**
	 * Add a list of related senses as {@link WikiVertex} to this topic
	 *
	 * @param wvList
	 *            List of WikiVertices that should be added
	 */
	public void addRelatedSenses(List<WikiVertex> wvList) {
		for (final WikiVertex wv : wvList) {
			this.addRelatedSense(wv);
		}
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * @return the relatedSenses
	 */
	public List<String> getRelatedSenses() {
		return this.relatedSenses;
	}

	/**
	 * @return the score
	 */
	public double getScore() {
		return this.score;
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
		result = (prime * result) + ((this.label == null) ? 0 : this.label.hashCode());
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
		if (!(obj instanceof Topic)) {
			return false;
		}
		final Topic other = (Topic) obj;
		if (!Objects.equals(this.label, other.label)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "\"" + this.label + "\"";
	}

}
