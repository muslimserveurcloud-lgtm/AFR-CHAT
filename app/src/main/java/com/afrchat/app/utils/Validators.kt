package com.afrchat.app.utils

import android.util.Patterns

/** Validation des champs de formulaire côté client (la validation côté serveur est faite par les Firestore Rules). */
object Validators {
    fun isValidEmail(input: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(input).matches()

    fun isValidPhone(input: String): Boolean = input.length in 8..15 && input.all { it.isDigit() || it == '+' }

    fun isValidEmailOrPhone(input: String): Boolean = isValidEmail(input) || isValidPhone(input)

    fun passwordError(password: String): String? = when {
        password.length < 6 -> "Le mot de passe doit contenir au moins 6 caractères."
        !password.any { it.isDigit() } -> "Le mot de passe doit contenir au moins un chiffre."
        else -> null
    }

    fun nameError(name: String): String? = when {
        name.isBlank() -> "Ce champ est obligatoire."
        name.length < 2 -> "Trop court."
        else -> null
    }
}
