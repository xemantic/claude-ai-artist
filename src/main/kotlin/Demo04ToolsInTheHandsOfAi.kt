import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.content.ToolUse
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.ToolInput
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
  val client = Anthropic { tool<FibonacciTool>() }
  val response = client.messages.create {
    +Message { +"What's Fibonacci number 42" }
    allTools()
  }
  val toolUse = response.content.filterIsInstance<ToolUse>().first()
  val toolResult = toolUse.use()
  println(toolResult)
}

tailrec fun fibonacci(
  n: Int, a: Int = 0, b: Int = 1
): Int = when (n) {
  0 -> a; 1 -> b; else -> fibonacci(n - 1, b, a + b)
}

@AnthropicTool("Fibonacci")
@Description("Calculates Fibonacci number n")
data class FibonacciTool(val n: Int) : ToolInput() {
  init {
    use {
      fibonacci(n)
    }
  }
}
