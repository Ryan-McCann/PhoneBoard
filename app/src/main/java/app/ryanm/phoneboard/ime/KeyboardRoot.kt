package app.ryanm.phoneboard.ime

import GboardRedDark
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import cc.ekblad.toml.decode
import cc.ekblad.toml.tomlMapper
import java.io.InputStream
import kotlin.math.absoluteValue

@Composable
fun KeyboardRoot(kbController: KBController) {
    MaterialTheme(colorScheme = GboardRedDark) {
        KeyboardGrid(kbController)
    }
}

@Composable
fun KeyButton(key: Key, kbController: KBController) {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current

    var pressed by remember { mutableStateOf(false) }
    var currentAction by remember {mutableStateOf(key.actions.tap)}

    val hasAlternates = listOf(key.actions.up, key.actions.down, key.actions.left, key.actions.right).any {it != null}

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if(interaction is PressInteraction.Press) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                kbController.pressStart(currentAction)
                pressed = true
            } else if(interaction is PressInteraction.Cancel || interaction is PressInteraction.Release) {
                kbController.pressEnd(currentAction)
                pressed = false
            }
        }
    }
    val buttonShape = if(key.circle) CircleShape else RoundedCornerShape(12.dp)
    val bgColor =
        if(pressed)
            MaterialTheme.colorScheme.secondaryContainer
        else if(key.shade == "dark")
            MaterialTheme.colorScheme.surfaceVariant
        else if(key.submit)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surface

    val wedgeColor = MaterialTheme.colorScheme.primary

    if(hasAlternates && pressed) {
        val size = 60
        val yOffset = if(currentAction == key.actions.up) {
            with(LocalDensity.current){(-size - 40).dp.roundToPx()}
        } else {
            with(LocalDensity.current){(-size - 5).dp.roundToPx()}
        }
        Popup(
            alignment = TopCenter,
            offset = IntOffset(0, yOffset)
        ) {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides Color.White
                ) {
                    val legend = when(currentAction) {
                        key.actions.tap -> key.legend
                        key.actions.up -> key.actions.up.text!!
                        key.actions.left -> key.actions.left.text!!
                        key.actions.right -> key.actions.right.text!!
                        key.actions.down -> key.actions.down.text!!
                        else -> key.legend
                    }

                    if(currentAction == key.actions.tap) {
                        Box(Modifier.fillMaxSize()) {
                            val legend = when (kbController.shifted) {
                                ShiftState.Off -> key.legend
                                ShiftState.Shift -> key.shift ?: key.legend.uppercase()
                                ShiftState.Caps -> key.caps ?: key.shift ?: key.legend.uppercase()
                            }

                            Text(
                                legend,
                                modifier = Modifier.align(Alignment.Center),
                                fontSize = 24.sp,
                            )
                            if (key.actions.up != null && key.actions.up.text != null) {
                                val up = when (kbController.shifted) {
                                    ShiftState.Off -> key.actions.up.text
                                    ShiftState.Shift -> key.actions.up.shift ?: key.actions.up.text.uppercase()
                                    ShiftState.Caps -> key.actions.up.caps ?: key.actions.up.shift
                                    ?: key.actions.up.text.uppercase()
                                }
                                Text(
                                    up,
                                    modifier = Modifier.align(TopCenter),
                                    fontWeight = FontWeight.Thin,
                                    fontSize = 14.sp
                                )
                            }
                            if (key.actions.down != null && key.actions.down.text != null) {
                                val down = when (kbController.shifted) {
                                    ShiftState.Off -> key.actions.down.text
                                    ShiftState.Shift -> key.actions.down.shift
                                        ?: key.actions.down.text.uppercase()

                                    ShiftState.Caps -> key.actions.down.caps ?: key.actions.down.shift
                                    ?: key.actions.down.text.uppercase()
                                }
                                Text(
                                    down,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    fontWeight = FontWeight.Thin,
                                    fontSize = 14.sp
                                )
                            }
                            if (key.actions.left != null && key.actions.left.text != null) {
                                val left = when (kbController.shifted) {
                                    ShiftState.Off -> key.actions.left.text
                                    ShiftState.Shift -> key.actions.left.shift
                                        ?: key.actions.left.text.uppercase()

                                    ShiftState.Caps -> key.actions.left.caps ?: key.actions.left.shift
                                    ?: key.actions.left.text.uppercase()
                                }

                                Text(
                                    left,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 8.dp),
                                    fontWeight = FontWeight.Thin,
                                    fontSize = 14.sp
                                )
                            }
                            if (key.actions.right != null && key.actions.right.text != null) {
                                val right = when (kbController.shifted) {
                                    ShiftState.Off -> key.actions.right.text
                                    ShiftState.Shift -> key.actions.right.shift
                                        ?: key.actions.right.text.uppercase()

                                    ShiftState.Caps -> key.actions.right.caps ?: key.actions.right.shift
                                    ?: key.actions.right.text.uppercase()
                                }

                                Text(
                                    right,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 8.dp),
                                    fontWeight = FontWeight.Thin,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        Text(legend)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(buttonShape)
            .background(bgColor)
            .drawWithContent {
                drawContent()
                if(hasAlternates && pressed) {
                    when(currentAction) {
                        key.actions.up -> {
                            val path = Path().apply {
                                moveTo(size.width / 2f, size.height / 2f)
                                lineTo(0f, 0f)
                                lineTo(size.width, 0f)
                                close()
                            }
                            drawPath(
                                path,
                                color = wedgeColor
                            )
                        }
                        key.actions.down -> {
                            val path = Path().apply {
                                moveTo(size.width / 2f, size.height / 2f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(
                                path,
                                color = wedgeColor
                            )
                        }
                        key.actions.left -> {
                            val path = Path().apply {
                                moveTo(size.width / 2f, size.height / 2f)
                                lineTo(0f, 0f)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(
                                path,
                                color = wedgeColor
                            )
                        }
                        key.actions.right -> {
                            val path = Path().apply {
                                moveTo(size.width / 2f, size.height / 2f)
                                lineTo(size.width, 0f)
                                lineTo(size.width, size.height)
                                close()
                            }
                            drawPath(
                                path,
                                color = wedgeColor
                            )
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val id = down.id
                        down.consume()
                        pressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                        val startPos = down.position

                        currentAction = key.actions.tap
                        kbController.pressStart(currentAction)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break

                            if (!change.pressed)
                                break

                            val currentPos = change.position
                            val offset = currentPos - startPos

                            if (offset.x.absoluteValue > offset.y.absoluteValue) {
                                currentAction =
                                    if (offset.x >= 50 && key.actions.right != null) {
                                        key.actions.right
                                    } else if (offset.x <= -50 && key.actions.left != null) {
                                        key.actions.left
                                    } else {
                                        key.actions.tap
                                    }
                            } else {
                                currentAction =
                                    if (offset.y >= 50 && key.actions.down != null) {
                                        key.actions.down
                                    } else if (offset.y <= -50 && key.actions.up != null) {
                                        key.actions.up
                                    } else {
                                        key.actions.tap
                                    }
                            }

                            change.consume()
                        }

                        kbController.pressEnd(currentAction)
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White
        ) {
            Box(Modifier.fillMaxSize()) {
                val legend = when (kbController.shifted) {
                    ShiftState.Off -> key.legend
                    ShiftState.Shift -> key.shift ?: key.legend.uppercase()
                    ShiftState.Caps -> key.caps ?: key.shift ?: key.legend.uppercase()
                }

                Text(
                    legend,
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 24.sp,
                )
                if (key.actions.up != null && key.actions.up.text != null) {
                    val up = when (kbController.shifted) {
                        ShiftState.Off -> key.actions.up.text
                        ShiftState.Shift -> key.actions.up.shift ?: key.actions.up.text.uppercase()
                        ShiftState.Caps -> key.actions.up.caps ?: key.actions.up.shift
                        ?: key.actions.up.text.uppercase()
                    }
                    Text(
                        up,
                        modifier = Modifier.align(TopCenter),
                        fontWeight = FontWeight.Thin,
                        fontSize = 14.sp
                    )
                }
                if (key.actions.down != null && key.actions.down.text != null) {
                    val down = when (kbController.shifted) {
                        ShiftState.Off -> key.actions.down.text
                        ShiftState.Shift -> key.actions.down.shift
                            ?: key.actions.down.text.uppercase()

                        ShiftState.Caps -> key.actions.down.caps ?: key.actions.down.shift
                        ?: key.actions.down.text.uppercase()
                    }
                    Text(
                        down,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        fontSize = 14.sp
                    )
                }
                if (key.actions.left != null && key.actions.left.text != null) {
                    val left = when (kbController.shifted) {
                        ShiftState.Off -> key.actions.left.text
                        ShiftState.Shift -> key.actions.left.shift
                            ?: key.actions.left.text.uppercase()

                        ShiftState.Caps -> key.actions.left.caps ?: key.actions.left.shift
                        ?: key.actions.left.text.uppercase()
                    }

                    Text(
                        left,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp),
                        fontSize = 14.sp
                    )
                }
                if (key.actions.right != null && key.actions.right.text != null) {
                    val right = when (kbController.shifted) {
                        ShiftState.Off -> key.actions.right.text
                        ShiftState.Shift -> key.actions.right.shift
                            ?: key.actions.right.text.uppercase()

                        ShiftState.Caps -> key.actions.right.caps ?: key.actions.right.shift
                        ?: key.actions.right.text.uppercase()
                    }

                    Text(
                        right,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardGrid(kbController: KBController) {
    val tomlFile = LocalContext.current.assets.open("layouts/alpha.toml")
    val layout = loadTOML(tomlFile)

    Box(
        Modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .padding(bottom = 60.dp)
        ) {
            layout.keys.chunked(5).forEach { rowKeys ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = spacedBy(4.dp)
                ) {
                    rowKeys.forEach { key ->
                        Column(Modifier.weight(1f)) {
                            KeyButton(key, kbController)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun KeyboardGridPreview() {
    val kbController = KBController({}, rememberCoroutineScope())
    KeyboardRoot(kbController)
}

fun loadTOML(file: InputStream): Layout {
    val mapper = tomlMapper {  }
    val layout = mapper.decode<Layout>(file)

    return layout
}