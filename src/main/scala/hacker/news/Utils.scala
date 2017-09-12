package hacker.news

import net.liftweb.json.JsonAST.JValue


object  Utils {
  // for context
  type URL = String
  type JSON = JValue
  type StoryName = String
  type StoryID = String
  type Username = String
  type UserID = String
  type Score = Int

  // case classes for parsing JSON strings
  case class Item(by: Option[Username], id: String, kids: List[UserID], title: Option[String])
  case class User(id: UserID, submitted: List[StoryID])
}
