package com.jc.subscription.module.metrics

import com.jc.subscription.model.config.PrometheusConfig
import zio.ZIO
import zio.metrics.prometheus.Registry
import zio.metrics.prometheus.exporters.Exporters
import zio.metrics.prometheus.helpers.{getCurrentRegistry, http, initializeDefaultExports}
import io.prometheus.client.exporter.{HTTPServer => PrometheusHttpServer}
import eu.timepit.refined.auto._

object PrometheusMetricsExporter {

  def create(config: PrometheusConfig): ZIO[Exporters with Registry, Throwable, PrometheusHttpServer] = {
    for {
      registry <- getCurrentRegistry()
      _ <- initializeDefaultExports(registry)
      prometheusServer <- http(registry, config.port)
    } yield prometheusServer
  }
}
