package edu.kit.ipd.pronat.topic_extraction_common.ontology;

import java.util.Optional;
import java.util.Set;

/**
 * @author Jan Keim
 * @author Sebastian Weigelt
 *
 */
public interface ResourceConnector {

	Optional<String> getResourceStringFor(String label);

	Set<String> getRelatedFor(String resource);

	Optional<String> getLabelForResource(String resource);

	String getLabelForResourceSimple(String dbResource);

	Set<String> getEquivalentResources(String resource);
}
