package ai.lum.odinson.extra

import java.io._

import ai.lum.common.ConfigFactory
import ai.lum.common.ConfigUtils._
import ai.lum.common.TryWithResources._
import ai.lum.odinson.TempMentionIndexWriter
import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

object IndexFakeMentions extends App with LazyLogging {

  var config = ConfigFactory.load()

  val numMentions: Int = config[Int]("indexmentions.numMentions")
  val uniqueDocsIds: File = config[File]("indexmentions.uniqueIds")
  val writer = TempMentionIndexWriter.fromConfig(config.getConfig("odinson"))


  val ids = using(Source.fromFile(uniqueDocsIds)) { src =>
    src.getLines().toArray
  }

  // ^ this part should be a function
  logger.info("Indexing mentions")
  writer.mkAndAddFakeMentions(ids, numMentions)
  writer.close
  // fin
}
