package ai.lum.odinson.state

import com.typesafe.config.Config

import scala.collection.mutable

class AwayMemoryStateFactory extends StateFactory {

  override def usingState[T](function: State => T): T = {
    function(new HomeMemoryState())
  }
}

object AwayMemoryStateFactory {

  def apply(config: Config): AwayMemoryStateFactory = {
    new AwayMemoryStateFactory()
  }
}