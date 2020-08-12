package ai.lum.odinson.state

import ai.lum.common.TryWithResources.using
import ai.lum.odinson.ExtractorEngine
import com.typesafe.config.Config

class AwayLuceneStateFactory() extends StateFactory {

  override def usingState[T](function: State => T): T = {
    val clazz = Class.forName("ai.lum.odinson.state.LuceneState")
    val instance = clazz.newInstance().asInstanceOf[State]

    using(instance) { state =>
      function(state)
    }
  }
}

object AwayLuceneStateFactory {

  def apply(config: Config): AwayLuceneStateFactory = {
    new AwayLuceneStateFactory()
  }

  def main(args: Array[String]): Unit = {
    println("Keith was here")
    val config = ExtractorEngine.defaultConfig
    val stateFactory = AwayLuceneStateFactory(config)

    stateFactory.usingState { state =>
      state.getDocIds(0, "label")
    }
  }
}
