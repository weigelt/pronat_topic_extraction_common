package edu.kit.ipd.parse.topic_extraction_common.ontology;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cached resource connector decorator.
 *
 * @author Dominik Fuchss
 *
 */
public class CachedResourceConnector implements ResourceConnector {
	private static final Logger logger = LoggerFactory.getLogger(CachedResourceConnector.class);

	private final ResourceConnector connector;

	private final CacheData data;

	public CachedResourceConnector(ResourceConnector connector) {
		this.connector = connector;
		this.data = CacheData.loadData();
	}

	@Override
	public Optional<String> getResourceStringFor(String label) {
		if (this.data.getResources().containsKey(label)) {
			return Optional.ofNullable(this.data.getResources().get(label));
		}

		try {
			final Optional<String> result = this.connector.getResourceStringFor(label);
			this.data.getResources().put(label, result.orElse(null));
			this.data.store();
			return result;
		} catch (final Exception e) {
			logger.error(e.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public Set<String> getRelatedFor(String resource) {
		if (this.data.getRelated().containsKey(resource)) {
			return this.data.getRelated().get(resource);
		}

		try {
			final TreeSet<String> result = new TreeSet<>(this.connector.getRelatedFor(resource));
			this.data.getRelated().put(resource, result);
			this.data.store();
			return result;
		} catch (final Exception e) {
			logger.error(e.getMessage());
			return new HashSet<>();
		}
	}

	@Override
	public Optional<String> getLabelForResource(String resource) {
		if (this.data.getLabels().containsKey(resource)) {
			return Optional.ofNullable(this.data.getLabels().get(resource));
		}

		try {
			final Optional<String> result = this.connector.getLabelForResource(resource);
			this.data.getLabels().put(resource, result.orElse(null));
			this.data.store();
			return result;
		} catch (final Exception e) {
			logger.error(e.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public String getLabelForResourceSimple(String dbResource) {
		if (this.data.getResourceSimples().containsKey(dbResource)) {
			return this.data.getResourceSimples().get(dbResource);
		}

		try {
			final String result = this.connector.getLabelForResourceSimple(dbResource);
			this.data.getResourceSimples().put(dbResource, result);
			this.data.store();
			return result;
		} catch (final Exception e) {
			logger.error(e.getMessage());
			return null;
		}
	}

	@Override
	public Set<String> getEquivalentResources(String resource) {
		if (this.data.getEquivalentResources().containsKey(resource)) {
			return this.data.getEquivalentResources().get(resource);
		}

		try {
			final TreeSet<String> result = new TreeSet<>(this.connector.getEquivalentResources(resource));
			this.data.getEquivalentResources().put(resource, result);
			this.data.store();
			return result;
		} catch (final Exception e) {
			logger.error(e.getMessage());
			return new HashSet<>();
		}
	}
}