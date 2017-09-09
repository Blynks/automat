package hacker.news

import hacker.news.Utils._
import net.liftweb.json._


object HackerNewsAPI {
  implicit val formats = DefaultFormats // brings in defaults for case classes
  /**
    * Gets the top N number of stories from Hacker News' API
    *
    * @param numberOfStories the desired number of top stories (up to 500)
    * @return Array containing the IDs of N top stories
    */
  def getHackerNewsTopStoryIDs(numberOfStories: Int = 30): Array[String] = {
    val topStoriesURL: URL = "https://hacker-news.firebaseio.com/v0/topstories.json"
    val apiResponse: JValue = parse(scala.io.Source.fromURL(topStoriesURL).mkString)
    val listOfTopStoryIDs = apiResponse.extract[Array[String]] // should use case class

    listOfTopStoryIDs.slice(0, numberOfStories-1)
  }

  /**
    * Get an Item based on their story ID
    *
    * @param id the ID of the story
    * @return body of the story
    */
  def getItemByID(id: String): Item = {
    val itemURL: URL = s"https://hacker-news.firebaseio.com/v0/item/$id.json"
    val apiResponse: JValue = parse(scala.io.Source.fromURL(itemURL).mkString)

    apiResponse.extract[Item]
  }

  def getUserByItemId(id: String): String = {
    val itemURL: URL = s"https://hacker-news.firebaseio.com/v0/item/$id.json"
    val apiResponse: JValue = parse(scala.io.Source.fromURL(itemURL).mkString)

    apiResponse.extract[Item].by
  }

}
