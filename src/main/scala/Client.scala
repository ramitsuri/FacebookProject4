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
  var location = apiLocation
  val interval1= int1
  val interval2 = int2
  val numberOfUsers = numOfUsers
  val timeout = 15.seconds
  var profile: Profile = new Profile("", new User("", "", Vector[WallPost]()), new FriendList("", Vector[String]()))
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
      var randomID = Random.nextInt(numberOfUsers)
      if(randomID==0)
        randomID = 1
      val pipelineGetProfile: HttpRequest => Future[Profile] = sendReceive ~> unmarshal[Profile]
      val futureGetProfile: Future[Profile] = pipelineGetProfile(Get(s"%s%s%d".format(apiLocation,"/users/getProfile/", randomID)))
      val result2 = Await.result(futureGetProfile, timeout)
      profile = result2
      println(result2.user.name)

      val pipelineAddFriend: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
      val futureAddFriend: Future[String] = pipelineAddFriend(Post(s"%s%s%d%s".format(apiLocation,"/users/", randomID, "/addFriend"), "user" + randomID))
      val result3 = Await.result(futureAddFriend, timeout)
      println("friended "+result3)

      val post: WallPost = new WallPost("", "user"+ randomID, "This is a new wall post")
      val pipelinePostOnWall: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
      val futurePostOnWall: Future[String] = pipelinePostOnWall(Post(s"%s%s%d%s".format(apiLocation,"/users/", randomID, "/posts/post"), post))
      val result1 = Await.result(futurePostOnWall, timeout)
      println("posted "+result1)
    }

    case RequestBatch2() => {
      var randomID = Random.nextInt(numberOfUsers)
      if(randomID==0)
        randomID = 1
      val pipelineFriendList: HttpRequest => Future[FriendList] = sendReceive ~> unmarshal[FriendList]
      val futureFriendList: Future[FriendList] = pipelineFriendList(Get(s"%s%s%d%s".format(apiLocation,"/users/", randomID, "/getFriendList")))
      val result1 = Await.result(futureFriendList, timeout)
      println(result1.members.length + " friends")

      val updatedUser = new User(profile.user.id, "new name", profile.user.posts)
      val pipelineEditProfile: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
      val futureEditProfile: Future[String] = pipelineEditProfile(Put(s"%s%s%d%s".format(apiLocation,"/users/", randomID, "/editProfile"), updatedUser))
      val result2 = Await.result(futureEditProfile, timeout)
      println("updated profile "+ result2)
    }
  }

}
