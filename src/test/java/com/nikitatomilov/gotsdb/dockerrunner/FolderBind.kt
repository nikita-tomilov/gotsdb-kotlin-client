package com.nikitatomilov.gotsdb.dockerrunner

import com.github.dockerjava.api.model.Bind

data class FolderBind(
  val hostPath: String,
  val containerPath: String
) {
  fun toBinding(): Bind = Bind.parse("$hostPath:$containerPath")
}