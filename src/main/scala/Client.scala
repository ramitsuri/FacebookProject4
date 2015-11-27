package com.ramitsuri.project4

import java.util.concurrent.TimeUnit

import akka.actor.{Cancellable, Props, Actor, ActorSystem}
import spray.client.pipelining._
import spray.http.HttpRequest
import spray.httpx.SprayJsonSupport._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random
//import scala.concurrent.ExecutionContext.Implicits.global


case class RequestBatch1()

case class RequestBatch2()

object Client extends App {
  val timeout = 5.seconds
  val apiLocation = "http://127.0.0.1:8080/project4"
  implicit val clientSystem = ActorSystem("ClientSystem")
  import clientSystem.dispatcher


  /*val pipeline: HttpRequest => Future[User] = sendReceive ~> unmarshal[User]
  val f: Future[User] = pipeline(Get(s"$apiLocation/users/getUser/10"))
  val user = Await.result(f, timeout)
  println(user.name)

  //post
  var post1: WallPost = WallPost("post1", "user1", "this is a test post")
  var post2: WallPost = WallPost("post2", "user2", "this is a test post")
  var post3: WallPost = WallPost("post3", "user3", "this is a test post")
  var post4: WallPost = WallPost("post4", "user4", "this is a test post")

  var posts = Vector[WallPost]()
  posts = posts :+ post1 :+ post2 :+ post3 :+ post4
  val pipeline2: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
  val f2: Future[String] = pipeline2(Post(s"$apiLocation/users/addUser", "Ramit"))
  val result2 = Await.result(f, timeout)
  println(result2)

  //put
  val userToUpdate = new User("11", "Ramit Suri", posts)
  val pipeline3: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
  val f3: Future[String] = pipeline2(Put(s"$apiLocation/users/11/editProfile", userToUpdate))
  val result3 = Await.result(f, timeout)
  println(result3)
    //delete
    val pipeline4: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
    val f4: Future[String] = pipeline4(Delete(s"$apiLocation/users/deleteUser", "11"))
    val result4 = Await.result(f, timeout)*/


  val pipelineNumOfUsers: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
  val futureNumOfUsers: Future[String] = pipelineNumOfUsers(Get(s"$apiLocation/users/getNumberOfUsers"))
  val numOfUsers: Int = Integer.parseInt(Await.result(futureNumOfUsers, timeout))

  val numOfUsers1 = numOfUsers/4
  val numOfUsers2 = numOfUsers/2
  val numOfUsers3 = numOfUsers/4
  val batch1Time1 = 10
  val batch1Time2 = 20
  val batch2Time1 = 20
  val batch2Time2 = 30
  val batch3Time1 = 30
  val batch3Time2 = 60


  for(i <- 1 to numOfUsers1){
    clientSystem.actorOf(Props(new ClientActor(i, clientSystem, apiLocation, numOfUsers, batch1Time1, batch1Time2 )), name = "clientActor" + i)
  }
  
  for(i <- numOfUsers1+1 to numOfUsers1+numOfUsers2){
    clientSystem.actorOf(Props(new ClientActor(i, clientSystem, apiLocation, numOfUsers, batch2Time1, batch2Time2)), name = "clientActor" + i)
  }
  
  for(i <- numOfUsers1+numOfUsers2+1 to numOfUsers){
    clientSystem.actorOf(Props(new ClientActor(i, clientSystem, apiLocation, numOfUsers, batch3Time1, batch3Time2)), name = "clientActor" + i)
  }

}

class ClientActor(id: Int, sys: ActorSystem, apiLocation: String,numOfUsers: Int, int1: Int, int2: Int) extends Actor{
  val userID = id
  val location = apiLocation
  val interval1= int1
  val interval2 = int2
  val numberOfUsers = numOfUsers
  val timeout = 5.seconds
  private var scheduler1: Cancellable = _
  private var scheduler2: Cancellable = _
  implicit val clientSystem = sys
  import clientSystem.dispatcher
  import NewJsonProtocol._

  override def preStart(): Unit = {
    import scala.concurrent.duration._
    scheduler1 = context.system.scheduler.schedule(Duration.create(0 , TimeUnit.MILLISECONDS), Duration.create(interval1, TimeUnit.SECONDS), self, RequestBatch1())
    scheduler2 = context.system.scheduler.schedule(Duration.create(0 , TimeUnit.MILLISECONDS), Duration.create(interval2, TimeUnit.SECONDS), self, RequestBatch2())

  }

  override def postStop(): Unit = {
    scheduler1.cancel()
    scheduler2.cancel()
  }

  def receive = {

    case RequestBatch1() => {
      val randomID = Random.nextInt(numberOfUsers-1) + 1

      val pipelineGetProfile: HttpRequest => Future[User] = sendReceive ~> unmarshal[User]
      val futureGetProfile: Future[User] = pipelineGetProfile(Get(s"%s%s%d".format(apiLocation,"/users/getProfile/", randomID)))
      val result2 = Await.result(futureGetProfile, timeout)
      println(result2.name)

      /*val pipelineAddFriend: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
      val futureAddFriend: Future[String] = pipelineAddFriend(Post(s"%s%s%d%s".format(apiLocation,"/users/", randomID, "/getProfile")))
      val result3 = Await.result(futureAddFriend, timeout)

      val pipelinePostOnWall: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
      val futurePostOnWall: Future[String] = pipelinePostOnWall(Post(s"%s%s%d%s".format(apiLocation,"/users/", randomID, "/posts/post")))
      val result1 = Await.result(futurePostOnWall, timeout)*/

    }

    case RequestBatch2() => {
      val randomID = Random.nextInt(numberOfUsers-1) + 1

     /* val pipelineFriendList: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
      val futureFriendList: Future[String] = pipelineFriendList(Get(s"%s%s%d%s".format(apiLocation,"/users/", randomID, "/getFriendList")))
      val result1 = Await.result(futureFriendList, timeout)

      val pipelineEditProfile: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
      val futureEditProfile: Future[String] = pipelineEditProfile(Put(s"%s%s%d%s".format(apiLocation,"/users/", randomID, "/editProfile")))
      val result2 = Await.result(futureEditProfile, timeout)*/

    }
  }

}
