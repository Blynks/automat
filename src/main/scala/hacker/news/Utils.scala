package hacker.news

import net.liftweb.json.JsonAST.JValue


object  Utils {
  // gives context
  type URL = String
  type StoryName = String
  type JSON = JValue

  // case classes for parsing JSON strings
  case class Item(by: Option[String], id: String, kids: List[String], title: Option[String])
  case class User(id: String, submitted: List[String])
}
