package com.tudorc.openair.data.network

class ApiException(
    val code: Int,
    message: String
) : Exception(message) {
    val isServerError: Boolean = code >= 500
}
