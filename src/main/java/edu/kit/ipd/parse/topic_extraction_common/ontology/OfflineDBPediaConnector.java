package edu.kit.ipd.parse.topic_extraction_common.ontology;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;

/**
 * @author Jan Keim
 *
 */
public class OfflineDBPediaConnector extends DBPediaConnector {
	// private static final Logger logger =
	// LoggerFactory.getLogger(OfflineDBPediaConnector.class);
	private static final String baseDbrString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "SELECT DISTINCT ?res ?label WHERE {" + "?res rdfs:label ?label . "
			+ "FILTER contains(LCASE(?label),LCASE(\"%s\")) .} ORDER BY (strlen(?label)) LIMIT 21";

	private OntModel model;

	public OfflineDBPediaConnector(String ontoFile, String... turtleFiles) {
		model = OfflineDBPediaConnector.loadOntologyModel(ontoFile);

		for (String nTripleFile : turtleFiles) {
			if ((nTripleFile != null) && !nTripleFile.isEmpty()) {
				OfflineDBPediaConnector.loadTurtlesToModel(nTripleFile, model);
			}
		}
	}

	public Optional<String> getResourceStringFor_optional(String queryLabel) {
		Property p = model.getProperty("http://www.w3.org/2000/01/rdf-schema#label");
		ResIterator it = model.listSubjectsWithProperty(p);
		String uri = null;
		while (it.hasNext()) {
			Resource resource = it.next();
			Statement s = resource.getProperty(p);
			String label = s.getString();
			uri = resource.getURI();
			if (checkResource(uri) && checkLabelWithQuery(label, queryLabel)) {
				break;
			}
		}
		return Optional.ofNullable(uri);
	}

	@Override
	public Optional<String> getLabelForResource(String dbResource) {
		Resource res = model.getResource("http://dbpedia.org/resource/Aristotle");
		Property p = model.getProperty("http://www.w3.org/2000/01/rdf-schema#label");
		Statement s = res.getProperty(p);
		Optional<String> labelOptional = Optional.empty();
		if (s != null) {
			labelOptional = Optional.of(s.getString());
		}
		return labelOptional;
	}

	@Override
	protected QueryExecution createQueryExecution(String queryString) {
		Query query = QueryFactory.create(queryString);
		return QueryExecutionFactory.create(query, model);
	}

	private static OntModel loadOntologyModel(String ontoFile) {
		OntModel ontoModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		try (InputStream in = FileManager.get().open(ontoFile)) {
			ontoModel.read(in, null);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ontoModel;
	}

	private static OntModel loadTurtlesToModel(String turtlesFile, OntModel model) {
		return OfflineDBPediaConnector.loadFileToModel(turtlesFile, model, Lang.TTL);
	}

	private static OntModel loadFileToModel(String triplesFile, OntModel model, Lang fileLang) {
		try (InputStream in = FileManager.get().open(triplesFile)) {
			RDFDataMgr.read(model, in, fileLang);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return model;
	}

	@Override
	protected String getBaseDbrString() {
		return baseDbrString;
	}

}
