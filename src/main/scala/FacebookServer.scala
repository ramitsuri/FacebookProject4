package com.ramitsuri.project4

import java.security.PublicKey
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

class FacebookServer extends HttpServiceActor with RestApi {
  def receive = runRoute(routes)
}

trait RestApi extends HttpService with ActorLogging {
  actor: Actor =>
  implicit val timeout = Timeout(10 seconds)

  val masterActor = context.actorOf(Props(new MasterActor(10000, 1000)), name = "masterActor")
  masterActor ! Start()

  val statisticsActor = context.actorOf((Props(new StatisticsActor)), name = "statisticsActor")

  def routes: Route = pathPrefix("project4") {
    import NewJsonProtocol._

    val userActorBasePath = "akka://FaceBookSystem/user/httpInterface/masterActor/user"
    val pageActorBasePath = "akka://FaceBookSystem/user/httpInterface/masterActor/page"
    val masterActorBasePath = "akka://FaceBookSystem/user/httpInterface/masterActor"

    path("users" / "getUser" / Segment) { userID =>
      get {
        statisticsActor ! RequestStatistics()
        respondWithMediaType(`application/json`) {
          complete {
            {
              val userActor = context.actorSelection(userActorBasePath + userID)
              val future: User = Await.result(userActor ? GetUserDetails(), timeout.duration).asInstanceOf[User]
              future
            }
          }
        }
      }
    } ~ //get all posts for a user
      path("users" / Segment / "posts" / "getPosts") { userID =>
        get {
          statisticsActor ! RequestStatistics()
          respondWithMediaType(`application/json`) {
            complete {
              val userActor = context.actorSelection(userActorBasePath + userID)
              val future: Array[WallPost] = Await.result(userActor ? GetAllPosts(), timeout.duration).asInstanceOf[Array[WallPost]]
              future
            }
          }
        }
      } ~ //get single post for a user
      path("users" / Segment / "posts" / "getSinglePost" / Segment) { (userID, postID) => {
        get {
          statisticsActor ! RequestStatistics()
          respondWithMediaType(`application/json`) {
            complete {
              val userActor = context.actorSelection(userActorBasePath + userID)
              val future: WallPost = Await.result(userActor ? GetWallPost(postID), timeout.duration).asInstanceOf[WallPost]
              future
            }
          }
        }

      }
      } ~ //display a profile
      path("users" / "getProfile" / Segment) { (profileID) => {
        get {
          statisticsActor ! RequestStatistics()
          respondWithMediaType(`application/json`) {
            complete {
              val userActor = context.actorSelection(userActorBasePath + profileID)
              val future: Profile = Await.result(userActor ? GetProfile(), timeout.duration).asInstanceOf[Profile]
              future
            }
          }
        }
      }
      } ~ //read a friend list
      path("users" / Segment / "getFriendList") { (userID) => {
        get {
          statisticsActor ! RequestStatistics()
          respondWithMediaType(`application/json`) {
            complete {
              val userActor = context.actorSelection(userActorBasePath + userID)
              val future: FriendList = Await.result(userActor ? GetFriendList(), timeout.duration).asInstanceOf[FriendList]
              future
            }
          }
        }
      }
      } ~ //Add a wall post
      path("users" / Segment / "posts" / "post") { (userID) => {
        post {
          statisticsActor ! RequestStatistics()
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
          statisticsActor ! RequestStatistics()
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
          statisticsActor ! RequestStatistics()
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
          statisticsActor ! RequestStatistics()
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
          statisticsActor ! RequestStatistics()
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
          statisticsActor ! RequestStatistics()
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
          statisticsActor ! RequestStatistics()
          respondWithMediaType(`application/json`) {
            complete {
              {
                val masterActor = context.actorSelection(masterActorBasePath)
                val future: Array[User] = Await.result(masterActor ? GetAllUsers(), timeout.duration).asInstanceOf[Array[User]]
                future
              }
            }
          }
        }
      }~ //add a user
      path("users" / "addUser") {
        post {
          statisticsActor ! RequestStatistics()
          respondWithMediaType(`text/plain`) {
            entity(as[User]) { user =>
            complete {
              {
                val masterActor = context.actorSelection(masterActorBasePath)
                masterActor ! AddUser(user.name, user.keys)
                "OK"
              }
            }}
          }
        }
      }~ //delete a user
      path("users" / "deleteUser") {
        delete {
          statisticsActor ! RequestStatistics()
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
                val future: Int = Await.result(masterActor ? GetNumberOfUsers(), timeout.duration).asInstanceOf[Int]
                future.toString
              }
            }
          }
        }
      }~ //get number of pages
      path("pages" / "getNumberOfPages") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              {
                val masterActor = context.actorSelection(masterActorBasePath)
                val future: Int = Await.result(masterActor ? GetNumberOfPages(), timeout.duration).asInstanceOf[Int]
                future.toString
              }
            }
          }
        }
      }~ //get a page
      path("pages" / "getPage" / Segment) { pageID =>
      get {
        statisticsActor ! RequestStatistics()
        respondWithMediaType(`application/json`) {
          complete {
            {
              val pageActor = context.actorSelection(pageActorBasePath + pageID)
              val future: Page = Await.result(pageActor ? GetPage(), timeout.duration).asInstanceOf[Page]
              future
            }
          }
        }
      }
    }~ // edit a page
      path("pages" / Segment / "editPage") { (pageID) => {
        put {
          statisticsActor ! RequestStatistics()
          respondWithMediaType(`text/plain`) {
            entity(as[Page]) { page =>
              complete {
                val pageActor = context.actorSelection(pageActorBasePath + pageID)
                pageActor ! UpdatePage(page)
                "OK"
              }
            }
          }
        }
      }
      } ~ //add a page
      path("pages" / "addPage") {
        post {
          statisticsActor ! RequestStatistics()
          respondWithMediaType(`text/plain`) {
            entity(as[String]) { string =>
              complete {
                {
                  val masterActor = context.actorSelection(masterActorBasePath)
                  masterActor ! AddPage(string.split(",")(0), string.split(",")(1))
                  "OK"
                }
              }}
          }
        }
      }~ //delete a page
      path("pages" / "deletePage") {
        delete {
          statisticsActor ! RequestStatistics()
          respondWithMediaType(`text/plain`) {
            entity(as[String]) { idToDelete =>
              complete {
                {
                  val masterActor = context.actorSelection(masterActorBasePath)
                  masterActor ! DeletePage(idToDelete)
                  "OK"
                }
              }
            }
          }
        }
      }~ //update aes keys
      path("users" / Segment / "updateKeys") { (userID) => {
        put {
          statisticsActor ! RequestStatistics()
          respondWithMediaType(`text/plain`) {
            entity(as[Vector[String]]) { keys: Vector[String] =>
              complete {
                val userActor = context.actorSelection(userActorBasePath + userID)
                userActor ! UpdateAESKeys(keys)
                "OK"
              }
            }
          }
        }
      }
      }

  }



}

case class RequestStatistics()

class StatisticsActor extends Actor{


  var totalRequestsReceived: Long = 0
  var isFirstRequest: Boolean = true
  var startTime: Long = 0
  var endTime: Long = 0

  def requestStatistics() = {
    if(isFirstRequest){
      startTime = System.currentTimeMillis()
    }
    totalRequestsReceived = totalRequestsReceived + 1
    isFirstRequest = false
    if(totalRequestsReceived%1000 == 0)
    {
      endTime = System.currentTimeMillis()
      val avg: BigDecimal = (totalRequestsReceived*1000/(endTime-startTime))
      println("Average number of requests served per second: " + avg.toString() + " for " + totalRequestsReceived + " requests received in " + (endTime-startTime) + "ms")
      /*println(totalRequestsReceived)
      println(endTime - startTime)*/
    }
  }

  def receive = {
    case RequestStatistics() => {
      requestStatistics()
    }
  }
}

