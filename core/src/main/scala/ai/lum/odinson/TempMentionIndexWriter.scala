package ai.lum.odinson

import java.io.File
import java.util.Collection

import ai.lum.common.ConfigFactory
import ai.lum.common.ConfigUtils._
import com.typesafe.config.{Config, ConfigValueFactory}
import com.typesafe.scalalogging.LazyLogging
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.{StoredField, TextField}
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.{Directory, FSDirectory, RAMDirectory}
import org.apache.lucene.util.BytesRef
import org.apache.lucene.{document => lucenedoc}
import java.nio.charset.StandardCharsets.UTF_8

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

class TempMentionIndexWriter(
  val directory: Directory,
  val labelVocabulary: Seq[String],
  val uniqueDocSentIdField: String,
) extends LazyLogging {

  val analyzer = new WhitespaceAnalyzer()
  val writerConfig = new IndexWriterConfig(analyzer)
  writerConfig.setOpenMode(OpenMode.CREATE)
  val writer = new IndexWriter(directory, writerConfig)

  def addDocuments(block: Seq[lucenedoc.Document]): Unit = {
    addDocuments(block.asJava)
  }

  def addDocuments(block: Collection[lucenedoc.Document]): Unit = {
    writer.addDocuments(block)
  }

  def mkAndAddFakeMentions(ids: Seq[String], n: Int): Unit = {
    val numDocs = ids.length
    val numLabels = labelVocabulary.length

    val mentions = new ArrayBuffer[FakeMention]()
    mentions.append(FakeMention("66d57214-bdf1-4af0-8db7-29a6284cee90--0", "A"))
    for (i <- 0 until n) {
      val chosenId = scala.util.Random.nextInt(numDocs)
      val chosenLabel = scala.util.Random.nextInt(numLabels)
      val mention = FakeMention(ids(chosenId), labelVocabulary(chosenLabel))
      mentions.append(mention)
    }
    val block = mkMentionBlock(mentions)

    // Add the fake mention block
    addDocuments(block)
  }

  def commit(): Unit = writer.commit()

  def close(): Unit = {
    writer.close()
  }

  case class FakeMention(docSentId: String, label: String)

  /** generates a lucenedoc document per sentence */
  def mkMentionBlock(ms: Seq[FakeMention]): Seq[lucenedoc.Document] = {
    val block = ArrayBuffer.empty[lucenedoc.Document]
    for (m <- ms) {
      block += mkMentionDoc(m)
    }
    block
  }

  def mkMentionDoc(m: FakeMention): lucenedoc.Document = {
    val ment = new lucenedoc.Document
    // Mention label
    ment.add(new lucenedoc.TextField("label", m.label, Store.NO))
    // Doc id where mention is located
    ment.add(new TextField(uniqueDocSentIdField, m.docSentId, Store.NO))
    ment.add(new lucenedoc.SortedDocValuesField(uniqueDocSentIdField, new BytesRef(m.docSentId)))

    ment
  }


}


object TempMentionIndexWriter {

  def fromConfig(): TempMentionIndexWriter = {
    fromConfig("odinson")
  }

  def fromConfig(path: String): TempMentionIndexWriter = {
    val config = ConfigFactory.load()
    fromConfig(config[Config](path))
  }

  def fromConfig(config: Config): TempMentionIndexWriter = {
    val indexDir = config[String]("mentionIndexDir")
    val uniqueDocSentIdField = config[String]("index.uniqueDocSentIdField")
    val labelVocabulary = config[List[String]]("index.fakeMentionLabels")

    val directory = indexDir match {
      case ":memory:" =>
        // memory index is supported in the configuration file
        new RAMDirectory
      case path =>
        FSDirectory.open(new File(path).toPath)
    }
    new TempMentionIndexWriter(
      directory,
      labelVocabulary,
      uniqueDocSentIdField,
    )
  }

  def inMemory: TempMentionIndexWriter = {
    val config = ConfigFactory.load()
    // if the user wants the index to live in memory then we override the configuration
    val newConfig = config.withValue("odinson.indexDir", ConfigValueFactory.fromAnyRef(":memory:"))
    fromConfig(newConfig[Config]("odinson"))
  }

}
