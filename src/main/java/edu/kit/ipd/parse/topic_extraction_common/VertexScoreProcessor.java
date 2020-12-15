/**
 *
 */
package edu.kit.ipd.parse.topic_extraction_common;

import java.util.List;
import java.util.Set;

/**
 * @author Jan Keim
 *
 */
public interface VertexScoreProcessor {
	Set<VertexScoreTuple> processCentralityScores(List<VertexScoreTuple> centralityScoresTuples);
}
