package ai.lum.odinson.extra

import java.io.File

import ai.lum.common.ConfigFactory
import ai.lum.common.ConfigUtils._
import ai.lum.odinson.ExtractorEngine
import ai.lum.odinson.lucene.search.OdinsonFilteredQuery
import org.apache.lucene.index.{DirectoryReader, Term}
import org.apache.lucene.search.{BooleanClause, BooleanQuery, IndexSearcher, TermQuery}
import org.apache.lucene.search.join.{JoinUtil, ScoreMode}
import org.apache.lucene.store.{Directory, FSDirectory}

object FSJoinQueryTimer extends App {

  var config = ConfigFactory.load()

  // Odinson Searcher
  val engine = ExtractorEngine.fromConfig()
  val sentenceSearcher = engine.indexSearcher

  // OdinsonQuery that should match all documents
  val odinsonQuery = engine.compiler.compile("""[]""")

  // Fake mention Searcher
  val mentionIndexPath = config[File]("odinson.mentionIndexDir").toPath
  val mentionDirectory: Directory = FSDirectory.open(mentionIndexPath)
  val mentionReader = DirectoryReader.open(mentionDirectory)
  val mentionSearcher = new IndexSearcher(mentionReader)

  // Labels of the fake mentions
  val labelVocabulary: List[String] = config[List[String]]("odinson.index.fakeMentionLabels")
  val numLabels = labelVocabulary.length

  // The pieces of the query-time join
  val uniqueDocSentIdField = config[String]("odinson.index.uniqueDocSentIdField")

  // Field (uniqDocSentId) is the same in both types of docs
  val fromField: String = uniqueDocSentIdField
  val toField: String = fromField

  // todo: false? or true?
  val multipleValuesPerDocument = false

  // how many queries to make....
  val reps: Int = 1

  for (i <- 0 until reps) {

    val chosenLabel = scala.util.Random.nextInt(numLabels)
    // The query for a given mention type (by label)
    val fromQuery = new TermQuery(new Term("label", labelVocabulary(chosenLabel)))
    println(labelVocabulary(chosenLabel))
    // joined with the sentence searcher
    val joinQuery = JoinUtil.createJoinQuery(fromField, multipleValuesPerDocument, toField, fromQuery, mentionSearcher, ScoreMode.Max)
    // and now put together with the query from above
    val finalQuery = new OdinsonFilteredQuery(odinsonQuery, joinQuery)
    // apply the query
    val results = engine.query(finalQuery)
    println(s"finalquery numresults: ${results.totalHits}")

    val joinresults = engine.indexSearcher.search(joinQuery, 10)
    println(s"joinquery numresults: ${joinresults.totalHits}")

    val fromresults = mentionSearcher.search(fromQuery, 10)
    println(s"fromquery numresults: ${fromresults.totalHits}")

    val toresults = engine.query(odinsonQuery)
    println(s"odinsonQuery numresults: ${toresults.totalHits}")
  }

}
