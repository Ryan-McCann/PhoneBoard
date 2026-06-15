package app.ryanm.phoneboard.ime

import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_NONE
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

            }
        }
    }

}