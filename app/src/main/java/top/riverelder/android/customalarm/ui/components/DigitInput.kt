package top.riverelder.android.customalarm.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import top.riverelder.android.customalarm.ignoreThrowable
import java.lang.Long.parseLong

@Composable
public fun DigitInput(
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    var stringValue by remember { mutableStateOf(value.toString()) }

    fun onStringValueChange(value: String) {
        stringValue = value
        ignoreThrowable { onValueChange(parseLong(stringValue)) }
    }

    fun onFinal() {
        val finalValue = try { parseLong(stringValue) } catch (ignored: Exception) { value }
        stringValue = finalValue.toString()
        onValueChange(finalValue)
    }

    BasicTextField(
        value = stringValue,
        onValueChange = { onStringValueChange(it) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        enabled = enabled,
        readOnly = false,
        textStyle = textStyle,
        keyboardActions = KeyboardActions { onFinal() },
        singleLine = true,
        maxLines = 1,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox,
    )
}