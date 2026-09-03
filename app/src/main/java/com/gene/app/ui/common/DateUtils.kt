package com.gene.app.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.asDate(): String = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(this))
fun Long.asFullDate(): String = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(this))

fun Long.asWhatsAppTime(): String {
    val now = java.util.Calendar.getInstance()
    val time = java.util.Calendar.getInstance().apply { timeInMillis = this@asWhatsAppTime }
    
    val isToday = now.get(java.util.Calendar.YEAR) == time.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == time.get(java.util.Calendar.DAY_OF_YEAR)
    if (isToday) {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(this@asWhatsAppTime))
    }
    
    val isYesterday = now.get(java.util.Calendar.YEAR) == time.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) - time.get(java.util.Calendar.DAY_OF_YEAR) == 1
    if (isYesterday) {
        return "Yesterday"
    }
    
    val diffDays = (now.timeInMillis - time.timeInMillis) / (1000 * 60 * 60 * 24)
    if (diffDays < 7) {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(this@asWhatsAppTime))
    }
    
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(this@asWhatsAppTime))
}
