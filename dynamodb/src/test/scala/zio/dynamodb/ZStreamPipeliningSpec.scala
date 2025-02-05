package zio.dynamodb

import zio.Chunk
import zio.dynamodb.DynamoDBQuery.put
import zio.schema.{ DeriveSchema, Schema }
import zio.stream.ZStream
import zio.test.Assertion.equalTo
import zio.test.{ assert, assertTrue, ZIOSpecDefault }

object ZStreamPipeliningSpec extends ZIOSpecDefault {
  final case class Person(id: Int, name: String)

  object Person {
    implicit val schema: Schema[Person] = DeriveSchema.gen[Person]
  }

  private val people       = (1 to 200).map(i => Person(i, s"name$i")).toList
  private val personStream = ZStream.fromIterable(people)

  override def spec =
    suite("ZStream piplelining suite")(
      test("round trip test") {
        for {
          _           <- TestDynamoDBExecutor.addTable("person", "id")
          _           <- batchWriteFromStream(personStream) { person =>
                           put("person", person)
                         }.runDrain
          xs          <- batchReadFromStream[Any, Person, Person]("person", personStream)(person =>
                           PrimaryKey("id" -> person.id)
                         ).right.runCollect
          actualPeople = xs.toList.map { case (_, p) => p }.collect { case Some(b) => b }
        } yield assert(actualPeople)(equalTo(people))
      },
      test(
        "surfaces successfully found items as Right([A, Some[B]]), not found as Right([A, None]) and decoding errors as a Left"
      ) {
        for {
          _            <- TestDynamoDBExecutor.addTable(
                            "person",
                            "id",
                            PrimaryKey("id" -> 1) -> Item("id" -> 1, "name" -> "Avi"),
                            PrimaryKey("id" -> 2) -> Item("id" -> 2, "boom!" -> "de-serialisation-error-expected")
                          )
          actualPeople <- batchReadFromStream[Any, Person, Person]("person", personStream.take(3))(person =>
                            PrimaryKey("id" -> person.id)
                          ).runCollect
        } yield assertTrue(
          actualPeople == Chunk(
            Right((Person(1, "name1"), Some(Person(1, "Avi")))),
            Left(
              DynamoDBError.DecodingError(message =
                "field 'name' not found in Map(Map(String(id) -> Number(2), String(boom!) -> String(de-serialisation-error-expected)))"
              )
            ),
            Right((Person(3, "name3"), None))
          )
        )
      }
    ).provide(DynamoDBExecutor.test)
}
