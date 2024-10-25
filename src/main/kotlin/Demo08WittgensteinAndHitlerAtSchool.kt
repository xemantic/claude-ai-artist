import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.message.*
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.UsableTool
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.launch
import org.openrndr.math.Vector2

var circlesToDraw = emptyList<Circle>()

fun main() = application {

    configure {
        width = 512
        height = 760
    }

    program {

        val schoolImage = loadImage("data/images/Leonardo_da_Vinci_-_Mona_Lisa_(Louvre,_Paris)FXD.jpg")

        val systemPrompt = "You can draw on a canvas visible to the human. Current resolution: ${width}x${height}, the background is black"
        val anthropic = Anthropic {
            tool<DrawCircles>()
        }

        launch(Dispatchers.IO) {
            println("request")
            val response = anthropic.messages.create {
                system(systemPrompt)
                +Message {
                    +"Draw black glasses."
                    +Image(
                        path = "data/images/Leonardo_da_Vinci_-_Mona_Lisa_(Louvre,_Paris)FXD.webp",
                        mediaType = Image.MediaType.IMAGE_WEBP
                    )
                }
                useTool<DrawCircles>()
                toolChoice = ToolChoice.Auto()
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
            println("$response")
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
) : UsableTool {

    override suspend fun use(toolUseId: String): ToolResult {
        circlesToDraw += circles
        return ToolResult(toolUseId, "circle drawn")
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
