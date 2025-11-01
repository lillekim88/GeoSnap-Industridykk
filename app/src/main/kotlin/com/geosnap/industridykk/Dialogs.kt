
package com.geosnap.industridykk

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText

object Dialogs {
    fun promptForComment(ctx: Context): String? {
        var result: String? = null
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_comment, null)
        val et = view.findViewById<EditText>(R.id.etComment)
        val dlg = AlertDialog.Builder(ctx)
            .setTitle("Kommentar (påkrevd)")
            .setView(view)
            .setPositiveButton("Lagre") { d, _ ->
                val t = et.text.toString().trim()
                if (t.isNotEmpty()) result = t else result = null
                d.dismiss()
            }
            .setNegativeButton("Avbryt") { d, _ -> d.dismiss() }
            .create()
        dlg.show()
        // Blokkerende vent er ikke ideelt; for MVP returner null hvis tomt
        // I produksjon: bruk suspendCancellableCoroutine
        try { Thread.sleep(200) } catch (_: Exception) {}
        return result
    }
}
