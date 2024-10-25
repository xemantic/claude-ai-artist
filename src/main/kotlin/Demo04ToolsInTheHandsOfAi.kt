import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.message.ToolUse
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.UsableTool
import kotlinx.coroutines.runBlocking

@AnthropicTool("Fibonacci")
@Description("Calculates Fibonacci number n")
data class FibonacciTool(val n: Int) : UsableTool {
  override suspend fun use(toolUseId: String) = ToolResult(
    toolUseId, text = "${fibonacci(n)}"
  )
  private tailrec fun fibonacci(
    n: Int, a: Int = 0, b: Int = 1
  ): Int = when (n) {
    0 -> a; 1 -> b; else -> fibonacci(n - 1, b, a + b)
  }
}
fun main() = runBlocking {
  val client = Anthropic { tool<FibonacciTool>() }
  val response = client.messages.create {
    +Message { +"What's Fibonacci number 42" }
    useTools()
  }
  val toolUse = response.content.filterIsInstance<ToolUse>().first()
  val toolResult = toolUse.use()
  println(toolResult)
}
