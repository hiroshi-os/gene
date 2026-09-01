package com.gene.app.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.asDate(): String = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(this))
fun Long.asFullDate(): String = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(this))
