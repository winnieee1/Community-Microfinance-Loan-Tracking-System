package microfinance.repository

import microfinance.model.Borrower

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import scala.jdk.CollectionConverters.*
import scala.util.Try

// ai-assisted: #12
// why: ChatGPT helped implement immutable CSV persistence with safe IO handling.
final class BorrowerCsvRepository extends Repository[Borrower]:

  // Kept private so only this repository controls the borrower data file.
  private val dataFile: Path =
    Paths.get(
      "src",
      "main",
      "resources",
      "data",
      "borrowers.csv"
    )

  private val header =
    "borrowerId,fullName,phoneNumber,monthlyIncome,numberOfDependents,employmentStatus"

  override def findAll: Either[String, List[Borrower]] =
    for
      _ <- ensureFileExists()

      lines <- Try(
        Files
          .readAllLines(dataFile, StandardCharsets.UTF_8)
          .asScala
          .drop(1)
          .filter(_.trim.nonEmpty)
          .toList
      ).toEither.left.map(_ => "Unable to read borrower data.")

      borrowers <- decodeAll(lines)
    yield borrowers

  override def findById(
                         id: String
                       ): Either[String, Option[Borrower]] =
    findAll.map(
      _.find(_.personId == id.trim)
    )

  override def save(
                     item: Borrower
                   ): Either[String, Unit] =
    findAll.flatMap { borrowers =>
      val updatedBorrowers =
        if borrowers.exists(_.personId == item.personId) then
          borrowers.map { borrower =>
            if borrower.personId == item.personId then
              item
            else
              borrower
          }
        else
          borrowers :+ item

      writeAll(updatedBorrowers)
    }

  override def delete(
                       id: String
                     ): Either[String, Unit] =
    findAll.flatMap { borrowers =>
      val remainingBorrowers =
        borrowers.filterNot(_.personId == id.trim)

      writeAll(remainingBorrowers)
    }

  private def ensureFileExists(): Either[String, Unit] =
    Try {
      Files.createDirectories(dataFile.getParent)

      if Files.notExists(dataFile) then
        Files.writeString(
          dataFile,
          header + System.lineSeparator(),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE
        )

      ()
    }.toEither.left.map(
      _ => "Unable to create the borrower data file."
    )

  private def writeAll(
                        borrowers: List[Borrower]
                      ): Either[String, Unit] =
    val lines =
      header :: borrowers.map(encode)

    Try {
      Files.write(
        dataFile,
        lines.asJava,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )

      ()
    }.toEither.left.map(
      _ => "Unable to save borrower data."
    )

  private def encode(
                      borrower: Borrower
                    ): String =
    List(
      cleanText(borrower.personId),
      cleanText(borrower.fullName),
      cleanText(borrower.contactNumber),
      borrower.monthlyIncome.toString,
      borrower.numberOfDependents.toString,
      cleanText(borrower.employmentStatus)
    ).mkString(",")

  private def cleanText(
                         text: String
                       ): String =
    text.replace(",", " ").trim

  private def decodeAll(
                         lines: List[String]
                       ): Either[String, List[Borrower]] =
    lines.foldLeft[Either[String, List[Borrower]]](
      Right(Nil)
    ) { (result, line) =>
      for
        borrowers <- result
        borrower <- decode(line)
      yield borrowers :+ borrower
    }

  private def decode(
                      line: String
                    ): Either[String, Borrower] =
    val fields =
      line.split(",", -1).toList.map(_.trim)

    fields match
      case borrowerId ::
        fullName ::
        phoneNumber ::
        monthlyIncomeText ::
        dependentsText ::
        employmentStatus ::
        Nil =>

        for
          monthlyIncome <-
            monthlyIncomeText
              .toDoubleOption
              .toRight(
                s"Invalid monthly income in row: $line"
              )

          numberOfDependents <-
            dependentsText
              .toIntOption
              .toRight(
                s"Invalid number of dependents in row: $line"
              )

        yield new Borrower(
          borrowerId,
          fullName,
          phoneNumber,
          monthlyIncome,
          numberOfDependents,
          employmentStatus
        )

      case _ =>
        Left(s"Invalid borrower row: $line")