package com.nikitatomilov.gotsdb.client

import com.google.common.collect.Range
import com.google.common.collect.RangeSet
import com.google.common.collect.TreeRangeSet
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import mu.KLogging
import proto.GoTSDBGrpc
import proto.Rpc
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class Client(
  private val address: String,
  private val port: Int
) {

  private lateinit var rpc: GoTSDBGrpc.GoTSDBFutureStub

  private val messageId = AtomicInteger(0)

  fun connect() {
    val channel = ManagedChannelBuilder.forAddress(address, port)
        .usePlaintext()
        .disableRetry()
        .keepAliveWithoutCalls(true)
        .build()
    rpc = GoTSDBGrpc.newFutureStub(channel)
    logger.warn { "Connection succeeded" }
  }

  //TODO: async operations
  //https://grpc.io/blog/optimizing-grpc-part-1/

  fun save(key: ByteArray, value: ByteArray) {
    val responseFuture = rpc.kvsSave(
        Rpc.KvsStoreRequest.newBuilder()
            .setMsgId(messageId.incrementAndGet())
            .setKey(ByteString.copyFrom(key))
            .setValue(ByteString.copyFrom(value))
            .build())
    responseFuture.get()
  }

  fun retrieve(key: ByteArray): ByteArray {
    val responseFuture = rpc.kvsRetrieve(
        Rpc.KvsRetrieveRequest.newBuilder()
            .setMsgId(messageId.incrementAndGet())
            .setKey(ByteString.copyFrom(key))
            .build())
    return responseFuture.get().value.toByteArray()
  }

  fun keyExists(key: ByteArray): Boolean {
    val responseFuture = rpc.kvsKeyExists(
        Rpc.KvsKeyExistsRequest.newBuilder()
            .setMsgId(messageId.incrementAndGet())
            .setKey(ByteString.copyFrom(key))
            .build())
    return responseFuture.get().exists
  }

  fun deleteKey(key: ByteArray) {
    val responseFuture = rpc.kvsDelete(
        Rpc.KvsDeleteRequest.newBuilder()
            .setMsgId(messageId.incrementAndGet())
            .setKey(ByteString.copyFrom(key))
            .build())
    responseFuture.get()
  }

  fun listKeys(): List<ByteArray> {
    val responseFuture =
        rpc.kvsGetKeys(
            Rpc.KvsAllKeysRequest.newBuilder()
                .setMsgId(messageId.incrementAndGet())
                .build())
    return responseFuture.get().keysList.map { it.toByteArray() }
  }

  fun save(dataSource: String, data: Map<String, Map<Long, Double>>, expiration: Duration) {
    val responseFuture = rpc.tSSave(
        Rpc.TSStoreRequest.newBuilder()
            .setMsgId(messageId.incrementAndGet())
            .setDataSource(dataSource)
            .putAllValues(data.toRpcData())
            .setExpirationMillis(expiration.toMillis())
            .build())
    responseFuture.get()
  }

  fun retrieve(
    dataSource: String,
    tags: Set<String>,
    domain: Range<Instant>
  ): Map<String, Map<Long, Double>> {
    val responseFuture = rpc.tSRetrieve(
        Rpc.TSRetrieveRequest.newBuilder()
            .setMsgId(messageId.incrementAndGet())
            .setDataSource(dataSource)
            .addAllTags(tags)
            .setFromTimestamp(domain.lowerEndpoint().toEpochMilli())
            .setToTimestamp(domain.upperEndpoint().toEpochMilli())
            .build())
    return responseFuture.get().valuesMap.map { it.key to it.value.pointsMap }.toMap()
  }

  fun availability(
    dataSource: String,
    domain: Range<Instant>
  ): RangeSet<Instant> {
    val responseFuture = rpc.tSAvailability(
        Rpc.TSAvailabilityRequest.newBuilder()
            .setMsgId(messageId.incrementAndGet())
            .setDataSource(dataSource)
            .setFromTimestamp(domain.lowerEndpoint().toEpochMilli())
            .setToTimestamp(domain.upperEndpoint().toEpochMilli())
            .build())
    return TreeRangeSet.create(responseFuture.get().availabilityList.map {
      Range.closed(
          Instant.ofEpochMilli(
              it.fromTimestamp), Instant.ofEpochMilli(it.toTimestamp))
    })
  }

  private fun Map<String, Map<Long, Double>>.toRpcData(): Map<String, Rpc.TSPoints> {
    return this.map {
      it.key to Rpc.TSPoints.newBuilder()
          .putAllPoints(it.value)
          .build()
    }.toMap()
  }

  companion object : KLogging()
}