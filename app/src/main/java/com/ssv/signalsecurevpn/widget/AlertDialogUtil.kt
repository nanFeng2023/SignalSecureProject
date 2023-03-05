package com.ssv.signalsecurevpn.widget

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

class AlertDialogUtil {
    fun createDialog(
        context: Context,
        title: String?,
        message: String?,
        positiveButtonMsg: String?,
        positiveButtonListener: DialogInterface.OnClickListener?,
        negativeButtonMsg: String?,
        negativeButtonListener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(positiveButtonMsg, positiveButtonListener)
        builder.setNegativeButton(negativeButtonMsg, negativeButtonListener)
        builder.setCancelable(false)
        builder.create().show()
    }
}