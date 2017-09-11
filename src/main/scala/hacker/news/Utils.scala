package hacker.news


object  Utils {
  // gives context
  type URL = String
  type StoryName = String

  // case classes
  case class Item(by: Option[String], id: String, kids: List[String], title: Option[String])
  case class User(id: String, submitted: List[String])


  // helper function


}
