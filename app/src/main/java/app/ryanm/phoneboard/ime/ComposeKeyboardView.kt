package app.ryanm.phoneboard.ime

import GboardRedDark
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView

class ComposeKeyboardView(context: Context, val kbController: KBController) : AbstractComposeView(context){

    @Composable
    override fun Content() {
        MaterialTheme( colorScheme = GboardRedDark) {
            KeyboardRoot(kbController)
        }
    }
}