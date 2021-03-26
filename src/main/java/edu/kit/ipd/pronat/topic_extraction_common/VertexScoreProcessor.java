/**
 *
 */
package edu.kit.ipd.pronat.topic_extraction_common;

import java.util.List;
import java.util.Set;

/**
 * @author Jan Keim
 * @author Sebastian Weigelt
 *
 */
public interface VertexScoreProcessor {
	Set<VertexScoreTuple> processCentralityScores(List<VertexScoreTuple> centralityScoresTuples);
}
