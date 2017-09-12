package hacker.news

import HackerNewsAPI._
import hacker.news.Utils.{Score, Username}

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
