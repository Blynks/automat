package hacker.news

import hacker.news.Utils._
import net.liftweb.json._


object HackerNewsAPI {
  implicit val formats = DefaultFormats // brings in defaults for case classes
  /**
    * Gets the top N number of stories from Hacker News' API
    * Default of 30 top stories.
    *
    * @param numberOfStories the desired number of top stories (up to 500)
    * @return Array containing the IDs of N top stories
    */
  def getHackerNewsTopStoryIDs(numberOfStories: Int = 30): List[String] = {
    val topStoriesURL: URL = "https://hacker-news.firebaseio.com/v0/topstories.json"
    val apiResponse: JValue = parse(scala.io.Source.fromURL(topStoriesURL).mkString)
    val topStoryIDs = apiResponse.extract[List[String]]

    topStoryIDs.take(numberOfStories)
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
    getItemByID(id).by match {
      case Some(value: String) => value
      case _ => "item deleted"
    }
  }

  def getUserComments(userName: String): List[String] = {
    userName match {
      case null => List("No Comments")
      case _ =>
        val userURL: URL = s"https://hacker-news.firebaseio.com/v0/user/$userName.json"
        val apiResponse: JValue = parse(scala.io.Source.fromURL(userURL).mkString)
        val userProfile: User = apiResponse.extract[User]

        userProfile.submitted
    }

  }

  def getTopStoriesComments: List[String] = {
    val topNStoriesIDs = getHackerNewsTopStoryIDs()
    val listOfStoriesIDs = topNStoriesIDs.map(x => getItemByID(x).kids)

    listOfStoriesIDs.flatten
  }


}
