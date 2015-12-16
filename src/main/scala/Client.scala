package com.ramitsuri.project4

import java.security.{PrivateKey, PublicKey, KeyPair, MessageDigest}
import java.util
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import akka.pattern.ask
import akka.actor.{Cancellable, Props, Actor, ActorSystem}
import akka.util.Timeout
import org.apache.commons.codec.binary.Base64
import spray.client.pipelining._
import spray.http.HttpRequest
import spray.httpx.SprayJsonSupport._
import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random

//import scala.concurrent.ExecutionContext.Implicits.global


case class UserRequestBatch1()

case class UserRequestBatch2()

case class SignUpUser()

case class StartScheduledTasks()

case class UpdateKeys(keys: Vector[String])

case class PageRequestBatch()

case class PeerToPeerMessaging()

case class GetPublicKey()

object Client extends App {
  val timeoutForApi = 5.seconds
  val apiLocation = "http://127.0.0.1:8080/project4"
  implicit val clientSystem = ActorSystem("ClientSystem")

  import clientSystem.dispatcher

  val pipelineNumOfUsers: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
  val futureNumOfUsers: Future[String] = pipelineNumOfUsers(Get(s"$apiLocation/users/getNumberOfUsers"))
  val numOfUsers: Int = Integer.parseInt(Await.result(futureNumOfUsers, timeoutForApi))

  val pipelineNumOfPages: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
  val futureNumOfPages: Future[String] = pipelineNumOfPages(Get(s"$apiLocation/pages/getNumberOfPages"))
  val numOfPages: Int = Integer.parseInt(Await.result(futureNumOfPages, timeoutForApi))

  val numOfUsers1 = numOfUsers / 4
  val numOfUsers2 = numOfUsers / 2
  val numOfUsers3 = numOfUsers / 4
  val batch1Time1 = 5
  val batch1Time2 = 10
  val batch2Time1 = 10
  val batch2Time2 = 15
  val batch3Time1 = 15
  val batch3Time2 = 30
  val batchPages = 5
  val clientUserActorBasePath = "akka://ClientSystem/user/clientActor"

  println("Creating actors for users and pages")

  /*for (i <- 1 to numOfUsers) {
    clientSystem.actorOf(Props(new ClientUserActor(i, clientSystem, apiLocation, numOfUsers, batch1Time1, batch1Time2)), name = "clientActor" + i)
    //clientSystem.actorSelection(clientUserActorBasePath + i) ! SignUpUser()
  }
*/
   for(i <- 1 to numOfUsers1){
     clientSystem.actorOf(Props(new ClientUserActor(i, clientSystem, apiLocation, numOfUsers, batch1Time1, batch1Time2 )), name = "clientActor" + i)
     clientSystem.actorSelection(clientUserActorBasePath + i) ! SignUpUser()
     }

   for(i <- numOfUsers1+1 to numOfUsers1+numOfUsers2){
     clientSystem.actorOf(Props(new ClientUserActor(i, clientSystem, apiLocation, numOfUsers, batch2Time1, batch2Time2)), name = "clientActor" + i)
     clientSystem.actorSelection(clientUserActorBasePath + i) ! SignUpUser()
     }

   for(i <- numOfUsers1+numOfUsers2+1 to numOfUsers){
     clientSystem.actorOf(Props(new ClientUserActor(i, clientSystem, apiLocation, numOfUsers, batch3Time1, batch3Time2)), name = "clientActor" + i)
     clientSystem.actorSelection(clientUserActorBasePath + i) ! SignUpUser()
     }

  Thread.sleep(5000)

  for (i <- 1 to numOfUsers) {
    clientSystem.actorSelection(clientUserActorBasePath + i) ! StartScheduledTasks()
  }

  for (i <- 1 to numOfPages) {
    clientSystem.actorOf(Props(new ClientPageActor(i, clientSystem, apiLocation, numOfPages, batchPages)))
  }

}

