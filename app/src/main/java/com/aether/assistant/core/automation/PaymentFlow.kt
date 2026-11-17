package com.aether.assistant.core.automation

import android.content.Context
import android.content.Intent
import android.net.Uri

data class QrPaymentInfo(val payee: String, val upiId: String, val amount: String?, val raw: String)

/**
 * Opens PhonePe's QR scanner and reads UPI QR data. IMPORTANT:
 * This class NEVER authorises a payment. Final confirmation and UPI PIN/biometric
 * are always performed by the user inside the banking app.
 */
class PaymentFlow(private val context: Context) {

    fun openScanner(): Result<String> = runCatching {
        val pm = context.packageManager
        val launch = pm.getLaunchIntentForPackage(PHONEPE_PACKAGE)
            ?: pm.getLaunchIntentForPackage("com.phonepe.app")
            ?: error("PhonePe is not installed")

        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

        // Try to tap the scanner entry through accessibility if the user enabled it.
        val tapped = AetherAccessibilityService.instance?.let { svc ->
            listOf("Scan", "Scan QR", "Scan any QR").firstNotNullOfOrNull { label ->
                if (svc.clickText(label)) label else null
            }
        }
        if (tapped != null) "Opened PhonePe and tapped \"$tapped\". Complete scanning in the app."
        else "Opened PhonePe. Tap the scanner in the app; Aether will read the QR when it is in view."
    }

    fun parseUpi(raw: String): QrPaymentInfo? {
        if (!raw.startsWith("upi://", ignoreCase = true)) return null
        val uri = Uri.parse(raw)
        return QrPaymentInfo(
            payee = uri.getQueryParameter("pn") ?: "Unknown payee",
            upiId = uri.getQueryParameter("pa") ?: "",
            amount = uri.getQueryParameter("am")?.takeIf { it.isNotBlank() },
            raw = raw
        )
    }

    fun openPlayStoreForPhonePe() {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PHONEPE_PACKAGE"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    companion object {
        const val PHONEPE_PACKAGE = "com.phonepe.app"
    }
}
