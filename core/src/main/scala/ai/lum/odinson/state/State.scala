package ai.lum.odinson.state

import scala.collection.mutable.ArrayBuffer
import org.h2.jdbcx.JdbcDataSource

class State {

  private val dataSource = new JdbcDataSource()
  dataSource.setURL("jdbc:h2:mem:odinson")
  private val connection = dataSource.getConnection()
  private val statement = connection.createStatement()

  def close(): Unit = {
    statement.close()
    connection.close()
  }

  def init(): Unit = {
    val sql = """CREATE TABLE mentions (
                |  id BIGINT AUTO_INCREMENT,
                |  label VARCHAR(50) NOT NULL,
                |  luceneDocId INT NOT NULL,
                |  startToken INT NOT NULL,
                |  endToken INT NOT NULL,
                |);""".stripMargin
    statement.executeUpdate(sql)
  }

  def addMention(
    label: String,
    luceneDocId: Int,
    startToken: Int,
    endToken: Int
  ): Unit = {
    val sql = s"""INSERT INTO mentions (
                 |  label,
                 |  luceneDocId,
                 |  startToken,
                 |  endToken,
                 |) VALUES (
                 |  '$label',
                 |  $luceneDocId,
                 |  $startToken,
                 |  $endToken,
                 |);""".stripMargin
    statement.executeUpdate(sql)
  }

  def getMatches(label: String, luceneDocId: Int): Array[(Int, Int)] = {
    val sql = s"""SELECT *
                 |FROM mentions
                 |WHERE label='$label' AND luceneDocId=$luceneDocId
                 |ORDER BY startToken, endToken
                 |;""".stripMargin
    val results = statement.executeQuery(sql)
    val matches = ArrayBuffer.empty[(Int, Int)]
    while (results.next()) {
      val start = results.getInt("startToken")
      val end = results.getInt("endToken")
      matches += Tuple2(start, end)
    }
    matches.toArray
  }

  def getDocIds(label: String): Array[Int] = {
    val sql = s"""SELECT DISTINCT luceneDocId
                 |FROM mentions
                 |WHERE label='$label'
                 |ORDER BY luceneDocId
                 |;""".stripMargin
    val results = statement.executeQuery(sql)
    val docIds = ArrayBuffer.empty[Int]
    while (results.next()) {
      docIds += results.getInt("luceneDocId")
    }
    docIds.toArray
  }

}
