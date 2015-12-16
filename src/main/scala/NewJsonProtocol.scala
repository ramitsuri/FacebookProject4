package com.ramitsuri.project4


import spray.json.{RootJsonFormat, DefaultJsonProtocol}


object NewJsonProtocol extends DefaultJsonProtocol {
  implicit val wallPostFormat: RootJsonFormat[WallPost] = jsonFormat3(WallPost)
  implicit val userFormat: RootJsonFormat[User] = jsonFormat4(User)
  implicit val friendListFormat: RootJsonFormat[FriendList] = jsonFormat2(FriendList)
  implicit val profileFormat: RootJsonFormat[Profile] = jsonFormat3(Profile)
  implicit val pageFormat: RootJsonFormat[Page] = jsonFormat4(Page)
}