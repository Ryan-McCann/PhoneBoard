package app.ryanm.phoneboard.ime

import android.view.inputmethod.InlineSuggestion
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Long.max


data class Action (
    val type: String,
    val text: String? = null,
    val shift: String? = null,
    val caps: String? = null
)

data class Actions(
    val tap: Action,
    val up: Action? = null,
    val down: Action? = null,
    val left: Action? = null,
    val right: Action? = null
)

data class Key (
    val legend: String,
    val shift: String?,
    val caps: String?,
    val shade: String,
    val submit: Boolean = false,
    val circle: Boolean,
    val actions: Actions
)

data class Layout (
    val keys: List<Key>
)

sealed interface KBIntent {
    data class CommitText(val text: String) : KBIntent
    data object Backspace: KBIntent
    data object Undo: KBIntent
    data object Enter: KBIntent
    data object OpenSettings: KBIntent
    data object SwitchLayout: KBIntent
    data object MoveLeft: KBIntent
    data object MoveRight: KBIntent
    data class ReplaceCurrentWord(val suggestion: String): KBIntent
}

enum class LayoutState {
    Alpha,
    Symbols,
    Numeric,
    Phone,
    Emote
}

enum class ShiftState {
    Off,
    Shift,
    Caps
}

class KBController (private val emitIntent: (KBIntent) -> Unit, private val scope: CoroutineScope) {

    private var repeatJob: Job? = null

    var shifted by mutableStateOf(ShiftState.Off)
    var currentLayout by mutableStateOf(LayoutState.Alpha)
    var suggestions by mutableStateOf<List<String>>(emptyList())

    fun pressStart(action: Action) {

        when(action.type) {
            "backspace" -> {
                repeatJob?.cancel()
                repeatJob = scope.launch {
                    var interval: Long = 140
                    while(true) {
                        emitIntent(KBIntent.Backspace)
                        delay(interval)
                        interval = max (40, ((interval * 0.8).toLong()))
                    }
                }
            }
        }
    }

    fun pressEnd(action: Action) {
        when(action.type) {
            "text" -> {
                if(action.text != null) {
                    val text =
                        when {
                            shifted != ShiftState.Off && action.shift != null -> action.shift
                            shifted != ShiftState.Off -> action.text.uppercase()
                            else -> action.text
                        }
                        if(shifted != ShiftState.Off) action.text.uppercase() else action.text

                    if(shifted == ShiftState.Shift)
                        shifted = ShiftState.Off

                    emitIntent(KBIntent.CommitText(text))
                }
            }
            "space" -> {
                emitIntent(KBIntent.CommitText(" "))
            }
            "undo" -> {
                emitIntent(KBIntent.Undo)
            }
            "submit" -> {
                emitIntent(KBIntent.Enter)
            }
            "shift" -> {
                shifted = when(shifted) {
                    ShiftState.Off -> ShiftState.Shift
                    ShiftState.Shift -> ShiftState.Caps
                    ShiftState.Caps -> ShiftState.Off
                }
            }
            "switch" -> {
                emitIntent(KBIntent.SwitchLayout)
            }
            "move_left" -> {
                emitIntent(KBIntent.MoveLeft)
            }
            "move_right" -> {
                emitIntent(KBIntent.MoveRight)
            }
            "suggestion" -> {
                action.text?.let {
                    emitIntent(KBIntent.ReplaceCurrentWord(it))
                }
            }
        }

        repeatJob?.cancel()
    }
}