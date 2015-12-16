package com.ramitsuri.project4
import scala.collection.mutable
import java.security.PublicKey
import javax.xml.crypto.dsig.keyinfo.KeyValue


case class User(id: String, var name: String, var posts: Vector[WallPost], var keys: Vector[String])

case class WallPost(id: String, postedBy: String, var content: String, var sharedWith: Vector[String])

case class FriendList(owner: String, var members: Vector[String])

case class Profile(id: String, var user: User, var friendList: FriendList)

case class Page(id: String, var owner: String, var name: String, var posts: Vector[WallPost])

case class Keys(var privateAESforName: String, var privateAESforPost: String)