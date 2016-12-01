package io.github.vigoo.examples.akkatypedtcp.actors

import akka.typed.ScalaDSL.{Full, Msg, Sig, Stopped, Unhandled}
import akka.typed.{ActorRef, Behavior, Terminated}
import io.github.vigoo.examples.akkatypedtcp.actors.Connection.{Connect, ConnectionMessage}
import io.github.vigoo.examples.akkatypedtcp.actors.Root.{RootMessage, StartClient}

/**
  * The root actor of the typed actor system
  *
  * It spawns a connection and monitors its lifecycle. Does not perform
  * reconnect in this example.
  */
class Root {
  def rootBehavior: Behavior[RootMessage] =
    Full {
      case Msg(ctx, StartClient(address, port, untypedActorSystem)) =>
        val connection = ctx.spawn(new Connection(untypedActorSystem).started(), "connection")
        ctx.watch(connection)

        connection ! Connect(address, port)

        running(connection)
    }

  def running(connection: ActorRef[ConnectionMessage]): Behavior[RootMessage] =
    Full {
      case Msg(_, StartClient(_, _, _)) =>
        println("Connection already started")
        Unhandled

      case Sig(_, Terminated(_)) =>
        println("Connection terminated")
        Stopped
    }
}

object Root {
  sealed trait RootMessage
  case class StartClient(remoteHostName: String, remotePort: Int, untypedActorSystem: akka.actor.ActorSystem) extends RootMessage
}
