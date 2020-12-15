package edu.kit.ipd.parse.topic_extraction_common.ontology;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.topic_extraction_common.ontology.OfflineDBPediaConnector;

@Ignore
public class OfflineDBPediaConnectorTest {
	private static final Logger logger = LoggerFactory.getLogger(OfflineDBPediaConnectorTest.class);

	// TODO change location of test owl and ttl files
	private static final String ONTOFILE = "D:\\MA_Stuff\\DBPedia Onto\\dbpedia_2016-10.owl";
	private static final String NTRIPLES_1 = "D:\\MA_Stuff\\DBPedia Onto\\category_labels_en.ttl";
	private static final String NTRIPLES_2 = "D:\\MA_Stuff\\DBPedia Onto\\labels_en.ttl";
	// private static final String NTRIPLES_3
	// = "D:\\MA_Stuff\\DBPedia Onto\\article_categories_en.ttl";

	private static OfflineDBPediaConnector connector;

	@BeforeClass
	public static void before() {
		Instant start = Instant.now();
		connector = new OfflineDBPediaConnector(ONTOFILE, NTRIPLES_1, NTRIPLES_2);
		logger.debug("Duration for loading: " + Duration.between(start, Instant.now()).getSeconds() + " seconds");
	}

	@Test
	public void test() {
		String resource = connector.getResourceStringFor("Refrigerator").orElse("NONE");
		Set<String> rel = new HashSet<>();
		if (!resource.equals("NONE")) {
			rel = connector.getRelatedFor(resource);
		}
		logger.info(resource);
		logger.info("Set.size()=" + rel.size());
		logger.info(Arrays.toString(rel.toArray()));
	}

	@Test
	public void testGetResourceStringFor() {
		Instant start = Instant.now();
		logger.info(connector.getResourceStringFor("Refrigerator").orElse("NONE"));
		logger.debug("Duration: " + Duration.between(start, Instant.now()).getSeconds() + " seconds");
		start = Instant.now();
		logger.info(connector.getResourceStringFor("refrigerator").orElse("NONE"));
		logger.debug("Duration: " + Duration.between(start, Instant.now()).getSeconds() + " seconds");
		start = Instant.now();
		logger.info(connector.getResourceStringFor("orange (fruit)").orElse("NONE"));
		logger.debug("Duration: " + Duration.between(start, Instant.now()).getSeconds() + " seconds");
		start = Instant.now();
		logger.info(connector.getResourceStringFor("Orange (fruit)").orElse("NONE"));
		logger.debug("Duration: " + Duration.between(start, Instant.now()).getSeconds() + " seconds");
		start = Instant.now();
		logger.info(connector.getResourceStringFor("hand").orElse("NONE"));
		logger.debug("Duration: " + Duration.between(start, Instant.now()).getSeconds() + " seconds");
		start = Instant.now();
		logger.info(connector.getResourceStringFor("popcorn").orElse("NONE"));
		logger.debug("Duration: " + Duration.between(start, Instant.now()).getSeconds() + " seconds");
	}

	@Test
	public void testPerformanceToGetResource() {
		int n = 5;
		long time = 0;
		Instant start;
		for (int i = 0; i < n; i++) {
			start = Instant.now();
			connector.getResourceStringFor("Refrigerator");
			time += Duration.between(start, Instant.now()).getSeconds();
		}
		logger.info("SPARQL Version takes " + ((double) time / (double) n) + " seconds in average");
		time = 0;
		for (int i = 0; i < n; i++) {
			start = Instant.now();
			connector.getResourceStringFor_optional("Refrigerator");
			time += Duration.between(start, Instant.now()).getSeconds();
		}

		logger.info("Jena Version takes " + ((double) time / (double) n) + " seconds in average");

	}
}
