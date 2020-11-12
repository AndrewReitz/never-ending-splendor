package never.ending.splendor.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.os.Process
import android.util.Base64
import never.ending.splendor.R
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException

/**
 * Validates that the calling package is authorized to browse a
 * [android.service.media.MediaBrowserService].
 *
 * The list of allowed signing certificates and their corresponding package names is defined in
 * res/xml/allowed_media_browser_callers.xml.
 *
 * If you add a new valid caller to allowed_media_browser_callers.xml and you don't know
 * its signature, this class will print to logcat (INFO level) a message with the proper base64
 * version of the caller certificate that has not been validated. You can copy from logcat and
 * paste into allowed_media_browser_callers.xml. Spaces and newlines are ignored.
 */
class PackageValidator(context: Context) {
    /**
     * Map allowed callers' certificate keys to the expected caller information.
     *
     */
    private val mValidCertificates: Map<String, ArrayList<CallerInfo>>
    private fun readValidCertificates(parser: XmlResourceParser): Map<String, ArrayList<CallerInfo>> {
        val validCertificates = HashMap<String, ArrayList<CallerInfo>>()
        try {
            var eventType = parser.next()
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG &&
                    parser.name == "signing_certificate"
                ) {
                    val name = parser.getAttributeValue(null, "name")
                    val packageName = parser.getAttributeValue(null, "package")
                    val isRelease = parser.getAttributeBooleanValue(null, "release", false)
                    val certificate = parser.nextText().replace("\\s|\\n".toRegex(), "")
                    val info = CallerInfo(name, packageName, isRelease)
                    var infos = validCertificates[certificate]
                    if (infos == null) {
                        infos = ArrayList()
                        validCertificates[certificate] = infos
                    }
                    Timber.v(
                        "Adding allowed caller: %s, package=%s release=%s certificate=%s",
                        info.name, info.packageName, info.release, certificate
                    )
                    infos.add(info)
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            Timber.e(e, "Could not read allowed callers from XML.")
        } catch (e: IOException) {
            Timber.e(e, "Could not read allowed callers from XML.")
        }
        return validCertificates
    }

    /**
     * @return false if the caller is not authorized to get data from this MediaBrowserService
     */
    @SuppressLint("BinaryOperationInTimber")
    fun isCallerAllowed(context: Context, callingPackage: String, callingUid: Int): Boolean {
        // Always allow calls from the framework, self app or development environment.
        if (Process.SYSTEM_UID == callingUid || Process.myUid() == callingUid) {
            return true
        }
        val packageManager = context.packageManager
        val packageInfo: PackageInfo
        packageInfo = try {
            packageManager.getPackageInfo(
                callingPackage, PackageManager.GET_SIGNING_CERTIFICATES
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.w(e, "Package manager can't find package: %s", callingPackage)
            return false
        }
        @Suppress("DEPRECATION") // not available until pi 26
        if (packageInfo.signatures.size != 1) {
            Timber.w("Caller has more than one signature certificate!")
            return false
        }
        @Suppress("DEPRECATION") // not available until pi 26
        val signature = Base64.encodeToString(
            packageInfo.signatures[0].toByteArray(), Base64.NO_WRAP
        )

        // Test for known signatures:
        val validCallers = mValidCertificates[signature]
        if (validCallers == null) {
            Timber.v(
                "Signature for caller %s is not valid: \n %s",
                callingPackage, signature
            )
            if (mValidCertificates.isEmpty()) {
                Timber.w(
                    "The list of valid certificates is empty. Either your file " +
                        "res/xml/allowed_media_browser_callers.xml is empty or there was an " +
                        "error while reading it. Check previous log messages."
                )
            }
            return false
        }

        // Check if the package name is valid for the certificate:
        val expectedPackages = StringBuffer()
        for (info in validCallers) {
            if (callingPackage == info.packageName) {
                Timber.v(
                    "Valid caller: %s package=%s release=%s",
                    info.name, info.packageName, info.release
                )
                return true
            }
            expectedPackages.append(info.packageName).append(' ')
        }
        Timber.i(
            """
    Caller has a valid certificate, but its package doesn't match any expected package for the given certificate. Caller's package is %s . Expected packages as defined in res/xml/allowed_media_browser_callers.xml are (%s). This caller's certificate is:
    %s
            """.trimIndent(),
            callingPackage, expectedPackages, signature
        )
        return false
    }

    private class CallerInfo(val name: String, val packageName: String, val release: Boolean)

    init {
        mValidCertificates = readValidCertificates(
            context.resources.getXml(
                R.xml.allowed_media_browser_callers
            )
        )
    }
}
