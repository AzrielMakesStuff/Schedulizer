package com.ateam.schedulizer

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.jvm.JvmInline

@JvmInline
public value class DecryptedString(private val d : String) {
    override fun toString(): String {
        return d;
    }
}
public value class DecryptedDate(private val t : LocalDateTime) {
    public fun getDate(): LocalDateTime {
        return t;
    }
}
