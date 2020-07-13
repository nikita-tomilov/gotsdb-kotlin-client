package com.nikitatomilov.gotsdb.client

import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import proto.GoTSDBGrpc
import proto.Rpc
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class Client(
  private val address: String,
  private val port: Int
) {

  private lateinit var rpc: GoTSDBGrpc.GoTSDBFutureStub

  private val executor = Executors.newSingleThreadExecutor()

  private val messageId = AtomicInteger(0)

  fun connect() {
    val channel = ManagedChannelBuilder.forAddress(address, port)
        .usePlaintext()
        .build()
    rpc = GoTSDBGrpc.newFutureStub(channel)
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
        rpc.kvsGetKeys(Rpc.KvsAllKeysRequest.newBuilder().setMsgId(messageId.incrementAndGet()).build())
    return responseFuture.get().keysList.map { it.toByteArray() }
  }
}