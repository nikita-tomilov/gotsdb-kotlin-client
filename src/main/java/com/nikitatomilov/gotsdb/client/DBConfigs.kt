package com.nikitatomilov.gotsdb.client

object DBConfigs {
  const val LOG_FILE_NAME = "log4go.json"
  fun getLogFileContent() = """
      {
        "console": {
          "enable": true,
          "level": "INFO",
          "pattern": "[%D %T] [%S] [%L] %M"
        }
      }
    """.trimIndent()

  const val APP_CONFIG_FILE_NAME = "app.properties"
}