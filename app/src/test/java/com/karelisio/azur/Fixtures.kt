package com.karelisio.azur

object Fixtures {
    fun read(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)) { "fixture $name absente" }.readText()
}
