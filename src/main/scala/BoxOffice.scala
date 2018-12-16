import akka.actor.{Actor, ActorRef, Props}

import scala.concurrent.Future
import akka.util.Timeout

object BoxOffice{
  def props(implicit timeout: Timeout): Props = Props(new BoxOffice)
  def name: String = "boxOffice"

  case class CreateEvent(nameEvent: String, qtyTickets: Int)
  case class GetEvent(nameEvent: String)
  case object GetEvents
  case class GetTickets(nameEvent: String, qtyTicketsForGet: Int)
  case class CancelEvent(nameEvent: String)


  case class Event(name: String, tickets: Int)
  case class Events(events: Seq[Event])

  trait EventResponse
  case class EventCreated(event: Event) extends EventResponse
  case object EventExist extends EventResponse
}

class BoxOffice(implicit timeout: Timeout) extends Actor {

  import BoxOffice._
  import context._

  def createTicketSeller(name: String): ActorRef = context.actorOf(TicketSeller.props(name), name)

  def receive: Receive = {
    case CreateEvent(nameEvent, qtyTickets) => {
      def create() = {
        val eventTickets: ActorRef = createTicketSeller(nameEvent)
        val newTickets = (1 to qtyTickets).map(TicketSeller.Ticket)
        eventTickets ! TicketSeller.Add(newTickets)
        sender() ! EventCreated(Event(nameEvent, qtyTickets))
      }

      context.child(nameEvent).fold(create())(_ => sender() ! EventExist)
    }
    case GetTickets(nameEvent, qtyTicketsForGet) => {
      def notFound(): Unit = sender() ! TicketSeller.Tickets(nameEvent)

      def buy(child: ActorRef): Unit = child forward TicketSeller.Buy(qtyTicketsForGet)

      context.child(nameEvent).fold(notFound())(buy)
    }

    case GetEvent(nameEvent) => {
      def notFound(): Unit = sender() ! None

      def getEvent(child: ActorRef): Unit = child forward TicketSeller.GetEvent

      context.child(nameEvent).fold(notFound())(getEvent)
    }

    case GetEvents => {
      import akka.pattern.ask
      import akka.pattern.pipe

      def getEvents() = context.children
        .map { c => self.ask(GetEvent(c.path.name)).mapTo[Option[Event]] }

      def convertToEvents(f: Future[Iterable[Option[Event]]]): Future[Events] =
        f.map(_.flatten).map(l => Events(l.toSeq))

      pipe(convertToEvents(Future.sequence(getEvents()))) to sender()
    }

    case CancelEvent(event) => {
      def notFound(): Unit = sender() ! None

      def cancelEvent(child: ActorRef): Unit = child forward TicketSeller.Cancel

      context.child(event).fold(notFound())(cancelEvent)
    }
  }
}