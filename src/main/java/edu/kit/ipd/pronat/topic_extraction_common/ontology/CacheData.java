package edu.kit.ipd.pronat.topic_extraction_common.ontology;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.kit.ipd.parse.luna.tools.ConfigManager;

/**
 * @author Jan Keim
 * @author Sebastian Weigelt
 *
 */
public final class CacheData implements Serializable {
	private static final long serialVersionUID = -4339271771491449916L;
	private static final String PATH = ConfigManager.APP_HOME + File.separator + CachedResourceConnector.class.getName();
	private static final String FILE = "data.json";
	private static final int WRITE_AT = 1000;
	private transient int writeCounter = 0;

	private static CacheData instance;

	private final TreeMap<String, String> resources = new TreeMap<>();

	private final TreeMap<String, TreeSet<String>> related = new TreeMap<>();

	private final TreeMap<String, String> labels = new TreeMap<>();

	private final TreeMap<String, String> resourceSimples = new TreeMap<>();

	private final TreeMap<String, TreeSet<String>> equivalentResources = new TreeMap<>();

	public static CacheData loadData() {
		synchronized (CacheData.class) {
			if (CacheData.instance != null) {
				return CacheData.instance;
			}
			try {
				final File store = new File(CacheData.PATH + File.separator + CacheData.FILE);
				if (!store.exists()) {
					return CacheData.instance = new CacheData();
				}
				return CacheData.instance = getObjectMapper().readValue(store, CacheData.class);
			} catch (final IOException e) {
				e.printStackTrace();
				return CacheData.instance = new CacheData();
			}
		}
	}

	public TreeMap<String, String> getResources() {
		return resources;
	}

	public TreeMap<String, String> getLabels() {
		return labels;
	}

	public TreeMap<String, TreeSet<String>> getRelated() {
		return related;
	}

	public TreeMap<String, String> getResourceSimples() {
		return resourceSimples;
	}

	public TreeMap<String, TreeSet<String>> getEquivalentResources() {
		return equivalentResources;
	}

	public void store() {
		if (++writeCounter % CacheData.WRITE_AT == 0) {
			write();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		write();
	}

	private void write() {
		synchronized (CacheData.class) {
			try {
				new File(CacheData.PATH).mkdirs();
				final File store = new File(CacheData.PATH + File.separator + CacheData.FILE);
				getObjectMapper().writeValue(store, this);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get a new default object mapper for the project.
	 *
	 * @param indent
	 *            indicator for indention
	 * @return a new default object mapper
	 */
	private static ObjectMapper getObjectMapper() {
		final var objectMapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, false);
		objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker() //
				.withFieldVisibility(Visibility.ANY)//
				.withGetterVisibility(Visibility.NONE)//
				.withSetterVisibility(Visibility.NONE)//
				.withIsGetterVisibility(Visibility.NONE));
		return objectMapper;
	}
}
