import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.StopReason
import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.message.ToolUse
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.UsableTool
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.launch
import org.openrndr.math.Vector2

var linesToDraw = emptyList<Line>()

fun main() = application {

  configure {
    width = 800
    height = 600
  }

  program {

    val systemPrompt = "You can draw on a canvas visible to the human. Current resolution: ${width}x${height}, the background is black"
    val anthropic = Anthropic {
      tool<DrawLines>()
    }

    launch(Dispatchers.IO) {
      println("request")
      val response = anthropic.messages.create {
        system(systemPrompt)
        +Message { +"Draw a tree" }
        useTool<DrawLines>()
      }
      if (response.stopReason == StopReason.TOOL_USE) {
        response.content.filterIsInstance<ToolUse>().forEach { it.use() }
      }
      println("$response")
    }
    extend {
      linesToDraw.forEach { line ->
        drawer.stroke = line.color
        drawer.strokeWeight = line.thickness
        drawer.lineSegment(line.start, line.end)
      }
    }
  }

}

@AnthropicTool("DrawLines")
@Description("Draws lines specified in the lines list")
data class DrawLines(
  val lines: List<Line>
) : UsableTool {

  override suspend fun use(toolUseId: String): ToolResult {
    linesToDraw += lines
    return ToolResult(toolUseId, "line drawn")
  }

}

@Serializable
@SerialName("line")
data class Line(
  val color: ColorRGBa,
  val thickness: Double,
  val start: Vector2,
  val end: Vector2
)
