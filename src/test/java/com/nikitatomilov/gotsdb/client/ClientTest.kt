package com.nikitatomilov.gotsdb.client

object ClientTest {
  @JvmStatic
  fun main(args: Array<String>) {
    val client = Client("127.0.0.1", 5300)
    client.connect()
    val keys = client.listKeys()
    keys.forEach {
      println(" - ${String(it)}")
    }
  }
}