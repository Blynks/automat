package hacker.news

import hacker.news.Utils._
import net.liftweb.json._

import scala.collection.mutable.ListBuffer


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
  def getHackerNewsTopStoryIDs(numberOfStories: Int = 30): List[StoryID] = {
    val topStoriesURL: URL = "https://hacker-news.firebaseio.com/v0/topstories.json"
    val request: JSON = parse(scala.io.Source.fromURL(topStoriesURL).mkString)
    val topStoryIDs = request.extract[List[StoryID]]

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
  def getUserByItemID(id: String): Username = {
    getItemByID(id).by match {
      case Some(userName: Username) => userName
      case _ => "item deleted"
    }
  }

  /**
    * Get the title of the story or thread.
    *
    * Comments, dead or deleted items do not have titles.
    *
    * @param storyItem valid item (story, comment etc...)
    * @return title of the story
    */
  def getItemTitle(storyItem: Item): StoryName = {
    storyItem.title match {
      case Some(title: StoryName) => title
      case _ => "No Title"
    }
  }

  /**
    * BFS traversal starting from a comment or thread.
    *
    * @param rootItem root node
    * @return list of comment ids and all their children ids
    */
  def getAllChildrenIDs(rootItem: Item): List[StoryID] = {
    var commentAccumulator = new ListBuffer[StoryID]() // should convert to a case class

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

  /**
    * Get the scores for each user in the story/thread.
    * Format: (username, score)
    *
    * ex (hackerdude123, 10)
    *
    * @param thread story to calculate score
    * @return map of user and score
    */
  def getUserScoresByThread(thread: Item): Map[Username, Score] = {
    val commentIDsByThread = getAllChildrenIDs(thread)
    val userScores = commentIDsByThread
      .map(storyID => getUserByItemID(storyID) -> 1) // give a score of 1 per post
      .filter(userPost => userPost._1 != "item deleted") // remove dead or deleted items
      .groupBy(userPost => userPost._1)
      .mapValues(_.map(_._2).sum) // sum by user name

    userScores
  }

  /**
    * Get the top N comments on a thread.
    * Default value of 10.
    *
    * @param userScores list of unsorted user scores
    * @param numberOfUsers desired number of top commenters
    * @return map of the top N commenters for a story
    */
  def getTopUserScores(userScores: Map[Username, Score], numberOfUsers: Int = 10): Map[String, Int] = {
    val sortedUserScores = userScores.toSeq.sortWith(_._2 > _._2)
    sortedUserScores.take(numberOfUsers).toMap
  }

  /**
    * Gets the author of an item if the item wasn't deleted or is dead.
    *
    * @param id item id
    * @return the author of the item
    */
  def getUserNameByItemID(id: StoryID): Username = getItemByID(id).by match {
    case Some(value: Username) => value
    case _ => "item deleted"
  }

  /**
    * Gets the set of comment IDs submitted by a particular user.
    *
    * @param userName the username or author
    * @return set of comments IDs from a particular user
    */
  def getUserSubmittedCommentIDs(userName: Username): Set[StoryID] = userName match {
    case null => Set("No Comments")
    case _ =>
      val userURL: URL = s"https://hacker-news.firebaseio.com/v0/user/$userName.json"
      val request: JSON = parse(scala.io.Source.fromURL(userURL).mkString)
      val userProfile: User = request.extract[User]
      userProfile.submitted.toSet
  }

  /**
    * Gets the score of the user by finding the intersection between submitted items and items in the thread.
    *
    * @param userName user of interest
    * @param threadCommentIDs the set of item IDs you want the user to be scored with
    * @return user's score for a particular set of comments
    */
  def getScore(userName: Username, threadCommentIDs: Set[StoryID]): Score =
    getUserSubmittedCommentIDs(userName).intersect(threadCommentIDs).size

  /**
    * Gets the top N number of users, scored by the number of posts.
    *
    * Default of top 10 commenters.
    *
    * @param commenterScores list of users and their scores
    * @param numberOfUsers the top number of users with the highest number of comments
    * @return top commenters of the list
    */
  def getSetOfTopUserScores(commenterScores: List[(Username, Score)],
                            numberOfUsers: Int = 10): List[(Username, Score)] = {
    commenterScores.sortWith(_._2 > _._2).take(numberOfUsers)
  }
}
