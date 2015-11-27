package com.ramitsuri.project4

import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, Props, ActorSystem, Actor}
import akka.util.Timeout
import spray.json.{RootJsonFormat, DefaultJsonProtocol}
import spray.routing._
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.{Await, Future}
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport._

/**
 * Created by ramit on 11/26/2015.
 */

object NewJsonProtocol extends DefaultJsonProtocol {
  implicit val wallPostFormat: RootJsonFormat[WallPost] = jsonFormat3(WallPost)
  implicit val userFormat: RootJsonFormat[User] = jsonFormat3(User)
  implicit val friendListFormat: RootJsonFormat[FriendList] = jsonFormat2(FriendList)
  implicit val profileFormat: RootJsonFormat[Profile] = jsonFormat3(Profile)
  implicit val pageFormat: RootJsonFormat[Page] = jsonFormat4(Page)
}


//Content classes
case class User(id: String, var name: String, var posts: Vector[WallPost])

case class WallPost(id: String, postedBy: String, var content: String)

case class FriendList(owner: String, var members: Vector[String])

case class Profile(id: String, var user: User, var friendList: FriendList)

case class Page(id: String, owner: String, name: String, posts: Vector[WallPost])

//Message classes for Master Actor
case class Start()

case class GetAllUsers()

case class AddUser(name: String)

case class DeleteUser(id: String)

case class GetNumberOfUsers()

case class AddPage()

case class DeletePage()

case class UpdatePage()

case class GetPage()

case class Test()


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

class UserActor(id: String, var name: String) extends Actor {
  var userInfo: User = new User(id, name, Vector[WallPost]())
  var friendList: FriendList = new FriendList(id, Vector[String]())
  var profile: Profile = new Profile(id, userInfo, friendList)


  def getUserInfo() = {
    userInfo
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
    userInfo.posts = userInfo.posts :+ wallPost
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


  }

  implicit def toUser(user: User): User = User(id = user.id, name = user.name, posts = user.posts)

  implicit def toPost(post: WallPost): WallPost = WallPost(id = post.id, postedBy = post.postedBy, content = post.content)

  implicit def toFriendList(friendList: FriendList): FriendList = FriendList(owner = friendList.owner, members = friendList.members)
}

class MasterActor extends Actor {

  var numberOfUsers = 10
  var usersIDs: Vector[String] = Vector[String]()
  var users: Vector[User] = Vector[User]()
  val userActorBasePath = "akka://FaceBookSystem/user/httpInterface/masterActor/user"

  def start() = {
    for (i <- 1 to numberOfUsers) {
      var user = context.actorOf(Props(new UserActor(i.toString, "name" + i)), name = "user" + i)
      usersIDs = usersIDs :+ i.toString
    }
  }

  def getAllUsers() = {
    for (i <- 1 to numberOfUsers) {
      val userActor = context.actorSelection(userActorBasePath + i)
      implicit val timeout = Timeout(Duration(10, TimeUnit.SECONDS))
      val future: User = Await.result(userActor ? GetUserDetails(), timeout.duration).asInstanceOf[User]
      users = users :+ future
    }
    users.toArray
  }

  def addUser(name: String) = {
    numberOfUsers = numberOfUsers + 1
    var user = context.actorOf(Props(new UserActor(numberOfUsers.toString, name)), name = "user" + numberOfUsers)
    usersIDs = usersIDs :+ numberOfUsers.toString
  }

  def deleteUser(id: String) = {
    numberOfUsers = numberOfUsers - 1
    usersIDs = usersIDs.filterNot(_ == id)
  }

  def getNumberOfUsers() = {
    numberOfUsers
  }

  def receive = {

    case Test() => {
      println(self.path)
    }

    case Start() => {
      start()
    }

    case GetAllUsers() => {
      sender ! getAllUsers()
    }

    case AddUser(name: String) => {
      addUser(name)
    }

    case DeleteUser(id: String) => {
      deleteUser(id)
    }

    case GetNumberOfUsers() => {
     sender ! getNumberOfUsers()
    }

    case AddPage() => {}

    case DeletePage() => {}

    case UpdatePage() => {}

    case GetPage() => {}


  }

  implicit def toPage(page: Page): Page = Page(id = page.id, owner = page.owner, name = page.name, posts = page.posts)
}


class FacebookServer extends HttpServiceActor with RestApi {
  def receive = runRoute(routes)
}

trait RestApi extends HttpService with ActorLogging {
  actor: Actor =>
  implicit val timeout = Timeout(10 seconds)
  //implicit val system = ActorSystem("FacebookSystem")

  val masterActor = context.actorOf(Props(new MasterActor()), name = "masterActor")
  masterActor ! Start()

