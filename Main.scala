import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

case class UserJoined(userActor: ActorRef)
case class UserLeft(username: String)

class ChatRoom extends Actor {
  var users: Map[String, ActorRef] = Map.empty

  def receive: Receive = {
    case UserJoined(userActor) =>
      context.watch(userActor)
      users += (userActor.path.name -> userActor)
      broadcastMessage(s"User ${userActor.path.name} joined the chat.")
    case UserLeft(username) =>
      users -= username
      broadcastMessage(s"User $username left the chat.")
    case message: String =>
      broadcastMessage(s"${sender().path.name}: $message")
  }

  def broadcastMessage(message: String): Unit = {
    users.values.foreach(_ ! TextMessage.Strict(message))
  }
}

class UserActor extends Actor {
  var userActor: Option[ActorRef] = None

  def receive: Receive = {
    case UserJoined(actor) =>
      userActor = Some(actor)
    case UserLeft(username) =>
      userActor.foreach(_ ! TextMessage.Strict("You have left the chat."))
      context.stop(self)
    case msg: TextMessage.Strict =>
      userActor.foreach(_ ! msg)
  }
}

object Main extends App {
  implicit val system = ActorSystem("ReactiveChatApp")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val chatRoom = system.actorOf(Props[ChatRoom], "chatRoom")

  val route =
    path("chat" / Segment) { room =>
      get {
        handleWebSocketMessages(userSocket(room))
      }
    }

  Http().bindAndHandle(route, "localhost", 8080)

  def userSocket(room: String): Flow[Message, Message, _] =
    Flow.fromMaterializer { (mat, _) =>
      val userActor = mat.system.actorOf(Props[UserActor], s"user-${java.util.UUID.randomUUID()}")
      userActor ! UserJoined(userActor)

      val incomingMessages: Sink[Message, _] =
        Sink.actorRef(userActor, UserLeft(userActor.path.name))

      val outgoingMessages: Source[Message, _] =
        Source.actorRef[Message](10, OverflowStrategy.fail)
          .mapMaterializedValue { outActor =>
            userActor ! UserJoined(outActor)
            outActor ! TextMessage.Strict("You have joined the chat.")
            outActor
          }

      Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
    }
}
