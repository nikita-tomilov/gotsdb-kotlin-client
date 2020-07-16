package com.nikitatomilov.gotsdb.client

import mu.KLogging

object ClientTest : KLogging() {
  @JvmStatic
  fun main(args: Array<String>) {
    val client = Client("127.0.0.1", 5300)
    client.connect()

    client.save("TEST_KEY".toByteArray(), "TEST_VALUE".toByteArray())

    val keys = client.listKeys()
    keys.forEach {
      logger.warn { " - ${String(it)}" }
    }
  }
}