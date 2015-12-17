package com.ramitsuri.project4

import java.security._
import java.util
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import akka.actor.{Cancellable, Props, Actor, ActorSystem}
import org.apache.commons.codec.binary.Base64
import spray.client.pipelining._
import spray.http.HttpRequest
import spray.httpx.SprayJsonSupport._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random
//import scala.concurrent.ExecutionContext.Implicits.global


case class UserRequestBatch1()

case class UserRequestBatch2()

case class SignUpUser()

case class StartScheduledTasks()

case class PageRequestBatch()

case class PeerToPeerMessaging()

object Client extends App {
  val timeout = 5.seconds
  val apiLocation = "http://127.0.0.1:8080/project4"
  implicit val clientSystem = ActorSystem("ClientSystem")
  import clientSystem.dispatcher

  val pipelineNumOfUsers: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
  val futureNumOfUsers: Future[String] = pipelineNumOfUsers(Get(s"$apiLocation/users/getNumberOfUsers"))
  val numOfUsers: Int = Integer.parseInt(Await.result(futureNumOfUsers, timeout))

  val pipelineNumOfPages: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
  val futureNumOfPages: Future[String] = pipelineNumOfPages(Get(s"$apiLocation/pages/getNumberOfPages"))
  val numOfPages: Int = Integer.parseInt(Await.result(futureNumOfPages, timeout))

  val numOfUsers1 = numOfUsers/4
  val numOfUsers2 = numOfUsers/2
  val numOfUsers3 = numOfUsers/4
  val batch1Time1 = 5
  val batch1Time2 = 10
  val batch2Time1 = 10
  val batch2Time2 = 15
  val batch3Time1 = 15
  val batch3Time2 = 30
  val batchPages = 5
  val clientUserActorBasePath = "akka://ClientSystem/user/clientActor"

  println("Creating actors for users and pages")

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

  Thread.sleep(1000)

  for(i<- 1 to numOfUsers){
    clientSystem.actorSelection(clientUserActorBasePath + i) ! StartScheduledTasks()
  }

  for(i<- 1 to numOfPages){
    clientSystem.actorOf(Props(new ClientPageActor(i, clientSystem, apiLocation, numOfPages, batchPages )))
  }

}

class ClientUserActor(id: Int, sys: ActorSystem, apiLocation: String,numOfUsers: Int, int1: Int, int2: Int) extends Actor {
  val userID = id
  var location = apiLocation
  val interval1 = int1
  val interval2 = int2
  val numberOfUsers = numOfUsers
  val timeout = 15.seconds
  var profile: Profile = new Profile("", new User("", "", Vector[WallPost](), Array[Byte]()), new FriendList("", Vector[String]()))
  private var scheduler1: Cancellable = _
  private var scheduler2: Cancellable = _
  private var scheduler3: Cancellable = _
  //private var privateAESKeyForName: String = generatePrivateKeyForAES()
  //private var privateAESKeyForPost: String = generatePrivateKeyForAES()
  var keyPair = RSA.getKeyPair()
  var publicKey = keyPair.getPublic()
  var privateKey = keyPair.getPrivate()
  var publicKeyBA = publicKey.getEncoded
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

