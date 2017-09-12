package hacker.news

import HackerNewsAPI._
import hacker.news.Utils.{Score, StoryID, Username}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Using the Hacker News API Documentation (https://github.com/HackerNews/API)
  * write a program that will print the title of the top 30 hacker news stories
  * and the top 10 commenter names of these stories with the total number of
  * comments that they posted (only for these 30 stories).
  *
  * The program has to parallelize requests and aggregate the results as efficiently as possible.
  *
  * ToDo:
  * - Create HTTP client to control for server downtime and timeouts
  * - Mock API and create unit tests
  * - Dockerize deployment
  * - Wrap API calls in Futures for concurrency
  *
  */

object Main extends App {
  // use Futures for concurrency
  val topStoryIDs = getHackerNewsTopStoryIDs() // default top 30 stories
  val topStoryItems = topStoryIDs.map(storyID => getItemByID(storyID))

  /**
    * This is the first solution I thought about. Convert each comment to a tuple of (username, 1)
    * then aggregate by username. Solution was slow even when parallelized.
    *
    * Potentially faster if API calls were wrapped into Futures (ToDo).
    *
    */
  def firstSolution(): Unit = {
    // acts as a cache for user scores for top stories
    val userScoresForTopStories = topStoryItems.flatMap(storyItem => getUserScoresByThread(storyItem)).toMap

    for ((storyItem, storyNumber) <- topStoryItems.zipWithIndex) {
      print(s"Story ${storyNumber + 1}: ")
      val storyTitle = getItemTitle(storyItem)
      println(storyTitle)

      val threadScores = getUserScoresByThread(storyItem)
      val topCommenters: Map[Username, Score] = getTopUserScores(threadScores)

      for (topCommenter <- topCommenters) {
        val (userName, userStoryScore) = (topCommenter._1, topCommenter._2)
        val usersTotalScore = userScoresForTopStories.getOrElse(topCommenter._1, userStoryScore)
        println(s"$userName (thread score: $userStoryScore, total score: $usersTotalScore)")
      }
      println
    }
  }

  /**
    * This second solution is much faster. Get comment IDs from each thread and find the intersection with each user's
    * submitted comments. Algorithmically less efficient but has fewer API calls.
    *
    */
  def secondSolution(): Unit = {
    // need to block here for accurate results
    val commentIDsForTopStories: Set[StoryID] = Await.result({
      Future { topStoryItems.flatMap(storyItem => getAllChildrenIDs(storyItem)).toSet }
      }, 2 minutes)

    for ((storyItem, storyNumber) <- topStoryItems.zipWithIndex) {
      print(s"Story ${storyNumber + 1}: ")
      val storyTitle = getItemTitle(storyItem)
      println(storyTitle)

      // users who posted in the top stories
      val topStoryUsers = getAllChildrenIDs(storyItem)
        .map(x => getUserNameByItemID(x))
        .filter(username => username != "item deleted")

      val commentIDsForThisStory = getAllChildrenIDs(storyItem).toSet

      val commenterScores: List[(Username, Score)] = topStoryUsers
        .map(user => (user, getScore(user, commentIDsForThisStory)))

      val topCommentersWithThreadScores = getSetOfTopUserScores(commenterScores)
      val topCommentersWithAllScores = for ((user, score) <- topCommentersWithThreadScores)
        yield (user, score, getScore(user, commentIDsForTopStories))

      topCommentersWithAllScores.foreach(x => println(s"${x._1} (thread score: ${x._2}, total score: ${x._3})"))
      println
    }
  }

  // only run second solution
  secondSolution()
}
