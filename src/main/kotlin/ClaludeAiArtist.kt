import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.message.Image
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.StopReason
import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.message.ToolUse
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.UsableTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noclear.NoClear
import org.openrndr.launch
import org.openrndr.math.Vector2
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import javax.sound.midi.MidiChannel
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer

fun main() = application {

  configure {
    width = 800
    height = 600
  }

  program {

    val synth =  MidiSystem.getSynthesizer()
    val channel = with(synth) {
      open()
      loadInstrument(defaultSoundbank.instruments[0])
      channels[0]
    }

    channel.noteOn(72, 127)
    runBlocking {
      delay(100)
    }
    channel.noteOff(72, 0)
    val lines = ConcurrentLinkedQueue<Line>()

    val systemPrompt = "Current resolution: ${width}x${height}, the background is black"
    val anthropic = Anthropic {
      tool<PlayMusic> {
        myChannel = channel
      }
      //logHttp = true
    }
    val conversation = mutableListOf<Message>()

//    conversation += Message { +"Draw 10 lines creating a spiral which looks like a cat" }
    conversation += Message {
      +"Can you play the music from the attached picture"
      +Image(
        file = File("data/images/maxresdefault.jpg"),
        mediaType = Image.MediaType.IMAGE_JPEG
      )
    }
    val job = GlobalScope.launch(Dispatchers.IO) {
      val response = anthropic.messages.create {
        messages = conversation
        system(systemPrompt)
        useTool<PlayMusic>()
      }
      if (response.stopReason == StopReason.TOOL_USE) {
        response.content.filterIsInstance<ToolUse>().forEach { it.use() }
//        this@program.launch {
//          toolUse.use()
//        }
      }
    }
    extend {
      lines.forEach { line ->
        drawer.stroke = line.color
        drawer.strokeWeight = line.thickness
        drawer.lineSegment(line.start, line.end)
      }
    }
  }

}

@AnthropicTool("draw_lines")
@Description("Draws lines specified in the lines list")
data class DrawLine(
  @Description("The list of lines to draw")
  val lines: List<Line>
) : UsableTool {

  @Transient
  internal lateinit var linesToDraw: ConcurrentLinkedQueue<Line>

  override suspend fun use(toolUseId: String): ToolResult {
    linesToDraw += this.lines
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

@AnthropicTool("play_music")
@Description("Plays the music on the local MIDI device")
data class PlayMusic(
  val notes: List<Note>,
) : UsableTool{

  @Transient
  lateinit var myChannel: MidiChannel

  override suspend fun use(toolUseId: String): ToolResult {
    println("Music")
    println(notes)
    notes.forEach {
      GlobalScope.launch {
        delay((it.startTime * 1000.0).toLong())
        myChannel.noteOn(it.midiKey, 127)
        delay((it.duration * 1000.0).toLong())
        myChannel.noteOff(it.midiKey, 0)
      }
    }
    return ToolResult(toolUseId, "music was played")
  }
}

@Serializable
@SerialName("note")
data class Note(
  val midiKey: Int,
  val startTime: Double,
  val endTime: Double,
  val duration: Double
)
