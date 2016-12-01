package io.github.vigoo.examples.akkatypedtcp.actors

import java.net.InetSocketAddress

import akka.actor.Actor
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.typed.ActorRef
import akka.typed.ScalaDSL.{Same, Stopped}
import io.github.vigoo.examples.akkatypedtcp.actors.TcpClient.{IncomingEvent, TcpClientMessage}

/**
  * Traditional untyped actor bridging the messages between the akka IO TCP connection and
  * the TcpClient typed actor.
  *
  * @param remoteHostName Host to connect to
  * @param remotePort Port to connect to
  * @param typedClient The typed actor to bridge to
  */
class ClientBridge(remoteHostName: String, remotePort: Int, typedClient: ActorRef[TcpClientMessage]) extends Actor {
  import context.system
  IO(Tcp) ! akka.io.Tcp.Connect(new InetSocketAddress(remoteHostName, remotePort))

  var connection: Option[akka.actor.ActorRef] = None

  override def receive: PartialFunction[Any, Unit] = {
    case c@Connected(_, _) =>
      connection = Some(sender())
      sender() ! Register(self)
      typedClient ! IncomingEvent(c)
    case event: Event =>
      typedClient ! IncomingEvent(event)
    case command: Command =>
      connection match {
        case Some(c) =>
          c ! command
          Same
        case None =>
          typedClient ! IncomingEvent(ErrorClosed("Sending attempt to not connected client"))
          Stopped
      }
  }
}