  def routes: Route = pathPrefix("project4") {
    import NewJsonProtocol._
    //Get a user
    val userActorBasePath = "akka://FaceBookSystem/user/httpInterface/masterActor/user"
    val masterActorBasePath = "akka://FaceBookSystem/user/httpInterface/masterActor"

    path("users" / "getUser" / Segment) { userID =>
      get {
        respondWithMediaType(`application/json`) {
          complete {
            {
              val userActor = context.actorSelection(userActorBasePath + userID)
              implicit val timeout = Timeout(Duration(10, TimeUnit.SECONDS))
              val future: User = Await.result(userActor ? GetUserDetails(), timeout.duration).asInstanceOf[User]
              future
            }
          }
        }
      }
    } ~ //get all posts for a user
      path("users" / Segment / "posts" / "getPosts") { userID =>
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val userActor = context.actorSelection(userActorBasePath + userID)
              implicit val timeout = Timeout(Duration(10, TimeUnit.SECONDS))
              val future: Array[WallPost] = Await.result(userActor ? GetAllPosts(), timeout.duration).asInstanceOf[Array[WallPost]]
              future
            }
          }
        }
      } ~ //get single post for a user
      path("users" / Segment / "posts" / "getSinglePost" / Segment) { (userID, postID) => {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val userActor = context.actorSelection(userActorBasePath + userID)
              implicit val timeout = Timeout(Duration(10, TimeUnit.SECONDS))
              val future: WallPost = Await.result(userActor ? GetWallPost(postID), timeout.duration).asInstanceOf[WallPost]
              future
            }
          }
        }

      }
      } ~ //display a profile
      path("getProfile" / Segment) { (profileID) => {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val userActor = context.actorSelection(userActorBasePath + profileID)
              implicit val timeout = Timeout(Duration(10, TimeUnit.SECONDS))
              val future: Profile = Await.result(userActor ? GetProfile(), timeout.duration).asInstanceOf[Profile]
              future
            }
          }
        }
      }
      } ~ //read a friend list
      path("users" / Segment / "getFriendList") { (userID) => {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val userActor = context.actorSelection(userActorBasePath + userID)
              implicit val timeout = Timeout(Duration(10, TimeUnit.SECONDS))
              val future: FriendList = Await.result(userActor ? GetFriendList(), timeout.duration).asInstanceOf[FriendList]
              future
            }
          }
        }
      }
      } ~ //Add a wall post
      path("users" / Segment / "posts" / "post") { (userID) => {
        post {
          respondWithMediaType(`text/plain`) {
            entity(as[WallPost]) { wallPost =>
              complete {
                val userActor = context.actorSelection(userActorBasePath + userID)
                userActor ! AddWallPost(wallPost)
                "OK"
              }
            }
          }
        }
      }
      } ~ //Add a friend
      path("users" / Segment / "addFriend") { (userID) => {
        post {
          respondWithMediaType(`text/plain`) {
            entity(as[String]) { friendToAdd =>
              complete {
                val userActor = context.actorSelection(userActorBasePath + userID)
                userActor ! AddFriend(friendToAdd)
                "OK"
              }
            }
          }
        }
      }
      } ~ //delete a wall post
      path("users" / Segment / "posts" / "delete") { (userID) => {
        delete {
          respondWithMediaType(`text/plain`) {
            entity(as[String]) { wallPostID =>
              complete {
                val userActor = context.actorSelection(userActorBasePath + userID)
                userActor ! RemoveWallPost(wallPostID)
                "OK"
              }
            }
          }
        }
      }
      } ~ //remove a friend
      path("users" / Segment / "friends" / "delete") { (userID) => {
        delete {
          respondWithMediaType(`text/plain`) {
            entity(as[String]) { friendID =>
              complete {
                val userActor = context.actorSelection(userActorBasePath + userID)
                userActor ! RemoveFriend(friendID)
                "OK"
              }
            }
          }
        }
      }
      } ~ // edit a profile
      path("users" / Segment / "editProfile") { (userID) => {
        put {
          respondWithMediaType(`text/plain`) {
            entity(as[User]) { user =>
              complete {
                val userActor = context.actorSelection(userActorBasePath + userID)
                userActor ! EditUserInfo(user)
                "OK"
              }
            }
          }
        }
      }
      } ~ //edit a post for a user
      path("users" / Segment / "posts" / "editPost") { (userID) => {
        put {
          respondWithMediaType(`text/plain`) {
            entity(as[WallPost]) { wallPost: WallPost =>
              complete {
                val userActor = context.actorSelection(userActorBasePath + userID)
                userActor ! EditPostInfo(wallPost)
                "OK"
              }
            }
          }
        }
      }
      } ~ //get all users
      path("users" / "getAllUsers") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              {
                val masterActor = context.actorSelection(masterActorBasePath)
                implicit val timeout = Timeout(Duration(10, TimeUnit.SECONDS))
                val future: Array[User] = Await.result(masterActor ? GetAllUsers(), timeout.duration).asInstanceOf[Array[User]]
                future
              }
            }
          }
        }
      }~ //add a user
      path("users" / "addUSer") {
        post {
          respondWithMediaType(`text/plain`) {
            entity(as[String]) { name =>
            complete {
              {
                val masterActor = context.actorSelection(masterActorBasePath)
                masterActor ! AddUser(name)
                "OK"
              }
            }}
          }
        }
      }~ //delete a user
      path("users" / "deleteUser") {
        delete {
          respondWithMediaType(`text/plain`) {
            entity(as[String]) { idToDelete =>
            complete {
              {
                val masterActor = context.actorSelection(masterActorBasePath)
                masterActor ! DeleteUser(idToDelete)
                "OK"
              }
            }
            }
          }
        }
      }~ //get number of users
      path("users" / "getNumberOfUsers") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              {
                val masterActor = context.actorSelection(masterActorBasePath)
                implicit val timeout = Timeout(Duration(10, TimeUnit.SECONDS))
                val future: Int = Await.result(masterActor ? GetNumberOfUsers(), timeout.duration).asInstanceOf[Int]
                future.toString
              }
            }
          }
        }
      }

  }

}

