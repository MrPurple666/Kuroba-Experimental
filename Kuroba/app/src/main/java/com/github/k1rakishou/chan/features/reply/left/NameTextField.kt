package com.github.k1rakishou.chan.features.reply.left

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.sp
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.features.reply.data.ReplyLayoutState
import com.github.k1rakishou.chan.ui.compose.components.KurobaComposeTextFieldV2
import com.github.k1rakishou.chan.ui.compose.components.KurobaLabelText
import com.github.k1rakishou.chan.ui.compose.ktu

@Composable
internal fun NameTextField(
  replyLayoutState: ReplyLayoutState,
  replyLayoutEnabled: Boolean,
  onMoveFocus: () -> Unit
) {
  val textStyle = remember { TextStyle(fontSize = 16.sp) }
  val labelText = stringResource(id = R.string.reply_name)

  val nameState = replyLayoutState.nameTextState

  KurobaComposeTextFieldV2(
    modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
    enabled = replyLayoutEnabled,
    state = nameState,
    textStyle = textStyle,
    lineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions = KeyboardOptions(
      capitalization = KeyboardCapitalization.Sentences,
      imeAction = ImeAction.Next
    ),
    onKeyboardAction = { onMoveFocus() },
    label = { interactionSource ->
      KurobaLabelText(
        enabled = replyLayoutEnabled,
        labelText = labelText,
        fontSize = 12.ktu,
        interactionSource = interactionSource
      )
    }
  )
}