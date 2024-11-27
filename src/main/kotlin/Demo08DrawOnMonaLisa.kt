import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.content.Image
import com.xemantic.anthropic.content.Text
import com.xemantic.anthropic.content.ToolUse
import com.xemantic.anthropic.message.*
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.ToolInput
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.launch
import org.openrndr.math.Vector2

var circlesToDraw = emptyList<Circle>()

// Downloaded from Wikipedia
private const val monaLisaPath =
  "data/images/Leonardo_da_Vinci_-_Mona_Lisa_(Louvre,_Paris)FXD.jpg"

fun main() = application {

    configure {
        width = 512
        height = 760
    }

    program {

        val schoolImage = loadImage(monaLisaPath)

        val systemPrompt = "You can draw on a canvas visible to the human. Current resolution: ${width}x${height}"
        val anthropic = Anthropic {
            tool<DrawCircles>()
        }

        launch(Dispatchers.IO) {
            println("Prompting Claude (Anthropic API)")
            val response = anthropic.messages.create {
                system(systemPrompt)
                +Message {
                    +Image(monaLisaPath)
                    +"Draw black circles around the eyes of the person on this picture."

                }
                allTools()
            }
            if (response.stopReason == StopReason.TOOL_USE) {
                response.content.forEach {
                    when (it) {
                        is Text -> println(it.text)
                        is ToolUse -> it.use()
                        else -> {}
                    }
                }
            }
        }
        extend {
            drawer.image(schoolImage, 0.0, 0.0, width.toDouble(), height.toDouble())
            circlesToDraw.forEach { circle ->
                drawer.stroke = circle.color
                drawer.strokeWeight = circle.thickness
                drawer.fill = null
                drawer.circle(
                    position = circle.position,
                    radius = circle.radius
                )
            }
        }
    }

}

@AnthropicTool("DrawCircles")
@Description("Draws circles specified in the circle list")
data class DrawCircles(
    val circles: List<Circle>
) : ToolInput() {

  init {
    use {
      circlesToDraw += circles
      "circle drawn"
    }
  }

}

@Serializable
@SerialName("circle")
data class Circle(
    val position: Vector2,
    val radius: Double,
    val color: ColorRGBa,
    val thickness: Double
)
