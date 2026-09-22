package com.todoapp.domain.util

import java.util.UUID

object IdGenerator {
    fun generate(): String = UUID.randomUUID().toString()
}
