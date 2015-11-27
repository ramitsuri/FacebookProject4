package com.ramitsuri.project4

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import spray.can.Http

object project4 extends App {

  implicit val facebookSystem = ActorSystem("FaceBookSystem")
  val api = facebookSystem.actorOf(Props(new FacebookServer()), "httpInterface")
  IO(Http) ! Http.Bind(api, "localhost", 8080)

}