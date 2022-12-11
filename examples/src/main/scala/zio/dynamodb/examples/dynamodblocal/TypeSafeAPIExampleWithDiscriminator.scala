package zio.dynamodb.examples.dynamodblocal

import zio.dynamodb.examples.dynamodblocal.DynamoDB._
import zio.dynamodb.Annotations.discriminator
import zio.dynamodb.DynamoDBQuery._
import zio.dynamodb._
import zio.dynamodb.examples.dynamodblocal.TypeSafeAPIExampleWithDiscriminator.TrafficLight.{ Amber, Box, Green }
import zio.schema.DeriveSchema
import zio.{ App, ExitCode, URIO, ZIO }

object TypeSafeAPIExampleWithDiscriminator extends App {

  @discriminator("light_type")
  sealed trait TrafficLight

  object TrafficLight {
    final case class Green(rgb: Int) extends TrafficLight

    object Green {
      implicit val schema = DeriveSchema.gen[Green]
      val rgb             = ProjectionExpression.accessors[Green]
    }

    final case class Red(rgb: Int) extends TrafficLight

    object Red {
      implicit val schema = DeriveSchema.gen[Red]
      val rgb             = ProjectionExpression.accessors[Red]
    }

    final case class Amber(rgb: Int) extends TrafficLight

    object Amber {
      implicit val schema = DeriveSchema.gen[Amber]
      val rgb             = ProjectionExpression.accessors[Amber]
    }

    final case class Box(id: Int, code: Int, trafficLightColour: TrafficLight)

    object Box {
      implicit val schema                = DeriveSchema.gen[Box]
      val (id, code, trafficLightColour) = ProjectionExpression.accessors[Box]
    }

    implicit val schema     = DeriveSchema.gen[TrafficLight]
    val (amber, green, red) = ProjectionExpression.accessors[TrafficLight]
  }

  private val program = for {
    _         <- createTable("box", KeySchema("id", "code"), BillingMode.PayPerRequest)(
                   AttributeDefinition.attrDefnNumber("id"),
                   AttributeDefinition.attrDefnNumber("code")
                 ).execute
    boxOfGreen = Box(1, 1, Green(1))
    boxOfAmber = Box(1, 2, Amber(1))
    _         <- put[Box]("box", boxOfGreen).execute
    _         <- put[Box]("box", boxOfAmber).execute
    query      = queryAll[Box]("box")
                   .whereKey(Box.id === 1)
                   .filter(Box.trafficLightColour >>> TrafficLight.green >>> Green.rgb === 1)
    stream    <- query.execute
    list      <- stream.runCollect
    _         <- ZIO.debug(s"boxes=$list")
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = program.provideCustomLayer(layer).exitCode
}
