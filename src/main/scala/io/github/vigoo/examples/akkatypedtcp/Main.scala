package io.github.vigoo.examples.akkatypedtcp

import akka.typed._
import io.github.vigoo.examples.akkatypedtcp.actors.Root
import io.github.vigoo.examples.akkatypedtcp.actors.Root.StartClient

object Main extends App {

  if (args.length == 2) {
    val hostname = args(0)
    val port = args(1).toInt

    // Starting a non-typed actor system for the IO
    val untypedSystem = akka.actor.ActorSystem()

    // And a typed actor system for the client code
    val system = ActorSystem("root", new Root().rootBehavior)
    system ! StartClient(hostname, port, untypedSystem)

    // Shutting down both actor systems at the end
    import system.executionContext
    system.whenTerminated.onComplete { _ =>
      untypedSystem.terminate()
    }
  }
  else {
    println("Usage: akka-typed-tcp-client <hostname> <port>")
  }
}
