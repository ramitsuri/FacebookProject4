package com.ramitsuri.project4

import java.security.PublicKey
import java.util.concurrent.TimeUnit
import akka.pattern.ask
import akka.actor.{Props, Actor}
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration.Duration


//Message classes for Master Actor
case class Start()

case class GetAllUsers()

case class AddUser(name: String, publicKey: Array[Byte])

case class DeleteUser(id: String)

case class GetNumberOfUsers()

case class GetNumberOfPages()

case class AddPage(name: String, owner: String)

case class DeletePage(id: String)

case class Test()


class MasterActor(numOfUsers: Int, numOfPages: Int) extends Actor {

  var numberOfUsers = numOfUsers
  var numberOfPages = numOfPages
  var usersIDs: Vector[String] = Vector[String]()
  var pageIDs: Vector[String] = Vector[String]()
  var users: Vector[User] = Vector[User]()
  var pages: Vector[Page] = Vector[Page]()
  val userActorBasePath = "akka://FaceBookSystem/user/httpInterface/masterActor/user"
  var hasRunOnceUsers = false
  var hasRunOncePages = false
  var lastID: Int = 0

  def start() = {
    /*for (i <- 1 to numberOfUsers) {*/
      //var user = context.actorOf(Props(new UserActor(i.toString, "name" + i)), name = "user" + i)
      //usersIDs = usersIDs :+ i.toString
   /* }*/

    for (i <- 1 to numberOfPages) {
      var page = context.actorOf(Props(new PagesActor(i.toString, "owner" + i, "name" + i)), name = "page" + i)
      pageIDs = pageIDs :+ i.toString
    }
  }

  def getAllUsers() = {
    if(!hasRunOnceUsers) {
      for (i <- 1 to numberOfUsers) {
        val userActor = context.actorSelection(userActorBasePath + i)
        implicit val timeout = Timeout(Duration(10, TimeUnit.SECONDS))
        val future: User = Await.result(userActor ? GetUserDetails(), timeout.duration).asInstanceOf[User]
        users = users :+ future
      }
    }
    hasRunOnceUsers = true
    users.toArray
  }

  def addUser(name: String, publicKey: Array[Byte]) = {
    //numberOfUsers = numberOfUsers + 1
    lastID = lastID + 1
    var user = context.actorOf(Props(new UserActor(lastID.toString, name, publicKey)), name = "user" + lastID)
    usersIDs = usersIDs :+ lastID.toString
    println("added " + lastID  + name)
  }

  def deleteUser(id: String) = {
    numberOfUsers = numberOfUsers - 1
    usersIDs = usersIDs.filterNot(_ == id)
  }

  def getNumberOfUsers() = {
    numberOfUsers
  }

  def getNumberOfPages() = {
    numberOfPages
  }

  def addPage(name: String, owner: String) = {
    numberOfPages = numberOfPages + 1
    var page = context.actorOf(Props(new PagesActor(numberOfPages.toString, owner , name)), name = "page" + numberOfPages)
    pageIDs = pageIDs :+ numberOfPages.toString
  }

  def deletePage(pageIDToDelete: String) = {
    numberOfPages = numberOfPages - 1
    pages = pages.filterNot(_.id == pageIDToDelete)
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

    case AddUser(name: String, publicKey: Array[Byte]) => {
      addUser(name, publicKey)
    }

    case DeleteUser(id: String) => {
      deleteUser(id)
    }

    case GetNumberOfUsers() => {
      sender ! getNumberOfUsers()
    }

    case AddPage(name: String, owner: String) => {
      addPage(name, owner)
    }

    case DeletePage(id: String) => {
      deletePage(id)
    }

    case GetNumberOfPages() => {
      sender ! getNumberOfPages()
    }
    


  }

  implicit def toPage(page: Page): Page = Page(id = page.id, owner = page.owner, name = page.name, posts = page.posts)
}