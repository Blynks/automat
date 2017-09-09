package hacker.news

import HackerNewsAPI._
import net.liftweb.json._
import hacker.news.Utils._

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

  // get the IDs of the top 30 stories
  val storyIDs = getHackerNewsTopStoryIDs()

//   for each of these stories, find the top 10 commenters
  for (x <- storyIDs) {
    getItemByID(x).title match {
      case Some(value: String) => println(value)
      case _ => println("No Title")
    }


  }


}
