package com.ramitsuri.project4


case class User(id: String, var name: String, var posts: Vector[WallPost])

case class WallPost(id: String, postedBy: String, var content: String)

case class FriendList(owner: String, var members: Vector[String])

case class Profile(id: String, var user: User, var friendList: FriendList)

case class Page(id: String, var owner: String, var name: String, var posts: Vector[WallPost])
