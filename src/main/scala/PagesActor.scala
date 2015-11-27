package com.ramitsuri.project4

import akka.actor.Actor


case class UpdatePage(updatedPage: Page)

case class GetPage()


class PagesActor(id: String, owner: String, name: String) extends Actor{
  var pageInfo: Page = new Page(id = id, owner = owner, name = name, Vector[WallPost]())


  def updatePage(updatedPage: Page) = {
    pageInfo.name = updatedPage.name
    pageInfo.owner = updatedPage.owner
    pageInfo.posts = updatedPage.posts
  }

  def getPageInfo() = {
    pageInfo
  }

  def receive = {

    case UpdatePage(updatedPage: Page) => {
      updatePage(updatedPage)
    }

    case GetPage() => {
      sender ! getPageInfo()
    }

  }
}
