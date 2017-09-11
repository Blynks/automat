package hacker.news

import HackerNewsAPI._
import net.liftweb.json._
import hacker.news.Utils._

import scala.collection.parallel.ParMap

/**
  * Using the Hacker News API Documentation (https://github.com/HackerNews/API)
  * write a program that will print the title of the top 30 hacker news stories
  * and the top 10 commenter names of these stories with the total number of
  * comments that they posted (only for these 30 stories).
  *
  * The program has to parallelize requests and aggregate the results as efficiently as possible.
  */

object Main extends App {
  implicit val formats = DefaultFormats
  // deal with errors and timeout
  // add parallelism**
  // control for timeout
  // dockerize
  // unit testing
  // use Futures for concurrency
  val storyIDs = getHackerNewsTopStoryIDs() // default top 30 stories

  val topCommenters: ParMap[String, Int] = storyIDs
    .par
    .map(y => getItemByID(y))
    .flatMap(x => getAllChildrenIDs(x))
    .map(n => getUserByItemID(n) -> 1)
    .groupBy(k => k._1)
    .mapValues(_.map(_._2).sum)

  val sortedTopCommenters = topCommenters.toMap

  for ((storyID, storyCount) <- storyIDs.zipWithIndex) {
    print(s"Story ${storyCount + 1}: ")
    val storyItem = getItemByID(storyID)

    storyItem.title match {
      case Some(value: String) => println(value)
      case _ => println("No Title")
    }

    val allCommentsForThread = getAllChildrenIDs(storyItem)
    val allUsersAndScores: Map[String, Int] = allCommentsForThread
      .map(x => getUserByItemID(x) -> 1)
      .groupBy(k => k._1)
      .mapValues(_.map(_._2).sum)

    val seq: Seq[(String, Int)] = allUsersAndScores.toSeq.filterNot(x => x._1 == "item deleted")
    val sortedSeq = seq.sortWith(_._2 > _._2).take(10)
    for (x <- sortedSeq) {
      val totalScore = sortedTopCommenters.getOrElse(x._1, 1)
      println(s"${x._1} (thread score: ${x._2}, total score: $totalScore)")
    }
    println
  }
}
