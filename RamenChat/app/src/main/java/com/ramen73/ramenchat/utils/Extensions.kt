package com.ramen73.ramenchat.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun View.snack(message: String) =
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }

fun Long.toTimeString(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000 -> "ora"
        diff < 3_600_000 -> "${diff / 60_000}m fa"
        diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(this))
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(this))
    }
}

fun Long.toFullTimeString(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(this))
