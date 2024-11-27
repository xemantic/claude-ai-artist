import com.xemantic.anthropic.Anthropic
import com.xemantic.anthropic.content.Image
import com.xemantic.anthropic.content.ToolUse
import com.xemantic.anthropic.message.Message
import com.xemantic.anthropic.schema.Description
import com.xemantic.anthropic.tool.AnthropicTool
import com.xemantic.anthropic.tool.ToolInput
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import javax.sound.midi.MidiChannel
import javax.sound.midi.MidiSystem

fun main() {
    runBlocking {
        val synth = getSynthesizer()
        val anthropic = Anthropic {
            tool<PlayMusic> {
                synthesizer = synth
                scope = this@runBlocking
            }
        }
        val response = anthropic.messages.create {
            +Message {
                +"Can you play the music from the attached picture?"
                +Image("data/images/chopin.jpg")
            }
            singleTool<PlayMusic>()
        }
        val toolUse = response.content.filterIsInstance<ToolUse>().first()
        toolUse.use()
    }
}

@AnthropicTool("PlayMusic")
@Description("Plays the music on the local MIDI device")
data class PlayMusic(val notes: List<Note>) : ToolInput() {

    @Transient
    lateinit var synthesizer: MidiChannel

    @Transient
    lateinit var scope: CoroutineScope

    init {
      use {
        scope.launch {
          notes.forEach { note ->
            launch {
              delay(note.startTime)
              synthesizer.noteOn(note.midiKey, 127)
              delay(note.duration)
              synthesizer.noteOff(note.midiKey, 0)
            }
          }
          "music was played"
        }
      }
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
