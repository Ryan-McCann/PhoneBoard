package app.ryanm.phoneboard.ime

import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_NONE
import android.view.inputmethod.ExtractedTextRequest
import androidx.compose.ui.input.key.KeyEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PhoneboardIMEService: PhoneboardLifecycleService(),
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val kbController: KBController = KBController(::handleIntent, scope)

    private val dictionary = FlatTrie.loadfromAssets(this, "dicts/dict.trie")

    override fun onCreateInputView(): View {
        val view = ComposeKeyboardView(this, kbController)

        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        return view
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)

        kbController.currentLayout = when(editorInfo?.inputType?.and(InputType.TYPE_MASK_CLASS)) {
            InputType.TYPE_CLASS_NUMBER -> LayoutState.Numeric

            InputType.TYPE_CLASS_PHONE -> LayoutState.Phone

            else -> LayoutState.Alpha
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()

        kbController.shifted = ShiftState.Off
    }

    override fun onWindowHidden() {
        super.onWindowHidden()

        kbController.shifted = ShiftState.Off
    }

    override val viewModelStore: ViewModelStore
        get() = store
    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    private val store = ViewModelStore()

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private fun isWordChar(ch: Char): Boolean {
        return ch.isLetter() || ch == '\'' || ch == '-'
    }

    private fun extractCurrentWord(): String {
        val lineStart = currentInputConnection
            .getTextBeforeCursor(64, 0)
            ?.toString()
            .orEmpty()

        val lineEnd = currentInputConnection
            .getTextAfterCursor(64, 0)
            ?.toString()
            .orEmpty()

        val start = lineStart.indexOfLast { !isWordChar(it)} + 1
        val wordStart = lineStart.substring(start)

        val end = lineEnd.indexOfFirst { !isWordChar(it) }

        val wordEnd =
            if(end > -1)
                lineEnd.substring(0, end)
            else
                lineEnd

        return wordStart + wordEnd
    }

    private fun refreshSuggestions() {
        val word = extractCurrentWord()

        kbController.suggestions = if(word.isNotEmpty() && kbController.currentLayout == LayoutState.Alpha)
            listOf(word)
        else
            emptyList()
    }

    private fun handleIntent(intent: KBIntent) {
        when(intent) {
            is KBIntent.CommitText -> {
                currentInputConnection.commitText(intent.text, 1)
            }
            KBIntent.Backspace -> {
                val selectedText = currentInputConnection.getSelectedText(0)
                if(selectedText != "" && selectedText != null)
                    currentInputConnection.commitText("", 1)
                else
                    currentInputConnection.deleteSurroundingText(1, 0)
            }
            KBIntent.Undo -> {
            }
            KBIntent.Enter -> {
                val actionId = currentInputEditorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
                val hasAction = actionId != IME_ACTION_NONE
                val noEnterAction = (currentInputEditorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

                if(hasAction && !noEnterAction)
                    sendDefaultEditorAction(true)
                else
                    currentInputConnection.commitText("\n", 1)
            }
            KBIntent.OpenSettings ->  {

            }

            KBIntent.SwitchLayout -> {
                if(kbController.currentLayout == LayoutState.Alpha)
                    kbController.currentLayout = LayoutState.Symbols
                else
                    kbController.currentLayout = LayoutState.Alpha
            }

            KBIntent.MoveLeft -> {
                val curPos = currentInputConnection.getExtractedText(ExtractedTextRequest(), 0).selectionStart ?: return

                currentInputConnection.setSelection(curPos-1, curPos-1)
            }
            KBIntent.MoveRight -> {
                val curPos = currentInputConnection.getExtractedText(ExtractedTextRequest(), 0).selectionStart ?: return

                currentInputConnection.setSelection(curPos+1, curPos+1)
            }
        }

        refreshSuggestions()
    }

}