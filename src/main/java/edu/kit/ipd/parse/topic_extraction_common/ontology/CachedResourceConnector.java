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

	private static final int RETRIES = 3;

	private final ResourceConnector connector;

	private final CacheData data;

	public CachedResourceConnector(ResourceConnector connector) {
		this.connector = connector;
		this.data = CacheData.loadData();
	}

	@Override
	public Optional<String> getResourceStringFor(String label) {
		return this.getResourceStringFor(label, 0);
	}

	private Optional<String> getResourceStringFor(String label, int tryNo) {
		if (tryNo >= CachedResourceConnector.RETRIES) {
			CachedResourceConnector.logger.error("Finally give up .. getResourceStringFor");
			return Optional.empty();
		}

		if (this.data.getResources().containsKey(label)) {
			return Optional.ofNullable(this.data.getResources().get(label));
		}

		try {
			final Optional<String> result = this.connector.getResourceStringFor(label);
			this.data.getResources().put(label, result.orElse(null));
			this.data.store();
			return result;
		} catch (final Exception e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
			// Wait because of possible timeout of online resource
			this.sleep();
			return this.getResourceStringFor(label, tryNo + 1);
		}

	}

	@Override
	public Set<String> getRelatedFor(String resource) {
		return this.getRelatedFor(resource, 0);
	}

	private Set<String> getRelatedFor(String resource, int tryNo) {
		if (tryNo >= CachedResourceConnector.RETRIES) {
			CachedResourceConnector.logger.error("Finally give up .. getRelatedFor");
			return new TreeSet<>();
		}

		if (this.data.getRelated().containsKey(resource)) {
			return this.data.getRelated().get(resource);
		}

		try {
			final TreeSet<String> result = new TreeSet<>(this.connector.getRelatedFor(resource));
			this.data.getRelated().put(resource, result);
			this.data.store();
			return result;
		} catch (final Exception e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
			// Wait because of possible timeout of online resource
			this.sleep();
			return this.getRelatedFor(resource, tryNo + 1);
		}
	}

	@Override
	public Optional<String> getLabelForResource(String resource) {
		return this.getLabelForResource(resource, 0);
	}

	private Optional<String> getLabelForResource(String resource, int tryNo) {
		if (tryNo >= CachedResourceConnector.RETRIES) {
			CachedResourceConnector.logger.error("Finally give up .. getLabelForResource");
			return Optional.empty();
		}

		if (this.data.getLabels().containsKey(resource)) {
			return Optional.ofNullable(this.data.getLabels().get(resource));
		}

		try {
			final Optional<String> result = this.connector.getLabelForResource(resource);
			this.data.getLabels().put(resource, result.orElse(null));
			this.data.store();
			return result;
		} catch (final Exception e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
			this.sleep();
			return this.getLabelForResource(resource, tryNo + 1);
		}
	}

	@Override
	public String getLabelForResourceSimple(String dbResource) {
		return this.getLabelForResourceSimple(dbResource, 0);
	}

	private String getLabelForResourceSimple(String dbResource, int tryNo) {
		if (tryNo >= CachedResourceConnector.RETRIES) {
			CachedResourceConnector.logger.error("Finally give up .. getLabelForResourceSimple");
			return null;
		}

		if (this.data.getResourceSimples().containsKey(dbResource)) {
			return this.data.getResourceSimples().get(dbResource);
		}

		try {
			final String result = this.connector.getLabelForResourceSimple(dbResource);
			this.data.getResourceSimples().put(dbResource, result);
			this.data.store();
			return result;
		} catch (final Exception e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
			this.sleep();
			return this.getLabelForResourceSimple(dbResource, tryNo + 1);
		}
	}

	@Override
	public Set<String> getEquivalentResources(String resource) {
		return this.getEquivalentResources(resource, 0);
	}

	private Set<String> getEquivalentResources(String resource, int tryNo) {
		if (tryNo >= CachedResourceConnector.RETRIES) {
			CachedResourceConnector.logger.error("Finally give up .. getEquivalentResources");
			return new HashSet<>();
		}

		if (this.data.getEquivalentResources().containsKey(resource)) {
			return this.data.getEquivalentResources().get(resource);
		}

		try {
			final TreeSet<String> result = new TreeSet<>(this.connector.getEquivalentResources(resource));
			this.data.getEquivalentResources().put(resource, result);
			this.data.store();
			return result;
		} catch (final Exception e) {
			CachedResourceConnector.logger.error(e.getMessage(), e.getCause());
			this.sleep();
			return this.getEquivalentResources(resource, tryNo + 1);
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