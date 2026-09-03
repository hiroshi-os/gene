package com.gene.app.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.asDate(): String = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(this))
fun Long.asFullDate(): String = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(this))

fun Long.asWhatsAppTime(): String {
    if (this <= 0L) return ""
    val now = java.util.Calendar.getInstance()
    val time = java.util.Calendar.getInstance().apply { timeInMillis = this@asWhatsAppTime }

    val isSameYear = now.get(java.util.Calendar.YEAR) == time.get(java.util.Calendar.YEAR)
    val dayDiff = now.get(java.util.Calendar.DAY_OF_YEAR) - time.get(java.util.Calendar.DAY_OF_YEAR)

    if (isSameYear && dayDiff == 0) {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(this))
    }

    if (isSameYear && dayDiff == 1) {
        return "Yesterday"
    }

    val elapsedDays = (now.timeInMillis - this) / (1000 * 60 * 60 * 24)
    if (elapsedDays in 0..6) {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(this))
    }

    return if (isSameYear) {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(this))
    } else {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(this))
    }
}
