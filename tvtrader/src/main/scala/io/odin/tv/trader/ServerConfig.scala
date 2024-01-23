package io.odin.tv.trader

import com.typesafe.config.ConfigFactory
import zio.config.ReadError
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config.typesafe.TypesafeConfig
import zio.{Has, Layer}

case class ServerConfig(host: String, port: Int)

object ServerConfig {

  private val configDescriptor = descriptor[ServerConfig]

  def configLayer: Layer[ReadError[String], Has[ServerConfig]] =
    TypesafeConfig.fromTypesafeConfig(ConfigFactory.load().getConfig("server"), configDescriptor)
}
