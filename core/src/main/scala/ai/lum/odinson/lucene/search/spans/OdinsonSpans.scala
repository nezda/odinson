package ai.lum.odinson.lucene.search.spans

import scala.collection.mutable.ArrayBuffer
import org.apache.lucene.search.spans.Spans
import ai.lum.odinson._

/**
 * Iterates through combinations of start/end positions per-doc.
 * Each start/end position represents a range of term positions within
 * the current document. These are enumerated in order, by increasing
 * document number, within that by increasing start position and finally
 * by increasing end position.
 *
 * (copied from lucene documentation)
 */
abstract class OdinsonSpans extends Spans {

  import Spans._

  def odinsonMatch: OdinsonMatch = {
    new NGramMatch(docID(), startPosition(), endPosition())
  }

  def width(): Int = 0

  def odinDoStartCurrentDoc() = doStartCurrentDoc()

  def odinDoCurrentSpans() = doCurrentSpans()

  def getAllMatches(): ArrayBuffer[OdinsonMatch] = {
    val buffer = new ArrayBuffer[OdinsonMatch]
    while (nextStartPosition() != NO_MORE_POSITIONS) {
      buffer += odinsonMatch
    }
    buffer
  }

}
