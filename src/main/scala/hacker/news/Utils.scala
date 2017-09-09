package hacker.news


object  Utils {
  // gives context
  type URL = String
  type StoryName = String
  type User = String

  // case classes
  case class Item(by: String, id: String, kids: List[String], title: Option[String])
  case class HNUser(id: String, about: String, submitted: List[String])

}