class ClientUserActor(id: Int, sys: ActorSystem, apiLocation: String, numOfUsers: Int, int1: Int, int2: Int) extends Actor {
  val userID = id
  var location = apiLocation
  val interval1 = int1
  val interval2 = int2
  val numberOfUsers = numOfUsers
  val timeoutForApi = 15.seconds
  private var privateAESKeyForName: String = generatePrivateKeyForAES()
  private var privateAESKeyForPost: String = generatePrivateKeyForAES()
  private var keyPair: KeyPair = Encryption.RSA.getKeyPair()
  private var privateRSAKey: PrivateKey = keyPair.getPrivate()
  private var publicRSAKey: PublicKey = keyPair.getPublic()
  var profile: Profile = new Profile("", new User("", "", Vector[WallPost](), keys = Vector[String]()), new FriendList("", Vector[String]()))
  private var scheduler1: Cancellable = _
  private var scheduler2: Cancellable = _
  private var scheduler3: Cancellable = _
  implicit val clientSystem = sys
  val clientUserActorBasePath = "akka://ClientSystem/user/clientUserActor"

  import clientSystem.dispatcher
  import NewJsonProtocol._

  override def preStart(): Unit = {
    import scala.concurrent.duration._
    //self ! SignUpUser()
    //scheduler1 = context.system.scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(interval1, TimeUnit.SECONDS), self, UserRequestBatch1())
    //scheduler2 = context.system.scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(interval2, TimeUnit.SECONDS), self, UserRequestBatch2())
    //scheduler3 = context.system.scheduler.schedule(Duration.create(10 , TimeUnit.SECONDS), Duration.create(100, TimeUnit.MILLISECONDS), self, RequestBatch2())

  }

  override def postStop(): Unit = {
    scheduler1.cancel()
    scheduler2.cancel()
  }

  private def generatePrivateKeyForAES(): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    val sb = new StringBuilder
    val random = new scala.util.Random(new java.security.SecureRandom())
    for (i <- 1 to 10) {
      val randomNum = random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString + userID
  }

  //implement
  private def updateKeys() ={
    privateAESKeyForName = generatePrivateKeyForAES()
    privateAESKeyForPost = generatePrivateKeyForAES()
    keyPair = Encryption.RSA.getKeyPair()
    publicRSAKey = keyPair.getPublic()
    privateRSAKey = keyPair.getPrivate()
  }


