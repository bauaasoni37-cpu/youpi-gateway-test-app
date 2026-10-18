package com.youpi.gatewaytest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class GatewayTestActivity : AppCompatActivity() {

    private lateinit var paymentClient: PaymentClient

    private lateinit var etBaseUrl: EditText
    private lateinit var etAmount: EditText
    private lateinit var etOrderId: EditText
    private lateinit var btnPay: Button
    private lateinit var btnCheck: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentClient = PaymentClient(this)

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "YOUPI Gateway Test"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        layout.addView(title)

        etBaseUrl = EditText(this).apply {
            hint = "Gateway URL (e.g. http://192.168.1.100:6100)"
            setText(paymentClient.getBaseUrl())
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        layout.addView(etBaseUrl)

        etOrderId = EditText(this).apply {
            hint = "Order ID (e.g. ORDER-1001)"
            setText("ORDER-GT-${System.currentTimeMillis() % 100000}")
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        layout.addView(etOrderId)

        etAmount = EditText(this).apply {
            hint = "Amount (INR)"
            setText("499.00")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(etAmount)

        btnPay = Button(this).apply {
            text = "Pay Now"
            setOnClickListener { onPayClicked() }
        }
        layout.addView(btnPay)

        btnCheck = Button(this).apply {
            text = "Check Payment Status"
            setOnClickListener { onCheckClicked() }
            isEnabled = false
        }
        layout.addView(btnCheck)

        tvStatus = TextView(this).apply {
            text = "Status: Idle"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        layout.addView(tvStatus)

        tvResult = TextView(this).apply {
            text = ""
            textSize = 14f
            setPadding(0, 0, 0, 16)
            movementMethod = android.text.method.ScrollingMovementMethod()
        }
        layout.addView(tvResult)

        scroll.addView(layout)
        setContentView(scroll)
    }

    private fun onPayClicked() {
        val baseUrl = etBaseUrl.text.toString().trim()
        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "Enter Gateway URL", Toast.LENGTH_SHORT).show()
            return
        }
        paymentClient.setBaseUrl(baseUrl)

        val orderId = etOrderId.text.toString().trim()
        val amount = etAmount.text.toString().trim()
        if (orderId.isEmpty() || amount.isEmpty()) {
            Toast.makeText(this, "Enter Order ID and Amount", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "Status: Creating payment..."
        btnPay.isEnabled = false
        btnCheck.isEnabled = false
        tvResult.text = ""

        Thread {
            val resp = paymentClient.createPayment(CreatePaymentRequest(orderId, amount))
            runOnUiThread {
                btnPay.isEnabled = true
                if (resp.error != null) {
                    tvStatus.text = "Status: FAILED"
                    tvResult.text = "Error: ${resp.error}"
                    Toast.makeText(this, "Payment creation failed: ${resp.error}", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                tvStatus.text = "Status: ${resp.status ?: "UNKNOWN"}"
                btnCheck.isEnabled = resp.paymentId != null

                val sb = StringBuilder()
                sb.append("Payment Created\n")
                sb.append("Payment ID: ${resp.paymentId}\n")
                sb.append("Order ID: ${resp.orderId}\n")
                sb.append("Amount: ${resp.amount} ${resp.currency}\n")
                sb.append("Status: ${resp.status}\n")
                sb.append("Checkout URL: ${resp.checkoutUrl}\n")
                tvResult.text = sb.toString()

                Toast.makeText(this, "Payment created. Status: ${resp.status}", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun onCheckClicked() {
        val paymentId = tvResult.text.toString().split("\n").firstOrNull { it.startsWith("Payment ID:") }
            ?.removePrefix("Payment ID: ")?.trim()
        if (paymentId.isNullOrEmpty()) {
            Toast.makeText(this, "No payment to check", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "Status: Checking..."
        tvResult.append("\n\nChecking status...")

        Thread {
            val state = paymentClient.queryPaymentState(paymentId)
            runOnUiThread {
                if (state.error != null) {
                    tvStatus.text = "Status: CHECK FAILED"
                    tvResult.append("\nError: ${state.error}")
                    return@runOnUiThread
                }

                tvStatus.text = "Status: ${state.status ?: "UNKNOWN"}"
                val sb = StringBuilder(tvResult.text)
                sb.append("\n\nLatest State\n")
                sb.append("Payment ID: ${state.paymentId}\n")
                sb.append("Order ID: ${state.orderId}\n")
                sb.append("Amount: ${state.amount}\n")
                sb.append("Status: ${state.status}\n")
                sb.append("Checkout URL: ${state.checkoutUrl}\n")
                sb.append("Payee VPA: ${state.payeeVpa ?: "-"}\n")
                sb.append("Payee Name: ${state.payeeName ?: "-"}\n")
                sb.append("UPI URI: ${state.upiUri ?: "-"}\n")
                sb.append("Time Remaining: ${state.timeRemainingSeconds}s\n")
                tvResult.text = sb.toString()

                if (state.status == "SUCCESS") {
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#047857"))
                    Toast.makeText(this, "Payment SUCCESS!", Toast.LENGTH_LONG).show()
                } else if (state.status == "FAILED" || state.status == "EXPIRED" || state.status == "CANCELLED") {
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#b91c1c"))
                }

                if (state.checkoutUrl != null) {
                    btnPay.text = "Open Checkout in Browser"
                    btnPay.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.checkoutUrl)))
                    }
                }
            }
        }.start()
    }
}
