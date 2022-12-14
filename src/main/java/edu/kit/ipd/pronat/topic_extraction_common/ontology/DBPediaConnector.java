package edu.kit.ipd.pronat.topic_extraction_common.ontology;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jan Keim
 * @author Sebastian Weigelt
 *
 */
public class DBPediaConnector implements ResourceConnector {
	private static final Logger logger = LoggerFactory.getLogger(DBPediaConnector.class);

	public static final String DEFAULT_SERVICE_URL = "http://dbpedia.org/sparql";
	private final String serviceUrl;
	private static final String baseDbrString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + "PREFIX bif: <bif:>\n"
			+ "SELECT DISTINCT ?res ?label WHERE {" + "?res rdfs:label ?label . " + "FILTER (lang(?label) = 'en') . "
			+ "?label bif:contains '\"%s\"' .} ORDER BY (strlen(?label)) LIMIT 21";

	private static final String baseRelatedString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX dbo: <http://dbpedia.org/ontology/>\n" + "PREFIX purlg: <http://purl.org/linguistics/gold/>\n"
			+ "PREFIX purl: <http://purl.org/dc/terms/>\n" + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
			+ "SELECT DISTINCT ?uri WHERE { VALUES ?s {<%s>}\n" + "  {?s purl:subject ?uri} UNION\n  {?uri purl:subject ?s} UNION\n"
			+ "  {?s purlg:hypernym ?uri} UNION\n  {?uri purlg:hypernym ?s} UNION\n"
			+ "  {?s purlg:meronym ?uri} UNION\n  {?uri purlg:meronym ?s} UNION\n"
			+ "  {?s purlg:synonym ?uri} UNION\n  {?uri purlg:synonym ?s} UNION\n"
			+ "  {?s rdfs:type ?uri FILTER contains(str(?uri),\"http://dbpedia.org/ontology/\")} UNION\n"
			+ "  {?s rdfs:seeAlso ?uri} UNION\n  {?uri rdfs:seeAlso ?s} UNION\n"
			+ "  {?s rdfs:subClassOf ?uri} UNION\n {?uri rdfs:subClassOf ?s} UNION\n"
			+ "  {?s skos:broader ?uri} UNION\n  {?uri skos:broader ?s} UNION\n"
			+ "  {?uri skos:broaderOf ?s} UNION\n  {?s skos:broaderOf ?uri} UNION\n" + "  {?s skos:narrower ?uri} UNION\n"
			+ "  {?uri skos:narrower ?s}" + " . FILTER NOT EXISTS { {?uri rdf:type <http://schema.org/CreativeWork>}"
			+ " UNION {?uri rdf:type <http://dbpedia.org/ontology/Work>}"
			+ " UNION {?uri rdf:type <http://dbpedia.org/ontology/MusicalWork>}"
			+ " UNION {?uri rdf:type <http://dbpedia.org/ontology/WrittenWork>}"
			+ " UNION {?uri rdf:type <http://dbpedia.org/ontology/Album>}" + " UNION {?uri rdf:type <http://dbpedia.org/ontology/Book>}"
			+ " UNION {?uri rdf:type <http://dbpedia.org/ontology/Song>}" + " UNION {?uri rdf:type <http://dbpedia.org/ontology/Single>}"
			+ " UNION {?uri rdf:type <http://dbpedia.org/ontology/Person>}" + " UNION {?uri rdf:type <http://dbpedia.org/ontology/Group>}"
			+ " UNION {?uri rdf:type <http://dbpedia.org/ontology/Band>}" + " UNION {?uri rdf:type <http://dbpedia.org/ontology/Agent>}"
			+ " UNION {?uri rdf:type <http://dbpedia.org/ontology/Organisation>}"
			+ " UNION {?uri rdf:type <http://dbpedia.org/ontology/Company>}"
			+ " UNION {?uri rdf:type <http://dbpedia.org/ontology/Place>}}}";

	private static final String baseRelatedString_paper = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX dcterms: <http://purl.org/dc/terms/>\n" + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
			+ "SELECT DISTINCT ?uri\n" + "WHERE {\n" + "  VALUES ?s {<http://dbpedia.org/resource/Refrigerator>}\n"
			+ "  {?s skos:broader ?uri} UNION\n" + "  {?s skos:broaderOf ?uri} UNION\n" + "  {?s rdfs:subClassOf ?uri} UNION\n"
			+ "  {?s rdfs:type ?uri} UNION\n" + "  {?s dcterms:subject ?uri}}";

	private static final String baseLabelQuery = "SELECT DISTINCT ?label WHERE { <%s> rdfs:label ?label . FILTER (lang(?label) = 'en')}";

	private static final String baseRedirectQuery = "PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?uri WHERE { VALUES ?s {<%s>} .\n"
			+ "{?uri dbo:wikiPageRedirects ?s} UNION {?s dbo:wikiPageRedirects ?uri}}";

	private List<String> stopwords = Arrays.asList("Articles containing", " national ", " brands", "Companies ", " manufacturers",
			" competitions", " inventions", "Articles ", "Article Wizard", " companies", " of the", "History of ", " GmbH", "Lists of",
			"List of", "Form", " introduced in ", " by type");

	public DBPediaConnector() {
		this(DEFAULT_SERVICE_URL);
	}

