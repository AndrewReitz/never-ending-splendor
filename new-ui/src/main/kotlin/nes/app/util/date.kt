package nes.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SIMPLE_DATE_FORMAT = SimpleDateFormat("yyyy.MM.dd", Locale.US)
private val ALBUM_DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd", Locale.US)
private val YEAR = SimpleDateFormat("yyyy", Locale.US)

fun Date.toSimpleFormat(): String = SIMPLE_DATE_FORMAT.format(this)
fun Date.toAlbumFormat(): String = ALBUM_DATE_FORMAT.format(this)
val Date.yearString get(): String = YEAR.format(this)
