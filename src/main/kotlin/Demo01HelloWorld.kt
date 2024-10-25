import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.message.Message
import kotlinx.coroutines.runBlocking

fun main() {
  val anthropic = Anthropic()
  val response = runBlocking {
    anthropic.messages.create {
      +Message {
        +"Hello World!"
      }
    }
  }
  println(response)
}
