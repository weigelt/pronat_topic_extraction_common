package edu.kit.ipd.parse.topic_extraction_common.ontology;

import java.util.Optional;
import java.util.Set;

/**
 * @author Jan Keim
 *
 */
public interface ResourceConnector {

	public Optional<String> getResourceStringFor(String label);

	public Set<String> getRelatedFor(String resource);

	public Optional<String> getLabelForResource(String resource);

	String getLabelForResourceSimple(String dbResource);

	Set<String> getEquivalentResources(String resource);
}
