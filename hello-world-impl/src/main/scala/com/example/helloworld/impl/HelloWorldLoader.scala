package com.example.helloworld.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraPersistenceComponents, CassandraReadSide, TenantCassandraPersistenceComponents, TenantReadSideCassandraPersistenceComponents}
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.example.helloworld.api.HelloWorldService
import com.example.helloworld.impl.daos.stock.StockDao
import com.lightbend.lagom.internal.scaladsl.persistence.cassandra.{CassandraReadSideImpl, TenantCassandraReadSideImpl}
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.softwaremill.macwire._

class HelloWorldLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new HelloWorldApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HelloWorldApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HelloWorldService])
}

abstract class HelloWorldApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with TenantCassandraPersistenceComponents
    //with CassandraPersistenceComponents
    with LagomKafkaComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[HelloWorldService](wire[HelloWorldServiceImpl])

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = HelloWorldSerializerRegistry

  lazy val stockDao: StockDao = wire[StockDao]

  //readSide.register(wire[StockEventProcessor])
  readSide.register(wire[PortfolioEventProcessor])

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(StockState.typeKey)(
      entityContext => StockBehavior.create(entityContext)
    )
  )

  clusterSharding.init(
    Entity(PortfolioState.typeKey)(
      entityContext => PortfolioBehavior.create(entityContext)
    )
  )

}
