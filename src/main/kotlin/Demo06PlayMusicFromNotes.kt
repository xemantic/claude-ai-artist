import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.message.Image
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.message.ToolResult
import com.xemantic.anthropic.message.ToolUse
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.UsableTool
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import javax.sound.midi.MidiChannel
import javax.sound.midi.MidiSystem

fun main() {
    runBlocking {
        val synth = getSynthesizer()
        val anthropic = Anthropic {
            tool<PlayMusic> {
                synthesizer = synth
            }
        }
        val response = anthropic.messages.create {
            +Message {
                +"Can you play the music from the attached picture?"
                +Image(
                    file = File("data/images/happy-birthday-chords-two-hands.webp"),
                    mediaType = Image.MediaType.IMAGE_WEBP
                )
            }
            useTool<PlayMusic>()
        }
        val toolUse = response.content.filterIsInstance<ToolUse>().first()
        toolUse.use()
    }
}

@AnthropicTool("PlayMusic")
@Description("Plays the music on the local MIDI device")
data class PlayMusic(val notes: List<Note>) : UsableTool {

    @Transient
    lateinit var synthesizer: MidiChannel

    override suspend fun use(toolUseId: String): ToolResult = coroutineScope {
        notes.forEach { note ->
            launch {
                delay(note.startTime)
                synthesizer.noteOn(note.midiKey, 127)
                delay(note.duration)
                synthesizer.noteOff(note.midiKey, 0)
            }
        }
        ToolResult(toolUseId, "music was played")
    }
}

@Serializable
@SerialName("note")
data class Note(
    val midiKey: Int,
    @Description("Note start time in milliseconds")
    val startTime: Long,
    @Description("Note duration in milliseconds")
    val duration: Long
)

fun getSynthesizer(): MidiChannel = MidiSystem.getSynthesizer().run {
    open()
    loadInstrument(defaultSoundbank.instruments[0])
    channels[0]
}
