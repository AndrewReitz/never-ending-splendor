package never.ending.splendor.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SIMPLE_DATE_FORMAT = SimpleDateFormat("yyyy.MM.dd", Locale.US)

fun Date.toSimpleFormat(): String = SIMPLE_DATE_FORMAT.format(this)
