package edu.kit.ipd.parse.topic_extraction_common.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import org.jgrapht.alg.scoring.PageRank;

/**
 * PageRank implementation. Adapted by Jan Keim to fit a biased version. Could
 * not use inheritance, as original class was final...
 *
 * <p>
 * The <a href="https://en.wikipedia.org/wiki/PageRank">wikipedia</a> article
 * contains a nice description of PageRank. The method can be found on the
 * article: Sergey Brin and Larry Page: The Anatomy of a Large-Scale
 * Hypertextual Web Search Engine. Proceedings of the 7th World-Wide Web
 * Conference, Brisbane, Australia, April 1998. See also the following
 * <a href="http://infolab.stanford.edu/~backrub/google.html">page</a>.
 * </p>
 *
 * <p>
 * This is a simple iterative implementation of PageRank which stops after a
 * given number of iterations or if the PageRank values between two iterations
 * do not change more than a predefined value. The implementation uses the
 * variant which divides by the number of nodes, thus forming a probability
 * distribution over graph nodes.
 * </p>
 *
 * <p>
 * Each iteration of the algorithm runs in linear time O(n+m) when n is the
 * number of nodes and m the number of edges of the graph. The maximum number of
 * iterations can be adjusted by the caller. The default value is
 * {@link PageRank#MAX_ITERATIONS_DEFAULT}.
 * </p>
 *
 * <p>
 * If the graph is a weighted graph, a weighted variant is used where the
 * probability of following an edge e out of node v is equal to the weight of e
 * over the sum of weights of all outgoing edges of v.
 * </p>
 *
 * @param <V>
 *            the graph vertex type
 * @param <E>
 *            the graph edge type
 *
 * @author Dimitrios Michail
 * @author Jan Keim
 * @since November 2017
 */
public final class BiasedPageRank<E> implements VertexScoringAlgorithm<WikiVertex, Double> {

	/**
	 * Default number of maximum iterations.
	 */
	public static final int MAX_ITERATIONS_DEFAULT = 100;

	/**
	 * Default value for the tolerance. The calculation will stop if the difference
	 * of PageRank values between iterations change less than this value.
	 */
	public static final double TOLERANCE_DEFAULT = 0.0001;

	/**
	 * Damping factor default value.
	 */
	public static final double DAMPING_FACTOR_DEFAULT = 0.85d;

	private final Graph<WikiVertex, E> g;
	private Map<WikiVertex, Double> scores;

	private List<WikiVertex> senses;

	/**
	 * Create and execute an instance of PageRank.
	 *
	 * @param g
	 *            the input graph
	 */
	public BiasedPageRank(Graph<WikiVertex, E> g, List<WikiVertex> senses) {
		this(g, DAMPING_FACTOR_DEFAULT, MAX_ITERATIONS_DEFAULT, TOLERANCE_DEFAULT, senses);
	}

	/**
	 * Create and execute an instance of PageRank.
	 *
	 * @param g
	 *            the input graph
	 * @param dampingFactor
	 *            the damping factor
	 */
	public BiasedPageRank(Graph<WikiVertex, E> g, double dampingFactor, List<WikiVertex> senses) {
		this(g, dampingFactor, MAX_ITERATIONS_DEFAULT, TOLERANCE_DEFAULT, senses);
	}

	/**
	 * Create and execute an instance of PageRank.
	 *
	 * @param g
	 *            the input graph
	 * @param dampingFactor
	 *            the damping factor
	 * @param maxIterations
	 *            the maximum number of iterations to perform
	 */
	public BiasedPageRank(Graph<WikiVertex, E> g, double dampingFactor, int maxIterations, List<WikiVertex> senses) {
		this(g, dampingFactor, maxIterations, TOLERANCE_DEFAULT, senses);
	}

