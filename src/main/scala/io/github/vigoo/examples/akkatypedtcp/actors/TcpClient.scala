package io.github.vigoo.examples.akkatypedtcp.actors

import akka.io.Tcp._
import akka.typed.ScalaDSL.{Full, Msg, Partial, Same, Stopped}
import akka.typed.{ActorRef, Behavior}
import akka.util.ByteString
import TcpClient.{IncomingEvent, Initialize, OutgoingCommand, TcpClientMessage}

/**
  * A low level typed TCP client actor
  *
  * Works together with the ClientBridge untyped actor to receive events from and send commands
  * to the akka IO TCP connection.
  *
  * @param wrapReceived Function to wrap the received byte string to a high-level protocol message
  * @param receiver The high-level client connection actor to send the incoming messages to
  * @tparam T High level message type
  */
class TcpClient[T](wrapReceived: ByteString => T, receiver: ActorRef[T]) {
  def started(): Behavior[TcpClientMessage] =
    Partial {
      case Initialize(bridge) =>
        initialized(bridge)
    }

  private def initialized(bridge: akka.actor.ActorRef): Behavior[TcpClientMessage] =
    Full {
      case Msg(_, IncomingEvent(CommandFailed(command))) =>
        println(s"Command failed: $command")
        Stopped
      case Msg(_, IncomingEvent(Connected(remote, local))) =>
        println("Connected")
        Same
      case Msg(_, IncomingEvent(Received(data))) =>
        println(s"Received $data")
        receiver ! wrapReceived(data)
        Same
      case Msg(_, IncomingEvent(_: ConnectionClosed)) =>
        println("Connection closed")
        Stopped
      case Msg(_, IncomingEvent(event)) =>
        println(s"Unhandled incoming event: $event")
        Same

      case Msg(_, OutgoingCommand(command)) =>
        bridge ! command
        Same
    }
}

object TcpClient {
  sealed trait TcpClientMessage
  case class Initialize(bridge: akka.actor.ActorRef) extends TcpClientMessage
  case class IncomingEvent(event: Event) extends TcpClientMessage
  case class OutgoingCommand(command: Command) extends TcpClientMessage
}
