package never.ending.splendor.app.utils

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

val View.layoutInflator get() = context.layoutInflater

@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.inflate(@LayoutRes layout: Int, attach: Boolean = false): T =
    layoutInflator.inflate(layout, this, attach) as T
