package io.github.vigoo.examples.akkatypedtcp.actors

import akka.actor.Props
import akka.io.Tcp.Write
import akka.typed.ScalaDSL.{Full, Msg, Same, Sig, Stopped, Unhandled}
import akka.typed.{ActorRef, Behavior, Terminated}
import akka.util.ByteString
import io.github.vigoo.examples.akkatypedtcp.actors.Connection.{Connect, ConnectionMessage, ProcessResponse, SendPing}
import io.github.vigoo.examples.akkatypedtcp.actors.TcpClient.{Initialize, OutgoingCommand, TcpClientMessage}

import scala.concurrent.duration._

/**
  * The high level connection actor, getting high level protocol messages to be sent
  * and unparsed response data that it can parse and forward to other components of
  * the system.
  *
  * @param untypedActorSystem The actor system in which the TCP IO is running
  */
class Connection(untypedActorSystem: akka.actor.ActorSystem) {
  def started(): Behavior[ConnectionMessage] =
    Full {
      case Msg(ctx, Connect(hostName, port)) =>
        // Spawning the low-level typed TCP client actor
        val client = ctx.spawn(new TcpClient(ProcessResponse, ctx.self).started(), "tcp-client")

        // Spawning the typed-untyped bridge actor
        val bridge = untypedActorSystem.actorOf(Props(new ClientBridge(hostName, port, client)), "bridge")


        client ! Initialize(bridge)
        ctx.watch(client)

        // Periodic ping messages as an example of the high level protocol
        ctx.schedule(1.second, ctx.self, SendPing)
        connected(client)
    }

  private def connected(client: ActorRef[TcpClientMessage]): Behavior[ConnectionMessage] =
    Full {
      case Msg(_, Connect(_, _)) =>
        println("Already connected")
        Unhandled

      case Msg(ctx, SendPing) =>
        println(s"Sending ping")

        // Sending the low-level TCP actor the command to write out a string
        client ! OutgoingCommand(Write(ByteString("ping")))

        ctx.schedule(1.second, ctx.self, SendPing)
        Same
      case Msg(_, ProcessResponse(response)) =>
        // Received data from the low-level TCP actor; to be parsed, etc.
        println(s"Got response: ${response.utf8String}")
        Same

      case Sig(_, Terminated(ref)) =>
        println("Client terminated")
        Stopped
    }
}

object Connection {
  sealed trait ConnectionMessage
  case class Connect(remoteHostName: String, remotePort: Int) extends ConnectionMessage
  case object SendPing extends ConnectionMessage
  case class ProcessResponse(response: ByteString) extends ConnectionMessage
}