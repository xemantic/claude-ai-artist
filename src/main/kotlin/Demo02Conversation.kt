import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.plusAssign
import kotlinx.coroutines.runBlocking

/**
 * This example shows how conversation cumulates in the
 * token window.
 */
fun main() = runBlocking {
  val anthropic = Anthropic()
  val conversation = mutableListOf<Message>()
  conversation += Message {
    +"Is it true, that to know we can die is to be dead already?"
  }
  val response1 = anthropic.messages.create {
    messages = conversation
  }
  println(response1)
  conversation += response1
  conversation += Message {
    +"Why do you think I asked you this question?"
  }
  val response2 = anthropic.messages.create {
    messages = conversation
  }
  println(response2)
  conversation += response2

  println("The whole past conversation is included in the token window")
  println(conversation)
}
