package org.stacktrace.yo.ombi

import akka.actor
import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.bot4s.telegram.api.{ActorBroker, AkkaDefaults}
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.{Message, Update}
import akka.actor.{Actor, ActorRef, Props, Terminated}
import cats.syntax.functor._
import cats.instances.future._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{ActorBroker, AkkaDefaults}
import com.bot4s.telegram.future.Polling
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.{Message, Update}

trait OmbiBroker extends ActorBroker with AkkaDefaults {

  override val broker: Option[ActorRef] = Some(system.actorOf(actor.Props(new Broker), "broker"))

  class Broker extends Actor {

    val chatActors = collection.mutable.Map[Long, ActorRef]()

    def receive = {
      case u: Update =>
        u.message.foreach { m =>
          val id = m.chat.id
          val handler = chatActors.getOrElseUpdate(m.chat.id, {
            val worker = system.actorOf(Props(new OmbiWorker), s"worker_$id")
            context.watch(worker)
            worker
          })
          handler ! m
        }

      case Terminated(worker) =>
        // This should be faster
        chatActors.find(_._2 == worker).foreach {
          case (k, _) => chatActors.remove(k)
        }

      case _ =>
    }
  }


  class OmbiWorker extends Actor {
    def receive = {
      case m: Message =>
//        request(SendMessage(m.source, self.toString))
      case _ =>
    }
  }

}

