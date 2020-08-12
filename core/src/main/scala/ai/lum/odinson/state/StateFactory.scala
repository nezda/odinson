package ai.lum.odinson.state

import com.typesafe.config.Config
import ai.lum.common.ConfigUtils._
import ai.lum.common.TryWithResources.using

trait StateFactory {
  def usingState[T](function: State => T): T
}

object StateFactory {

  def apply(config: Config): StateFactory = {
    val provider = config[String]("state.provider")
    val stateFactory = provider match {
      case "file" => FileStateFactory(config)
      case "lucene" => AwayLuceneStateFactory(config)
//      case "lucene" => HomeLuceneStateFactory(config)
      case "memory" => AwayMemoryStateFactory(config)
//      case "memory" => HomeMemoryStateFactory(config)
      case "sql" => SqlStateFactory(config)
      case _ => throw new Exception(s"Unknown state provider: $provider")
    }

    stateFactory
  }
}
