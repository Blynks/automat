package hacker.news

import hacker.news.Utils._
import net.liftweb.json._

import scala.collection.mutable.ListBuffer
import scala.collection.parallel.ParMap


object HackerNewsAPI {
  implicit val formats = DefaultFormats // brings in defaults for case classes

  /**
    * Gets the top N number of story IDs from Hacker News' API.
    * Default of 30 top stories.
    *
    * ToDo:
    * - Control for timeouts (didn't want to add more dependencies atm)
    * - Control if server is down (would be in the HttpClient)
    *
    * @param numberOfStories the desired number of top stories (up to 500)
    * @return Array containing the IDs of N top stories
    */
  def getHackerNewsTopStoryIDs(numberOfStories: Int = 30): List[String] = {
    val topStoriesURL: URL = "https://hacker-news.firebaseio.com/v0/topstories.json"
    val request: JSON = parse(scala.io.Source.fromURL(topStoriesURL).mkString)
    val topStoryIDs = request.extract[List[String]]

    topStoryIDs.take(numberOfStories)
  }

  /**
    * Get an Item based on their story ID.
    *
    * @param id the ID of the story
    * @return body of the story
    */
  def getItemByID(id: String): Item = {
    val itemURL: URL = s"https://hacker-news.firebaseio.com/v0/item/$id.json"
    val request: JSON = parse(scala.io.Source.fromURL(itemURL).mkString)

    request.extract[Item]
  }

  /**
    * Get the username of a User based on their ID.
    *
    * ID is found through an item (ex. Thread name, comment etc...)
    * "item deleted" if there is no user associated with an ID.
    * Empty if item was deleted or dead.
    *
    * @param id the ID of the user
    * @return the username associated with the ID
    */
  def getUserByItemID(id: String): String = {
    getItemByID(id).by match {
      case Some(userID: String) => userID
      case _ => "item deleted"
    }
  }

  /**
    * BFS traversal starting from a comment or thread.
    *
    * @param rootItem root node
    * @return list of comment ids and all their children ids
    */
  def getAllChildrenIDs(rootItem: Item): List[String] = {
    var commentAccumulator = new ListBuffer[String]() // should convert to a case class

    // keep traversing while an item has children
    def itemTraversal(thread: Item): Unit = {
      thread.kids match {
        case Nil =>
          commentAccumulator += thread.id

        case _ =>
          commentAccumulator += thread.id
          val childrenItems = thread.kids
            .par // parallelized api calls
            .map(itemID => getItemByID(itemID))
            .map(childItem => itemTraversal(childItem))
      }
    }
    itemTraversal(rootItem)
    commentAccumulator.toList
  }

  def getUserScoresByThread(thread: Item): ParMap[String, Int] = {
    val commentIDsByThread = getAllChildrenIDs(thread)
    val userScores = commentIDsByThread
      .par.map(storyID => getUserByItemID(storyID) -> 1)
      .filter(userPost => userPost._1 != "item deleted")
      .groupBy(userPost => userPost._1)
      .mapValues(_.map(_._2).sum) // sum by user name

    userScores
  }
}
