package ai.lum.odinson.state

import ai.lum.odinson.BaseSpec
import ai.lum.odinson.ExtractorEngine

class TestAwayState extends BaseSpec {

  behavior of "AwayLuceneStateFactory"

  it should "load the state" in {
    val config = ExtractorEngine.defaultConfig
    val stateFactory = AwayLuceneStateFactory(config)

    stateFactory.usingState { state =>
      state.getDocIds(0, "label")
    }
  }
}
