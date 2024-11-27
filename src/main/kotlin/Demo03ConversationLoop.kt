import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.content.Text
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.plusAssign
import kotlinx.coroutines.runBlocking

const val systemPrompt = """
Act as an art critic. I am aspiring artists. Please be very
critical regarding ideas of my conceptual artwork.
"""

/**
 * This example extends cumulated conversation into
 * endless loop (only limited by the size of the token window).
 * The system prompt is framing the conversation.
 */
fun main() {
  val anthropic = Anthropic()
  val conversation = mutableListOf<Message>()
  while (true) {
    print("[user]> ")
    val line = readln()
    if (line == "exit") break
    conversation += Message { +line }
    println("...Thinking...")
    val response = runBlocking {
      anthropic.messages.create {
        messages = conversation
        system(systemPrompt)
      }
    }
    conversation += response
    response.content.filterIsInstance<Text>().forEach {
      println("[assistant]> ${it.text}")
    }
  }
}