	public DBPediaConnector(String serviceUrl) {
		this.serviceUrl = serviceUrl;
		stopwords = loadStopwords();
	}

	private List<String> loadStopwords() {
		List<String> retList = new ArrayList<>();
		final InputStream stopwordsStream = DBPediaConnector.class.getResourceAsStream("/stopwords.txt");
		try (Scanner lines = new Scanner(stopwordsStream)) {
			while (lines.hasNextLine()) {
				retList.add(lines.nextLine().replace("\"", ""));
			}
		}
		return retList;
	}

	@Override
	public Optional<String> getResourceStringFor(String queryLabel) {
		Objects.requireNonNull(queryLabel);
		String queryString = String.format(getBaseDbrString(), queryLabel.replace("'", " "));

		String retString = null;
		try (QueryExecution qexec = createQueryExecution(queryString)) {
			final ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				final QuerySolution soln = results.nextSolution();
				final String res = soln.get("res").toString();
				final String label = soln.get("label").toString();
				if (checkLabelWithQuery(label, queryLabel) && checkResource(res)) {
					retString = res;
					break;
				}
			}
		} catch (final Exception e) {
			logger.warn("Could not get resource string for \"{}\"", queryLabel);
			logger.debug("Used Query: \n{}", queryString);
			logger.warn(e.getMessage(), e.getCause());
		}

		if (Objects.nonNull(retString)) {
			// get the page the page is redirected to
			queryString = "PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?res WHERE { <%s> dbo:wikiPageRedirects ?res}";
			queryString = String.format(queryString, retString);
			try (QueryExecution qexec = createQueryExecution(queryString)) {
				final ResultSet results = qexec.execSelect();
				if (results.hasNext()) {
					final QuerySolution soln = results.nextSolution();
					retString = soln.get("res").toString();
				}
			}
		}
		return Optional.ofNullable(retString);
	}

	@Override
	public Set<String> getEquivalentResources(String resource) {
		Objects.nonNull(resource);
		final Set<String> retSet = new HashSet<>();

		final String queryString = String.format(DBPediaConnector.getBaseRedirectQuery(), resource);

		try (QueryExecution qexec = createQueryExecution(queryString)) {
			final ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				final QuerySolution soln = results.nextSolution();
				final String uri = soln.get("uri").toString();
				retSet.add(uri);
			}
		}

		return retSet;
	}

	protected boolean checkResource(String res) {
		return res.startsWith("http://dbpedia.org/resource") && !res.contains("Category:");
	}

	protected boolean checkLabelWithQuery(String label, String queryLabel) {
		label = label.replace("\"", "");
		label = label.replace("@en", "");
		if (label.equals(queryLabel)) {
			// completely equal
			return true;
		} else if (label.equalsIgnoreCase(queryLabel)) {
			// ignoring case equal
			return !(label.matches("^[\\p{Lu}]{2,}.*"));
		}
		return false;
	}

	@Override
	public Set<String> getRelatedFor(String dbResource) {
		Objects.requireNonNull(dbResource);
		final Set<String> retSet = new HashSet<>();

		final String queryString = String.format(getBaseRelatedString(), dbResource);

		try (QueryExecution qexec = createQueryExecution(queryString)) {
			final ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				final QuerySolution soln = results.nextSolution();
				final String uri = soln.get("uri").toString();
				if (checkURI(uri)) {
					retSet.add(uri);
				}
			}
		}

		return retSet;
	}

	private boolean checkURI(String uri) {
		return uri.startsWith("http://dbpedia.org/resource") && !uri.contains("File:") && !uri.contains("Wikt:")
				&& isNoStopWord(getLabelForResourceSimple(uri));
	}

	@Override
	public String getLabelForResourceSimple(String dbResource) {
		final String cleaner = dbResource.replaceAll("http:\\/\\/[\\w\\.]+(\\/\\w*)?\\/(\\w+:)?([\\w\\(\\)]+)", "$3");
		return cleaner.replace("_", " ");
	}

	@Override
	public Optional<String> getLabelForResource(String dbResource) {
		Objects.requireNonNull(dbResource);
		String retString = "";

		final String query = String.format(baseLabelQuery, dbResource);
		try (QueryExecution qexec = createQueryExecution(query)) {
			final ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				final QuerySolution soln = results.nextSolution();
				final String label = soln.get("label").toString();
				retString = label.replace("\"", "").replace("@en", "");
				break;
			}
		}

		return Optional.ofNullable(retString);
	}

	protected QueryExecution createQueryExecution(String queryString) {
		final QueryEngineHTTP qe = new QueryEngineHTTP(serviceUrl, queryString);
		qe.addParam("timeout", "30000");
		return qe;
	}

	protected boolean isNoStopWord(String label) {
		if (label.matches("^\\d+.*")) {
			// starts with a year
			return false;
		}
		// check for stopwords defined
		for (final String stopword : stopwords) {
			if (label.contains(stopword)) {
				// is in the s
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the baseDbrString
	 */
	protected String getBaseDbrString() {
		return baseDbrString;
	}

	/**
	 * @return the baserelatedstring
	 */
	protected String getBaseRelatedString() {
		return baseRelatedString;
	}

	/**
	 * @return the baseredirectquery
	 */
	protected static String getBaseRedirectQuery() {
		return baseRedirectQuery;
	}
}
