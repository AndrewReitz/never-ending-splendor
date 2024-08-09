package nes.app.utils

import com.google.common.truth.Truth.assertThat
import nes.app.util.toSimpleFormat
import org.junit.Test
import java.util.GregorianCalendar

class DateKtTest {
    @Test
    fun shouldFormatDate() {
        val data = GregorianCalendar(1999, 11, 31).time
        val result = data.toSimpleFormat()
        assertThat(result).isEqualTo("1999.12.31")
    }
}
