package com.bih.mangosos.domain

object PhoneNumberValidator {
    private val allowedPattern = Regex("^[0-9+()\\-\\s]{3,20}$")

    fun parseContacts(raw: String): List<String> {
        return raw.split(',', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun isValid(number: String): Boolean {
        val contacts = parseContacts(number)
        return contacts.isNotEmpty() && contacts.all { allowedPattern.matches(it) }
    }
}
