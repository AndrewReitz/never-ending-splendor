package com.bayapps.android.robophish.utils

import java.text.SimpleDateFormat
import java.util.*

fun Date.toSimpleFormat(): String {
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    return dateFormat.format(this)
}

fun Date.toStandardDateTimeString(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateFormat.format(this)
}
