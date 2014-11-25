import
  java.{ io, net },
    io.{ File, PrintWriter },
    net.URL

import
  scala.io.Source.{ fromFile, fromURL }

import
  argonaut.{ Argonaut, Json, Parse },
    Argonaut.{ casecodec1, ListDecodeJson, ToJsonIdentity }

object MyApp extends App {

  val StatusOracleURL = new URL("http://localhost:9000/model/statuses.json")
  val GoodsFile       = new File("goods.json")
  val BadsFile        = new File("bads.json")
  val StatusKey       = "status"
  val GoodStatus      = "compiling"

  val (newGoods, newBads) = findGoodsAndBads()
  val (oldGoods, oldBads) = findOldGoodsAndBads()

  printDiffs(newGoods, newBads, oldGoods, oldBads)

  modernize(newGoods, newBads)

  private def findGoodsAndBads(): (Set[String], Set[String]) = {

    case class Status(status: String)
    implicit def StatusCodecJson = casecodec1(Status.apply, Status.unapply)(StatusKey)

    val source  = fromURL(StatusOracleURL)
    val jsonStr = source.mkString
    source.close()

    val json             = Parse.parse(jsonStr).fold(_ => Json(), identity)
    val modelToStatusMap = json.assoc.getOrElse(Nil).toMap.mapValues(_.as[Status].map(_.status).getOr("") == GoodStatus)
    val statusToModelMap = modelToStatusMap.toSeq.groupBy(_._2).mapValues(_.map(_._1))

    (statusToModelMap(true).toSet, statusToModelMap(false).toSet)

  }

  private def findOldGoodsAndBads(): (Set[String], Set[String]) = {
    def slurpSetFrom(file: File): Set[String] = {
      val jsonStr =
        try {
          val source = fromFile(file)
          val str    = source.mkString
          source.close()
          str
        }
        catch {
          case ex: Exception => "{}"
        }
      Parse.parse(jsonStr).fold(_ => Json(), identity).as[List[String]].getOr(Nil).toSet
    }
    (slurpSetFrom(GoodsFile), slurpSetFrom(BadsFile))
  }

  private def printDiffs(newGoods: Set[String], newBads: Set[String], oldGoods: Set[String], oldBads: Set[String]): Unit = {
    val printUpdate =
      (label: String) => (news: Set[String]) => (olds: Set[String]) =>
        println(s"$label:\n${news.diff(olds).toList.sorted.mkString("\n")}")
    printUpdate("Newly working models")(newGoods)(oldGoods)
    println()
    printUpdate("Newly broken models")(newBads)(oldBads)
  }

  private def modernize(newGoods: Set[String], newBads: Set[String]): Unit = {
    val f = (set: Set[String]) => (pw: PrintWriter) => pw.println(set.toList.asJson)
    printToFile(GoodsFile)(f(newGoods))
    printToFile(BadsFile)(f(newBads))
  }

  private def printToFile(f: File)(op: (PrintWriter) => Unit): Unit = {
    val p = new PrintWriter(f)
    try op(p) finally p.close()
  }

}
