import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.content.Image
import com.xemantic.anthropic.content.ToolUse
import com.xemantic.anthropic.message.*
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.ToolInput
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

@AnthropicTool("OpenCallsReceiver")
@Description("Receives entries from the image")
data class OpenCallsReceiver(
  val calls: List<Entry>
) : ToolInput() {
  init {
    use {
      "Data provided to client"
    }
  }
}

@Serializable
data class Entry(
  val deadline: String,
  val title: String,
)

fun main() = runBlocking {

  val client = Anthropic {
    tool<OpenCallsReceiver>()
  }

  val response = client.messages.create {
    +Message {
      +Image("data/images/open-calls-creatives.jpg")
      +"Decode open calls from supplied image"
    }
    singleTool<OpenCallsReceiver>()
  }

  val tool = response.content.filterIsInstance<ToolUse>().first()
  val receiver = tool.input<OpenCallsReceiver>()

  receiver.calls.forEach {
    println(it)
  }

}
