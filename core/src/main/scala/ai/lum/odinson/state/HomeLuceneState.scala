package ai.lum.odinson.state

import ai.lum.common.TryWithResources.using
import com.typesafe.config.Config

class HomeLuceneState extends State {
  override def addResultItems(resultItems: Iterator[ResultItem]): Unit = ???

  override def getDocIds(docBase: Int, label: String): Array[Int] = ???

  override def getResultItems(docBase: Int, docId: Int, label: String): Array[ResultItem] = ???
}

class HomeLuceneStateFactory() extends StateFactory {

  override def usingState[T](function: State => T): T = {
    using(new HomeLuceneState()) { state =>
      function(state)
    }
  }
}

object HomeLuceneStateFactory {

  def apply(config: Config): HomeLuceneStateFactory = {
    new HomeLuceneStateFactory()
  }
}