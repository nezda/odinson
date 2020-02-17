package ai.lum.odinson

import java.util.{ Collection, Map => JMap }
import scala.collection.JavaConverters._
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import ai.lum.odinson.compiler.QueryCompiler
import ai.lum.odinson.lucene.search.OdinsonQuery
import ai.lum.odinson.utils.VariableSubstitutor

case class Rule(
  name: String,
  ruletype: String,
  pattern: String,
)

case class Extractor(
  name: String,
  // label
  // priority
  query: OdinsonQuery,
)

case class RuleFile(rules: Seq[Rule], variables: Map[String, String])

case class Mention(
  odinsonMatch: OdinsonMatch,
  // label
  foundBy: String,
  docID: String,
  sentenceID: Int,
)

class RuleReader(val compiler: QueryCompiler) {

  def compileRuleFile(input: String, userVariables: Map[String, String] = Map.empty): Seq[Extractor] = {
    val f = parseRuleFile(input)
    val variables = f.variables ++ userVariables
    mkExtractors(f.rules, variables)
  }

  def parseRuleFile(input: String): RuleFile = {
    val yaml = new Yaml(new Constructor(classOf[JMap[String, Any]]))
    val master = yaml.load(input).asInstanceOf[JMap[String, Any]].asScala.toMap
    val variables = mkVariables(master)
    val rules = mkRules(master)
    RuleFile(rules, variables)
  }

  def mkExtractors(f: RuleFile): Seq[Extractor] = mkExtractors(f.rules, f.variables)

  def mkExtractors(rules: Seq[Rule], variables: Map[String, String] = Map.empty): Seq[Extractor] = {
    val varsub = new VariableSubstitutor(variables)
    rules.map(r => mkExtractor(r, varsub))
  }

  private def mkExtractor(rule: Rule, varsub: VariableSubstitutor): Extractor = {
    val ruletype = varsub(rule.ruletype)
    val query = ruletype match {
      case "simple" =>
        compiler.compile(varsub(rule.pattern))
      case "triggered" =>
        compiler.compileEventQuery(varsub(rule.pattern))
      case _ =>
        sys.error(s"invalid rule type '$ruletype'")
      }
    Extractor(rule.name, query)
  }

  private def mkVariables(data: Map[String, Any]): Map[String, String] = {
    data.get("vars").map(parseVariables).getOrElse(Map.empty)
  }

  private def parseVariables(data: Any): Map[String, String] = {
    data match {
      case vars: JMap[_, _] =>
        vars
          .asScala
          .map { case (k, v) => k.toString -> v.toString }
          .toMap
      case _ => sys.error("invalid variables data")
    }
  }

  private def mkRules(data: Any): Seq[Rule] = {
    data match {
      case data: JMap[_, _] =>
        data.get("rules") match {
          case None => Seq.empty
          case Some(rules: Collection[_]) =>
            rules.asScala.map(mkRule).toSeq
          case _ => sys.error("invalid rules data")
        }
      case _ => sys.error("invalid rules data")
    }
  }

  private def mkRule(data: Any): Rule = {
    data match {
      case data: JMap[_, _] =>
        val fields = data.asInstanceOf[JMap[String, Any]].asScala.toMap
        def getField(name: String) = fields.get(name).getOrElse(sys.error(s"'$name' is required")).toString()
        val name = getField("name")
        val ruletype = getField("type")
        val pattern = getField("pattern")
        Rule(name, ruletype, pattern)
      case _ => sys.error("invalid rule data")
    }
  }

}
