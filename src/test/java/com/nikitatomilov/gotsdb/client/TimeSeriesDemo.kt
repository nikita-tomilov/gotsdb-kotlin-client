package com.nikitatomilov.gotsdb.client

import com.google.common.collect.Range
import mu.KLogging
import java.lang.Exception
import java.time.Duration
import java.time.Instant

object TimeSeriesDemo : KLogging() {
  @JvmStatic
  fun main(args: Array<String>) {
    val client = Client("127.0.0.1", 5300)
    client.connect()

    val datasource = "GoTsDbClientDemo"
    var availability = client.availability(datasource, Range.closed(Instant.EPOCH, Instant.now()))

    if (availability.isEmpty) {
      logger.warn { "Seems that there is no data... Lets save some" }
      val data = buildDummyData()
      client.save(datasource, data, Duration.ofSeconds(0))
      logger.warn { "Saved some" }
      availability = client.availability(datasource, Range.closed(Instant.EPOCH, Instant.now()))
    }

    logger.warn { "Available data for" }
    availability.asRanges().forEach {
      logger.warn { " - ${it.lowerEndpoint()} to ${it.upperEndpoint()}" }
    }

    val c = "cchannel0"
    val retrieved = client.retrieve(datasource, setOf(c), availability.asRanges().first())
    logger.warn { "Data for $c has ${retrieved[c]?.size ?: -1} points" }
  }

  private fun buildDummyData(): Map<String, Map<Long, Double>> {
    return (0 until 10)
        .map { "cchannel$it" }
        .associateWith {
          val to = Instant.now().toEpochMilli() - 10000L
          val from = to - 60 * 60 * 1000
          (from until to step 1000).map { it to (it - from) * 0.00001 }.toMap()
        }
  }
}