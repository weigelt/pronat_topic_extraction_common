package edu.kit.ipd.pronat.topic_extraction_common.ontology;

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
 * @author Sebastian Weigelt
 *
 */
public class CachedResourceConnector implements ResourceConnector {
	private static final Logger logger = LoggerFactory.getLogger(CachedResourceConnector.class);

	private static final int RETRIES = 3;

	private final ResourceConnector connector;

	private final CacheData data;

	public CachedResourceConnector(ResourceConnector connector) {
		this.connector = connector;
		data = CacheData.loadData();
	}

	@Override
	public Optional<String> getResourceStringFor(String label) {
		return getResourceStringFor(label, 0);
	}

	private Optional<String> getResourceStringFor(String label, int tryNo) {
		if (tryNo >= CachedResourceConnector.RETRIES) {
			CachedResourceConnector.logger.error("Finally give up .. getResourceStringFor");
			return Optional.empty();
		}

		if (data.getResources().containsKey(label)) {
			return Optional.ofNullable(data.getResources().get(label));
		}

		try {
			final Optional<String> result = connector.getResourceStringFor(label);
			data.getResources().put(label, result.orElse(null));
			data.store();
			return result;
		} catch (final Exception e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
			// Wait because of possible timeout of online resource
			sleep();
			return getResourceStringFor(label, tryNo + 1);
		}

	}

	@Override
	public Set<String> getRelatedFor(String resource) {
		return getRelatedFor(resource, 0);
	}

	private Set<String> getRelatedFor(String resource, int tryNo) {
		if (tryNo >= CachedResourceConnector.RETRIES) {
			CachedResourceConnector.logger.error("Finally give up .. getRelatedFor");
			return new TreeSet<>();
		}

		if (data.getRelated().containsKey(resource)) {
			return data.getRelated().get(resource);
		}

		try {
			final TreeSet<String> result = new TreeSet<>(connector.getRelatedFor(resource));
			data.getRelated().put(resource, result);
			data.store();
			return result;
		} catch (final Exception e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
			// Wait because of possible timeout of online resource
			sleep();
			return getRelatedFor(resource, tryNo + 1);
		}
	}

	@Override
	public Optional<String> getLabelForResource(String resource) {
		return getLabelForResource(resource, 0);
	}

	private Optional<String> getLabelForResource(String resource, int tryNo) {
		if (tryNo >= CachedResourceConnector.RETRIES) {
			CachedResourceConnector.logger.error("Finally give up .. getLabelForResource");
			return Optional.empty();
		}

		if (data.getLabels().containsKey(resource)) {
			return Optional.ofNullable(data.getLabels().get(resource));
		}

		try {
			final Optional<String> result = connector.getLabelForResource(resource);
			data.getLabels().put(resource, result.orElse(null));
			data.store();
			return result;
		} catch (final Exception e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
			sleep();
			return getLabelForResource(resource, tryNo + 1);
		}
	}

	@Override
	public String getLabelForResourceSimple(String dbResource) {
		return getLabelForResourceSimple(dbResource, 0);
	}

	private String getLabelForResourceSimple(String dbResource, int tryNo) {
		if (tryNo >= CachedResourceConnector.RETRIES) {
			CachedResourceConnector.logger.error("Finally give up .. getLabelForResourceSimple");
			return null;
		}

		if (data.getResourceSimples().containsKey(dbResource)) {
			return data.getResourceSimples().get(dbResource);
		}

		try {
			final String result = connector.getLabelForResourceSimple(dbResource);
			data.getResourceSimples().put(dbResource, result);
			data.store();
			return result;
		} catch (final Exception e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
			sleep();
			return getLabelForResourceSimple(dbResource, tryNo + 1);
		}
	}

	@Override
	public Set<String> getEquivalentResources(String resource) {
		return getEquivalentResources(resource, 0);
	}

	private Set<String> getEquivalentResources(String resource, int tryNo) {
		if (tryNo >= CachedResourceConnector.RETRIES) {
			CachedResourceConnector.logger.error("Finally give up .. getEquivalentResources");
			return new HashSet<>();
		}

		if (data.getEquivalentResources().containsKey(resource)) {
			return data.getEquivalentResources().get(resource);
		}

		try {
			final TreeSet<String> result = new TreeSet<>(connector.getEquivalentResources(resource));
			data.getEquivalentResources().put(resource, result);
			data.store();
			return result;
		} catch (final Exception e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
			sleep();
			return getEquivalentResources(resource, tryNo + 1);
		}
	}

	private void sleep() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
		}
	}
}