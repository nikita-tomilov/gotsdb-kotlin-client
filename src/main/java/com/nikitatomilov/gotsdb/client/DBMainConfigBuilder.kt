package com.nikitatomilov.gotsdb.client

import java.time.Duration

class DBMainConfigBuilder(
  private var clientPort: Int = 5300,
  private var clusterPort: Int = 5123,
  private var rootDir: String = "/tmp/gotsdb",
  private var tsdbImpl: String = "bbolt",
  private var clusterMode: String = "single-instance",
  private var clusterNodesString: String = "localhost:5123;localhost:5124",
  private var performExpirationEvery: Duration = Duration.ofSeconds(10)
) {

  fun contents() = """
    grpc.listenAddress=0.0.0.0:$clientPort
    kvs.engine=file
    kvs.fileKVSPath=$rootDir/kvs
    tss.engine=$tsdbImpl
    tss.filePath=$rootDir/tss
    tss.performExpirationEveryMs=${performExpirationEvery.toMillis()}
    server.mode=$clusterMode
    #cluster, single-instance
    cluster.listenAddress=localhost:$clusterPort
    cluster.knownNodes=$clusterNodesString
    cluster.readingConsistency=none
    #none, any, all
    cluster.writingConsistency=none
    """.trimIndent()
}