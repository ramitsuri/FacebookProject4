package com.ramitsuri.project4

import java.security.PublicKey

import akka.actor.Actor
import scala.collection.mutable


//Message classes for User Actor
case class GetUserDetails()

case class EditUserInfo(userToEdit: User)

case class EditPostInfo(postToEdit: WallPost)

case class AddWallPost(postToAdd: WallPost)

case class RemoveWallPost(postToRemove: String)

case class GetWallPost(wallPostID: String)

case class GetAllPosts()

case class AddFriend(friendToAdd: String)

case class RemoveFriend(friendToRemove: String)

case class GetFriendList()

case class GetProfile()

case class Message1()

case class Message2()

case class UpdateAESKeys(keys:Vector[String])

case class Check()

class UserActor(id: String, var name: String, var keys1: Vector[String]) extends Actor {
  var keys = keys1
  var userInfo: User = new User(id, name, Vector[WallPost](), keys)
  var friendList: FriendList = new FriendList(id, Vector[String]())
  var profile: Profile = new Profile(id, userInfo, friendList)
  var postsCount = getPostsCount()

  /*var aesKeyForPost: String = keys.privateAESforPost
  var aesKeyForName: String = keys.privateAESforName*/

  def getUserInfo() = {
    userInfo
  }

  def getPostsCount() = {
    userInfo.posts.length
  }
  def updatePostCount() = {
    postsCount = postsCount + 1
  }
  def editUserInfo(user: User) = {
    userInfo.name = user.name
    userInfo.posts = user.posts
    updateProfile()
  }

  def editWallPost(wallPost: WallPost) = {
    userInfo.posts = userInfo.posts.filterNot(_.id == wallPost.id)
    userInfo.posts = userInfo.posts :+ wallPost
    updateProfile()
  }

  def addWallPost(wallPost: WallPost) = {
    userInfo.posts = userInfo.posts :+ new WallPost((postsCount+1).toString, postedBy = wallPost.postedBy, content = wallPost.content, sharedWith = wallPost.sharedWith)
    updatePostCount()
    updateProfile()
  }

  def removeWallPost(wallPost: String) = {
    userInfo.posts = userInfo.posts.filterNot(_.id == wallPost)
    updateProfile()
  }

  def getWallPost(wallpostID: String) = {
    userInfo.posts.find(_.id == wallpostID).map(toPost)
  }

  def getAllWallPosts() = {
    userInfo.posts.toArray
  }

  def addFriend(friendToAdd: String) = {
    friendList.members = friendList.members :+ friendToAdd
    updateProfile()
  }

  def removeFriend(friendToRemove: String) = {
    friendList.members = friendList.members.filterNot(_ == friendToRemove)
    updateProfile()
  }

  def getFriendList() = {
    friendList
  }

  def updateProfile() = {
    profile.user = userInfo
    profile.friendList = friendList
  }

  def getProfile() = {
    profile
  }

  def updateAESKeys(keys: Vector[String]) ={
    this.keys = keys
  }


  def receive = {
    case GetUserDetails() => {
      sender ! userInfo
    }

    case EditUserInfo(userToEdit) => {
      editUserInfo(userToEdit)
    }

    case EditPostInfo(postToEdit) => {
      editWallPost(postToEdit)
    }

    case AddWallPost(postToAdd) => {
      addWallPost(postToAdd)
    }

    case RemoveWallPost(postToRemove) => {
      removeWallPost(postToRemove)
    }
    case GetWallPost(wallPostID) => {
      sender ! getWallPost(wallPostID)
    }
    case GetAllPosts() => {
      sender ! getAllWallPosts()
    }
    case AddFriend(friendToAdd) => {
      addFriend(friendToAdd)
    }
    case RemoveFriend(friendToRemove) => {
      removeFriend(friendToRemove)
    }
    case GetFriendList() => {
      sender ! getFriendList()
    }
    case GetProfile() => {
      sender ! profile
    }

    case Message1() => {
      sender ! Message2()
    }

    case Message2() => {

    }

    case UpdateAESKeys(keys) => {
      updateAESKeys(keys)
    }

    case Check() => {
      println("working")
    }

  }

  implicit def toUser(user: User): User = User(id = user.id, name = user.name, posts = user.posts, keys = user.keys)

  implicit def toPost(post: WallPost): WallPost = WallPost(id = post.id, postedBy = post.postedBy, content = post.content, sharedWith = post.sharedWith)

  implicit def toFriendList(friendList: FriendList): FriendList = FriendList(owner = friendList.owner, members = friendList.members)
}