  def receive = {

    case SignUpUser() => {

      try{
        val aesKey = generatePrivateKeyForAES()
        println(self.path)
        var userToSignup = new User(id = "", name = AES.encrypt(aesKey, "name" + userID )+ ", " + RSA.encrypt(aesKey, publicKey), posts = Vector[WallPost](), publicKey = publicKeyBA)
        val pipelineAddUser: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
        val futureAddUser: Future[String] = pipelineAddUser(Post(s"%s%s".format(apiLocation, "/users/addUser"), userToSignup ))
        val result2 = Await.result(futureAddUser, timeout)
      }
      /*catch {
        case ex: java.util.concurrent.TimeoutException => {ex.getMessage}
      }*/
    }

    case StartScheduledTasks() => {
      import scala.concurrent.duration._
      scheduler1 = context.system.scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(interval1, TimeUnit.SECONDS), self, UserRequestBatch1())
      scheduler2 = context.system.scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(interval2, TimeUnit.SECONDS), self, UserRequestBatch2())
    }

    case UserRequestBatch1() => {
        var randomID = Random.nextInt(numberOfUsers)
        if (randomID == 0)
          randomID = 1
      try {
        val pipelineGetProfile: HttpRequest => Future[Profile] = sendReceive ~> unmarshal[Profile]
        val futureGetProfile: Future[Profile] = pipelineGetProfile(Get(s"%s%s%d".format(apiLocation, "/users/getProfile/", userID)))
        val result2 = Await.result(futureGetProfile, timeout)
        profile = result2
        val combinedName = result2.user.name
        val name = combinedName.split(",")(0)
        println("User "+ AES.decrypt(RSA.decrypt(combinedName.split(",")(1), privateKey), name))

        val pipelineAddFriend: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
        val futureAddFriend: Future[String] = pipelineAddFriend(Post(s"%s%s%d%s".format(apiLocation, "/users/", randomID, "/addFriend"), "user" + randomID))
        val result3 = Await.result(futureAddFriend, timeout)
        println("Friended " + result3)

        val aesKey= generatePrivateKeyForAES()
        val post: WallPost = new WallPost("", "user" + randomID, Encryption.AES.encrypt(aesKey, "This is a new wall post") + ", " + RSA.encrypt(aesKey, publicKey))
        val pipelinePostOnWall: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
        val futurePostOnWall: Future[String] = pipelinePostOnWall(Post(s"%s%s%d%s".format(apiLocation, "/users/", randomID, "/posts/post"), post))
        val result1 = Await.result(futurePostOnWall, timeout)
        println("Posted " + result1)
      }
      /*catch {
        case ex: java.util.concurrent.TimeoutException => {ex.getMessage}
        case ex: Exception => {ex.getMessage}
      }*/
    }

    case UserRequestBatch2() => {

        var randomID = Random.nextInt(numberOfUsers)
        if (randomID == 0)
          randomID = 1
      try {
        val pipelineFriendList: HttpRequest => Future[FriendList] = sendReceive ~> unmarshal[FriendList]
        val futureFriendList: Future[FriendList] = pipelineFriendList(Get(s"%s%s%d%s".format(apiLocation, "/users/", randomID, "/getFriendList")))
        val result1 = Await.result(futureFriendList, timeout)
        println(result1.members.length + " Friends")

        val aesKey = generatePrivateKeyForAES()
        val newName = Encryption.AES.encrypt(aesKey, "new name")+ ", " + RSA.encrypt(aesKey, publicKey)
        val updatedUser = new User(profile.user.id, newName, profile.user.posts, profile.user.publicKey)
        val pipelineEditProfile: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
        val futureEditProfile: Future[String] = pipelineEditProfile(Put(s"%s%s%d%s".format(apiLocation, "/users/", randomID, "/editProfile"), updatedUser))
        val result2 = Await.result(futureEditProfile, timeout)
        println("Updated profile " + result2)
      }
      /*catch {
        case ex: java.util.concurrent.TimeoutException => {ex.getMessage}
        case ex: Exception => {ex.getMessage}
      }*/
    }
  }

  object RSA {

    def getKeyPair() : KeyPair = {
      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(2048)
      val keyPair = keyPairGenerator.generateKeyPair()
      keyPair
    }

    def encrypt(dataToEncrypt: String, publicKey: PublicKey) : String = {

      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.ENCRYPT_MODE, publicKey)
      val encryptedBytes = cipher.doFinal(dataToEncrypt.getBytes());
      val encryptedtext = new String(Base64.encodeBase64(encryptedBytes));
      encryptedtext
    }

    def decrypt(dataToDecrypt: String, privateKey: PrivateKey): String = {
      val cipher = Cipher.getInstance("RSA")
      cipher.init(Cipher.DECRYPT_MODE, privateKey)
      val encryptedtextBytes = Base64.decodeBase64(dataToDecrypt.getBytes());
      val decryptedBytes = cipher.doFinal(encryptedtextBytes);
      val decryptedString = new String(decryptedBytes);
      decryptedString
    }
  }

  object AES {

    private val SALT: String = "jMhKlOuJnM34G6NHkqo9V010GhLAqOpF0BePojHgh1HgNg8^72k"

    def encrypt(key: String, value: String): String = {
      val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
      cipher.init(Cipher.ENCRYPT_MODE, keyToSpec(key))
      Base64.encodeBase64String(cipher.doFinal(value.getBytes("UTF-8")))
    }

    def decrypt(key: String, encryptedValue: String): String = {
      val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
      cipher.init(Cipher.DECRYPT_MODE, keyToSpec(key))
      new String(cipher.doFinal(Base64.decodeBase64(encryptedValue)))
    }

    def keyToSpec(key: String): SecretKeySpec = {
      var keyBytes: Array[Byte] = (SALT + key).getBytes("UTF-8")
      val sha: MessageDigest = MessageDigest.getInstance("SHA-1")
      keyBytes = sha.digest(keyBytes)
      keyBytes = util.Arrays.copyOf(keyBytes, 16)
      new SecretKeySpec(keyBytes, "AES")
    }
  }

}


class ClientPageActor(id: Int, sys: ActorSystem, apiLocation: String, numOfPages: Int, interval1: Int) extends Actor{

  val pageID = id
  var location = apiLocation
  val interval = interval1
  val numberOfPages = numOfPages
  val timeout = 15.seconds
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
        val result2 = Await.result(futureGetPage, timeout)
        println("Page " + result2.name)
        var page = new Page(id = result2.id, owner = result2.owner, name = "new page name", result2.posts)

        val pipelineEditPage: HttpRequest => Future[String] = sendReceive ~> unmarshal[String]
        val futureEditPage: Future[String] = pipelineEditPage(Put(s"%s%s%d%s".format(apiLocation, "/pages/", randomID, "/editPage"), page))
        val result3 = Await.result(futureEditPage, timeout)
        println("Page edited " + result3)

      }
      /*catch {
        case ex: java.util.concurrent.TimeoutException => {ex.getMessage}
      }*/
    }
  }
}



