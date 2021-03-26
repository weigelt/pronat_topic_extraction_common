package edu.kit.ipd.pronat.topic_extraction_common.graph;

import java.io.Serializable;

/**
 * @author Jan Keim
 * @author Sebastian Weigelt
 *
 */
public class WikiVertex implements Serializable {

	private static final long serialVersionUID = -6530169345636353515L;

	private String label;
	private String resource;

	public WikiVertex(String label, String resource) {
		this.label = label;
		this.resource = resource;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the resource
	 */
	public String getResource() {
		return resource;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return label.hashCode();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WikiVertex [label=");
		builder.append(label);
		builder.append(", resource=");
		builder.append(resource);
		builder.append("]");
		return builder.toString();
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
		if (!(obj instanceof WikiVertex)) {
			return false;
		}
		WikiVertex other = (WikiVertex) obj;
		if (label == null) {
			if (other.label != null) {
				return false;
			}
		} else if (!label.equals(other.label)) {
			return false;
		}
		return true;
	}

	public boolean equals(String obj) {
		if (obj == null) {
			return false;
		}
		return obj.equals(label);
	}

}
