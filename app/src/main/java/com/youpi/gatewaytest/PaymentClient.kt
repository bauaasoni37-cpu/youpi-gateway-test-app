package com.youpi.gatewaytest

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PaymentClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        "youpi_gateway_test_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getBaseUrl(): String {
        return prefs.getString("base_url", "http://10.0.2.2:6100") ?: "http://10.0.2.2:6100"
    }

    fun setBaseUrl(url: String) {
        prefs.edit().putString("base_url", url.trimEnd('/')).apply()
    }

    fun createPayment(req: CreatePaymentRequest): CreatePaymentResponse {
        val baseUrl = getBaseUrl()
        val url = "$baseUrl/api/v1/payments"

        val bodyJson = JSONObject().apply {
            put("orderId", req.orderId)
            put("amount", req.amount)
            put("currency", req.currency)
            put("method", req.method)
            put("deviceId", "android-gateway-test-${android.os.Build.MODEL}")
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    CreatePaymentResponse(null, null, null, null, null, null, null,
                        "HTTP ${response.code}: $respBody")
                } else {
                    val json = JSONObject(respBody)
                    CreatePaymentResponse(
                        paymentId = json.optString("paymentId", null),
                        orderId = json.optString("orderId", null),
                        amount = json.optString("amount", null),
                        amountPaise = json.optString("amountPaise", null),
                        currency = json.optString("currency", null),
                        status = json.optString("status", null),
                        checkoutUrl = json.optString("checkoutUrl", null),
                        error = null
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            CreatePaymentResponse(null, null, null, null, null, null, null, e.message)
        }
    }

    fun queryPaymentState(paymentId: String): PaymentState {
        val baseUrl = getBaseUrl()
        val url = "$baseUrl/api/v1/checkout/$paymentId/state"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    PaymentState(null, null, null, null, null, null, null, null, null,
                        "HTTP ${response.code}: $respBody")
                } else {
                    val json = JSONObject(respBody)
                    PaymentState(
                        paymentId = json.optString("paymentId", null),
                        orderId = json.optString("orderId", null),
                        amount = json.optString("amount", null),
                        status = json.optString("status", null),
                        checkoutUrl = json.optString("checkoutUrl", null),
                        upiUri = json.optString("upiUri", null),
                        payeeVpa = json.optString("payeeVpa", null),
                        payeeName = json.optString("payeeName", null),
                        timeRemainingSeconds = json.optLong("timeRemainingSeconds", -1),
                        error = null
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PaymentState(null, null, null, null, null, null, null, null, null, e.message)
        }
    }
}
