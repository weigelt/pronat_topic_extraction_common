package edu.kit.ipd.parse.topic_extraction_common.ontology;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * @author Jan Keim
 *
 */
public class DBPediaConnector implements ResourceConnector {

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
		this.stopwords = this.loadStopwords();
	}

	private List<String> loadStopwords() {
		List<String> retList = new ArrayList<>();
		final URL fileURL = DBPediaConnector.class.getResource("/stopwords.txt");
		try (Stream<String> lines = Files.lines(Paths.get(fileURL.toURI()))) {
			retList = lines.map(s -> s.replaceAll("\"", "")).collect(Collectors.toList());
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		return retList;
	}

	@Override
	public Optional<String> getResourceStringFor(String queryLabel) {
		Objects.requireNonNull(queryLabel);
		String queryString = String.format(this.getBaseDbrString(), queryLabel);

		String retString = null;
		try (QueryExecution qexec = this.createQueryExecution(queryString)) {
			final ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				final QuerySolution soln = results.nextSolution();
				final String res = soln.get("res").toString();
				final String label = soln.get("label").toString();
				if (this.checkLabelWithQuery(label, queryLabel) && this.checkResource(res)) {
					retString = res;
					break;
				}
			}
			qexec.close();
		}

		if (Objects.nonNull(retString)) {
			// get the page the page is redirected to
			queryString = "PREFIX dbo: <http://dbpedia.org/ontology/> SELECT DISTINCT ?res WHERE { <%s> dbo:wikiPageRedirects ?res}";
			queryString = String.format(queryString, retString);
			try (QueryExecution qexec = this.createQueryExecution(queryString)) {
				final ResultSet results = qexec.execSelect();
				if (results.hasNext()) {
					final QuerySolution soln = results.nextSolution();
					retString = soln.get("res").toString();
				}
				qexec.close();
			}
		}
		return Optional.ofNullable(retString);
	}

	@Override
	public Set<String> getEquivalentResources(String resource) {
		Objects.nonNull(resource);
		final Set<String> retSet = new HashSet<>();

		final String queryString = String.format(DBPediaConnector.getBaseRedirectQuery(), resource);

		try (QueryExecution qexec = this.createQueryExecution(queryString)) {
			final ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				final QuerySolution soln = results.nextSolution();
				final String uri = soln.get("uri").toString();
				retSet.add(uri);
			}
			qexec.close();
		}

		return retSet;
	}

	protected boolean checkResource(String res) {
		return res.startsWith("http://dbpedia.org/resource") && !res.contains("Category:");
	}

	protected boolean checkLabelWithQuery(String label, String queryLabel) {
		label = label.replaceAll("\"", "");
		label = label.replace("@en", "");
		if (label.equals(queryLabel)) {
			// completely equal
			return true;
		} else if (label.equalsIgnoreCase(queryLabel)) {
			// ignoring case equal
			if (label.matches("^[\\p{Lu}]{2,}.*")) {
				// is "all-uppercase" (at least 2 uppercase characters
				return false;
			} else {
				// only first character uppercase
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<String> getRelatedFor(String dbResource) {
		Objects.requireNonNull(dbResource);
		final Set<String> retSet = new HashSet<>();

		final String queryString = String.format(this.getBaseRelatedString(), dbResource);

		try (QueryExecution qexec = this.createQueryExecution(queryString)) {
			final ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				final QuerySolution soln = results.nextSolution();
				final String uri = soln.get("uri").toString();
				if (this.checkURI(uri)) {
					retSet.add(uri);
				}
			}
			qexec.close();
		}

		return retSet;
	}

	private boolean checkURI(String uri) {
		return uri.startsWith("http://dbpedia.org/resource") && !uri.contains("File:") && !uri.contains("Wikt:")
				&& this.isNoStopWord(this.getLabelForResourceSimple(uri));
	}

	@Override
	public String getLabelForResourceSimple(String dbResource) {
		final String cleaner = dbResource.replaceAll("http:\\/\\/[\\w\\.]+(\\/\\w*)?\\/(\\w+:)?([\\w\\(\\)]+)", "$3");
		return cleaner.replaceAll("_", " ");
	}

	@Override
	public Optional<String> getLabelForResource(String dbResource) {
		Objects.requireNonNull(dbResource);
		String retString = "";

		final String query = String.format(baseLabelQuery, dbResource);
		try (QueryExecution qexec = this.createQueryExecution(query)) {
			final ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				final QuerySolution soln = results.nextSolution();
				final String label = soln.get("label").toString();
				retString = label.replaceAll("\"", "").replace("@en", "");
				break;
			}
			qexec.close();
		}

		return Optional.ofNullable(retString);
	}

	protected QueryExecution createQueryExecution(String queryString) {
		final QueryEngineHTTP qe = new QueryEngineHTTP(this.serviceUrl, queryString);
		qe.addParam("timeout", "30000");
		return qe;
	}

	protected boolean isNoStopWord(String label) {
		if (label.matches("^\\d+.*")) {
			// starts with a year
			return false;
		}
		// check for stopwords defined
		for (final String stopword : this.stopwords) {
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
		// return baseRelatedString_paper;
	}

	/**
	 * @return the baseredirectquery
	 */
	protected static String getBaseRedirectQuery() {
		return baseRedirectQuery;
	}
}
