package com.youpi.gatewaytest

data class CreatePaymentRequest(
    val orderId: String,
    val amount: String,
    val currency: String = "INR",
    val method: String = "UPI"
)

data class CreatePaymentResponse(
    val paymentId: String?,
    val orderId: String?,
    val amount: String?,
    val amountPaise: String?,
    val currency: String?,
    val status: String?,
    val checkoutUrl: String?,
    val error: String?
)

data class PaymentState(
    val paymentId: String?,
    val orderId: String?,
    val amount: String?,
    val status: String?,
    val checkoutUrl: String?,
    val upiUri: String?,
    val payeeVpa: String?,
    val payeeName: String?,
    val timeRemainingSeconds: Long?,
    val error: String?
)
