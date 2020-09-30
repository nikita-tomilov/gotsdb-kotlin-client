package com.nikitatomilov.gotsdb.dockerrunner

import com.github.dockerjava.api.model.PortBinding

data class PortBind(
  val hostPort: Long,
  val containerPort: Long
) {
  fun toPortBinding(): PortBinding = PortBinding.parse("$hostPort:$containerPort")
}