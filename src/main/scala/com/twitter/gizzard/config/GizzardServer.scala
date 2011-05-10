package com.twitter.gizzard
package config

import com.twitter.logging.Logger
import com.twitter.logging.config.LoggerConfig
import com.twitter.querulous.config.QueryEvaluator
import com.twitter.util._
import com.twitter.util.Duration
import com.twitter.util.TimeConversions._

trait GizzardServer {
  var loggers: List[LoggerConfig] = Nil
  def jobQueues: Map[Int, Scheduler]
  def nameServer: NameServer

  var manager: Manager         = new Manager with TThreadServer
  var jobInjector: JobInjector = new JobInjector with THsHaServer

  var stats: StatsCollection = new StatsCollection { }
}

trait StatsCollection {
  var slowQueryThreshold: Duration = 2.seconds
  var slowQueryLoggerName: String = "slow_query"

  var sampledQueryRate: Double = 0.0
  var sampledQueryLoggerName: String = "sampled_query"

  def apply[T <: AnyRef](name: String, obj: T)(implicit manifest: Manifest[T]): T = {
    val sampledQueryConsumer = new SampledTransactionalStatsConsumer(
      new LoggingTransactionalStatsConsumer(Logger.get(sampledQueryLoggerName)), sampledQueryRate)
    val slowQueryConsumer = new SlowTransactionalStatsConsumer(
      new LoggingTransactionalStatsConsumer(Logger.get(slowQueryLoggerName)), slowQueryThreshold.inMillis)
    proxy.LoggingProxy(Seq(sampledQueryConsumer, slowQueryConsumer), name, obj)
  }
}

trait Manager extends TServer {
  def name = "GizzardManager"
  var port = 7920
}

trait JobInjector extends TServer {
  def name = "JobInjector"
  var port = 7921
}

