package com.afrchat.app.data.model

/** Enveloppe standard pour les résultats de repository : gestion centralisée des erreurs. */
sealed class AfrResult<out T> {
    data class Success<T>(val data: T) : AfrResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : AfrResult<Nothing>()
    object Loading : AfrResult<Nothing>()
}