  def receive = {

    case SignUpUser() => {

      /*try {*/
        println(self.path)
        var name = Encryption.AES.encrypt(privateAESKeyForName, "user" + userID)
        var userSignUp: User = new User("", name = name,  posts = Vector[WallPost](), keys = Vector[String]())
        val pipelineAddUser: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
        val futureAddUser: Future[String] = pipelineAddUser(Post(s"%s%s".format(apiLocation, "/users/addUser"), userSignUp))
        val result2 = Await.result(futureAddUser, timeoutForApi)

      /*}
      catch {
        case ex: java.util.concurrent.TimeoutException => {}
      }*/
    }

    case StartScheduledTasks() => {

try {
  /*var keys = Vector[String]()
  if (userID != 1) {
    implicit val timeout = Timeout(10 seconds)
    implicit val ec = context.dispatcher
    var future = clientSystem.actorSelection(clientUserActorBasePath + (userID - 1)) ? GetPublicKey()
    val result = Await.result(future, timeout.duration).asInstanceOf[PublicKey]
    keys :+ (userID - 1).toString+","+ Encryption.RSA.encrypt(privateAESKeyForName, result)
  }
  self ! UpdateKeys(keys)*/
  import scala.concurrent.duration._
  scheduler1 = context.system.scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(interval1, TimeUnit.SECONDS), self, UserRequestBatch1())
  scheduler2 = context.system.scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(interval2, TimeUnit.SECONDS), self, UserRequestBatch2())
}
      catch {
        case ex:Exception =>{

        }
      }
    }

    case UserRequestBatch1() => {
      var randomID = Random.nextInt(numberOfUsers)
      if (randomID == 0)
        randomID = 1
      var name = ""
      try {
        val pipelineGetProfile: HttpRequest => Future[Profile] = sendReceive ~> unmarshal[Profile]
        val futureGetProfile: Future[Profile] = pipelineGetProfile(Get(s"%s%s%d".format(apiLocation, "/users/getProfile/", userID)))
        val result2 = Await.result(futureGetProfile, timeoutForApi)
        profile = result2
        name = result2.user.name
        println("User " + Encryption.AES.decrypt(privateAESKeyForName, name))
        //println(self.toString()  + userID + " " + privateAESKeyForName +  " "  + Encryption.AES.encrypt(privateAESKeyForName, "name" + userID) + "User "+ Encryption.AES.decrypt(privateAESKeyForName, result2.user.name))

        println("User " + "Key " + privateAESKeyForName + "name " + name + " decrypted " + Encryption.AES.decrypt(result2.user.name, privateAESKeyForName))
        val pipelineAddFriend: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
        val futureAddFriend: Future[String] = pipelineAddFriend(Post(s"%s%s%d%s".format(apiLocation, "/users/", randomID, "/addFriend"), "user" + randomID))
        val result3 = Await.result(futureAddFriend, timeoutForApi)
        println("Friended " + result3)

        val encryptedPostText = Encryption.AES.encrypt(privateAESKeyForPost, "This is a new wall post")
        val post: WallPost = new WallPost("", "user" + randomID, encryptedPostText, Vector[String]())
        val pipelinePostOnWall: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
        val futurePostOnWall: Future[String] = pipelinePostOnWall(Post(s"%s%s%d%s".format(apiLocation, "/users/", randomID, "/posts/post"), post))
        val result1 = Await.result(futurePostOnWall, timeoutForApi)
        println("Posted " + result1)
      }
      catch {
        case ex: java.util.concurrent.TimeoutException => {}
        case ex: Exception => {
          //println("execp " + privateAESKeyForName + " " + name)
        }
      }
    }

    case UserRequestBatch2() => {

      var randomID = Random.nextInt(numberOfUsers)
      if (randomID == 0)
        randomID = 1
      try {
        val pipelineFriendList: HttpRequest => Future[FriendList] = sendReceive ~> unmarshal[FriendList]
        val futureFriendList: Future[FriendList] = pipelineFriendList(Get(s"%s%s%d%s".format(apiLocation, "/users/", randomID, "/getFriendList")))
        val result1 = Await.result(futureFriendList, timeoutForApi)
        println(result1.members.length + " Friends")

        val updatedUser = new User(profile.user.id, "new name", profile.user.posts, profile.user.keys)
        val pipelineEditProfile: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
        val futureEditProfile: Future[String] = pipelineEditProfile(Put(s"%s%s%d%s".format(apiLocation, "/users/", randomID, "/editProfile"), updatedUser))
        val result2 = Await.result(futureEditProfile, timeoutForApi)
        println("Updated profile " + result2)
      }
      catch {
        case ex: java.util.concurrent.TimeoutException => {}
      }
    }

    case GetPublicKey() => {
      sender() ! publicRSAKey
    }

    case UpdateKeys(keys) =>{
      updateKeys()

    }
  }
}


class ClientPageActor(id: Int, sys: ActorSystem, apiLocation: String, numOfPages: Int, interval1: Int) extends Actor {

  val pageID = id
  var location = apiLocation
  val interval = interval1
  val numberOfPages = numOfPages
  val timeoutForApi = 15.seconds
  private var scheduler1: Cancellable = _
  implicit val clientSystem = sys
  val clientPageActorBasePath = "akka://ClientSystem/user/clientPageActor"

  import clientSystem.dispatcher
  import NewJsonProtocol._

  override def preStart(): Unit = {
    import scala.concurrent.duration._
    scheduler1 = context.system.scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(interval, TimeUnit.SECONDS), self, PageRequestBatch())
  }

  override def postStop(): Unit = {
    scheduler1.cancel()
  }


  def receive = {
    case PageRequestBatch() => {
      var randomID = Random.nextInt(numberOfPages)
      if (randomID == 0)
        randomID = 1
      try {
         val pipelineGetPage: HttpRequest => Future[Page] = sendReceive ~> unmarshal[Page]
         val futureGetPage: Future[Page] = pipelineGetPage(Get(s"%s%s%d".format(apiLocation, "/pages/getPage/", randomID)))
         val result2 = Await.result(futureGetPage, timeoutForApi)
         println("Page " + result2.name)
         var page = new Page(id = result2.id, owner = result2.owner, name = "new page name", result2.posts)

         val pipelineEditPage: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
         val futureEditPage: Future[String] = pipelineEditPage(Put(s"%s%s%d%s".format(apiLocation, "/pages/", randomID, "/editPage"), page))
         val result3 = Await.result(futureEditPage, timeoutForApi)
         println("Page edited " + result3)

      }
      catch {
        case ex: java.util.concurrent.TimeoutException => {}
      }
    }
  }
}

