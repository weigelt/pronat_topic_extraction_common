package edu.kit.ipd.parse.topic_extraction_common.ontology;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.topic_extraction_common.ontology.DBPediaConnector;

@Ignore
public class OnlineDBPediaConnectorTest {
	private static final Logger logger = LoggerFactory.getLogger(OnlineDBPediaConnectorTest.class);

	private DBPediaConnector connector;

	@Before
	public void before() {
		connector = new DBPediaConnector();
	}

	@Test
	public void test() {
		String resource = connector.getResourceStringFor("Refrigerator").orElse("NONE");
		Set<String> rel = new HashSet<>();
		if (!resource.equals("NONE")) {
			rel = connector.getRelatedFor(resource);
		}
		logger.info(resource);
		logger.info(Arrays.toString(rel.toArray()));
	}

}