	/**
	 * Create and execute an instance of PageRank.
	 *
	 * @param g
	 *            the input graph
	 * @param dampingFactor
	 *            the damping factor
	 * @param maxIterations
	 *            the maximum number of iterations to perform
	 * @param tolerance
	 *            the calculation will stop if the difference of PageRank values
	 *            between iterations change less than this value
	 */
	public BiasedPageRank(Graph<WikiVertex, E> g, double dampingFactor, int maxIterations, double tolerance, List<WikiVertex> senses) {
		this.g = g;
		scores = new HashMap<>();

		this.senses = senses;

		if (maxIterations <= 0) {
			throw new IllegalArgumentException("Maximum iterations must be positive");
		}

		if ((dampingFactor < 0.0) || (dampingFactor > 1.0)) {
			throw new IllegalArgumentException("Damping factor not valid");
		}

		if (tolerance <= 0.0) {
			throw new IllegalArgumentException("Tolerance not valid, must be positive");
		}

		run(dampingFactor, maxIterations, tolerance);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<WikiVertex, Double> getScores() {
		return Collections.unmodifiableMap(scores);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Double getVertexScore(WikiVertex v) {
		if (!g.containsVertex(v)) {
			throw new IllegalArgumentException("Cannot return score of unknown vertex");
		}
		return scores.get(v);
	}

	private void run(double dampingFactor, int maxIterations, double tolerance) {
		// initialization
		Specifics specifics;
		if (g instanceof DirectedGraph<?, ?>) {
			specifics = new DirectedSpecifics((DirectedGraph<WikiVertex, E>) g);
		} else {
			specifics = new UndirectedSpecifics(g);
		}

		final int totalVertices = g.vertexSet().size();
		final boolean weighted = (g instanceof WeightedGraph<?, ?>);
		Map<WikiVertex, Double> weights;
		if (weighted) {
			weights = new HashMap<>(totalVertices);
		} else {
			weights = Collections.emptyMap();
		}

		final double initScore = 1.0d / totalVertices;
		for (final WikiVertex v : g.vertexSet()) {
			scores.put(v, initScore);
			if (weighted) {
				double sum = 0;
				for (final E e : specifics.outgoingEdgesOf(v)) {
					sum += g.getEdgeWeight(e);
				}
				weights.put(v, sum);
			}
		}

		// run PageRank
		Map<WikiVertex, Double> nextScores = new HashMap<>();
		double maxChange = tolerance;

		while ((maxIterations > 0) && (maxChange >= tolerance)) {
			// compute next iteration scores
			double r = 0d;
			for (final WikiVertex v : g.vertexSet()) {
				if (specifics.outgoingEdgesOf(v).size() > 0) {
					r += (1d - dampingFactor) * getBias(v) * scores.get(v);
				} else {
					r += scores.get(v);
				}
			}
			r /= totalVertices;

			maxChange = 0d;
			for (final WikiVertex v : g.vertexSet()) {
				double contribution = 0d;

				if (weighted) {
					for (final E e : specifics.incomingEdgesOf(v)) {
						final WikiVertex w = Graphs.getOppositeVertex(g, e, v);
						contribution += (dampingFactor * scores.get(w) * g.getEdgeWeight(e)) / weights.get(w);
					}
				} else {
					for (final E e : specifics.incomingEdgesOf(v)) {
						final WikiVertex w = Graphs.getOppositeVertex(g, e, v);
						contribution += (dampingFactor * scores.get(w)) / specifics.outgoingEdgesOf(w).size();
					}
				}

				final double vOldValue = scores.get(v);
				final double vNewValue = r + contribution;
				maxChange = Math.max(maxChange, Math.abs(vNewValue - vOldValue));
				nextScores.put(v, vNewValue);
			}

			// swap scores
			final Map<WikiVertex, Double> tmp = scores;
			scores = nextScores;
			nextScores = tmp;

			// progress
			maxIterations--;
		}

	}

	private double getBias(WikiVertex v) {
		// TODO tune this
		if (senses.contains(v)) {
			return 1.0 / senses.size();
		} else {
			return 0;
		}
	}

	abstract class Specifics {
		public abstract Set<? extends E> outgoingEdgesOf(WikiVertex vertex);

		public abstract Set<? extends E> incomingEdgesOf(WikiVertex vertex);
	}

	class DirectedSpecifics extends Specifics {
		private final DirectedGraph<WikiVertex, E> graph;

		public DirectedSpecifics(DirectedGraph<WikiVertex, E> g) {
			graph = g;
		}

		@Override
		public Set<? extends E> outgoingEdgesOf(WikiVertex vertex) {
			return graph.outgoingEdgesOf(vertex);
		}

		@Override
		public Set<? extends E> incomingEdgesOf(WikiVertex vertex) {
			return graph.incomingEdgesOf(vertex);
		}
	}

	class UndirectedSpecifics extends Specifics {
		private final Graph<WikiVertex, E> graph;

		public UndirectedSpecifics(Graph<WikiVertex, E> g) {
			graph = g;
		}

		@Override
		public Set<E> outgoingEdgesOf(WikiVertex vertex) {
			return graph.edgesOf(vertex);
		}

		@Override
		public Set<E> incomingEdgesOf(WikiVertex vertex) {
			return graph.edgesOf(vertex);
		}
	}

}
