package hacker.news

import HackerNewsAPI._
import net.liftweb.json._
import hacker.news.Utils._

import scala.collection.immutable.ListMap

/**
  *
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
  // if comments are less than 10?
  // control for timeout
  // recusively search all children items in a thread
  // get the IDs of the top 30 stories
  val storyIDs = getHackerNewsTopStoryIDs()

//   for each of these stories, find the top 10 commenters
//  for ((x, count)<- storyIDs.zipWithIndex) {
//    print(s"Story ${count + 1}: ")
//    val item = getItemByID(x)
//
//    item.title match {
//      case Some(value: String) => println(value)
//      case _ => println("No Title")
//    }
//
//    val topStoryComments = getTopStoriesComments.toSet
//    // ONLY CHECKS FIRST 10 FIX THIS
//    for (y <- getItemByID(x).kids.take(10)) {
//      val userName = getUserByItemId(y)
//
//      userName match {
//        case "item deleted" => println("deleted comment")
//        case _ =>
//          val userSubmitted = getUserComments(userName).toSet
//          val itemKids = item.kids.toSet
//
//
//          val numberOfPostsInThisThread = userSubmitted.intersect(itemKids).size
//          val numberOfPostsInTopThreads = userSubmitted.intersect(topStoryComments).size
//
//          println(s"$userName (thread score: $numberOfPostsInThisThread, total score: $numberOfPostsInTopThreads)")
//      }
//    }
//    println()
//  }
  val testItem = getItemByID("15208138")
  val testItem2 = getItemByID("15214602")


//  commentTraversal(testItem2)
//  getAllThreadIDs(testItem2).foreach(println)

  val topCommenters = storyIDs
    .par
    .map(y => getItemByID(y))
    .flatMap(x => getAllThreadIDs(x))
    .map(n => getUserByItemId(n) -> 1)
    .groupBy(k => k._1)
    .mapValues(_.map(_._2).sum)

  val sortedTopCommenters = topCommenters.toMap


//  topCommenters.foreach(x => println(x))
//  println(sortedTopCommenters)
//  sortedTopCommenters.keys.foreach(println)
//  println(sortedTopCommenters("pmoriarty"))

  for ((x, count)<- storyIDs.zipWithIndex) {
    print(s"Story ${count + 1}: ")
    val item = getItemByID(x)

    item.title match {
      case Some(value: String) => println(value)
      case _ => println("No Title")
    }

    val allCommentsForThread = getAllThreadIDs(item)
    val allUsersAndScores: Map[String, Int] = allCommentsForThread
      .map(x => getUserByItemId(x) -> 1)
      .groupBy(k => k._1)
      .mapValues(_.map(_._2).sum)

    val seq = allUsersAndScores.toSeq
    val sortedSeq = seq.sortWith(_._2 > _._2).take(10)
    for (x <- sortedSeq) {
      val totalScore = sortedTopCommenters(x._1)
      println(s"${x._1} (thread score: ${x._2}, total score: $totalScore)")
    }
    println
  }
}
