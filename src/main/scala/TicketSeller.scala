import akka.actor.{Actor, ActorRef, PoisonPill, Props}

object TicketSeller {
  def props(nameEvent: String): Props = Props(new TicketSeller(nameEvent))

  case class Buy(qty: Int)
  case class Add(tickets: Seq[Ticket])
  case object GetEvent
  case object Cancel

  case class Ticket(idTicket: Int)
  case class Tickets(nameEvent: String, entries: Seq[Ticket] = Seq.empty[Ticket])

}
class TicketSeller(nameEvent: String) extends Actor {
  import TicketSeller._

  var tickets = Seq.empty[Ticket]

  def receive: Receive = {
    case Add(newTickets) => tickets = tickets ++ newTickets

    case Buy(qty) => {
      val entries = tickets.take(qty)
      if(entries.size >= qty) {
        sender() ! Tickets(nameEvent, entries)
        tickets = tickets.drop(qty)
      } else sender() ! Tickets(nameEvent)
    }

    case GetEvent => sender() ! Some(BoxOffice.Event(nameEvent, tickets.size))

    case Cancel =>
      sender() ! Some(BoxOffice.Event(nameEvent, tickets.size))
      self ! PoisonPill
  }
}
