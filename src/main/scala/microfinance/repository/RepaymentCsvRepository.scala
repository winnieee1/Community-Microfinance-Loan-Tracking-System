package microfinance.repository

import microfinance.model.Repayment

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.time.LocalDate
import scala.jdk.CollectionConverters.*
import scala.util.Try

// Stores and manages repayment records using CSV persistence.
// It also supports finding repayments for a specific loan.
// CSV keeps persistence simple; a database could support larger datasets.

// ai-assisted: #22
// why: ChatGPT helped implement safe CSV persistence for repayment records.
final class RepaymentCsvRepository extends Repository[Repayment]:

  // Kept private so only this repository controls the repayment data file.
  private val dataFile: Path =
    Paths.get(
      "src",
      "main",
      "resources",
      "data",
      "repayments.csv"
    )

  // Column structure used when storing repayment records.
  private val header =
    "repaymentId,loanId,amountPaid,paymentDate,paymentMethod"


  // Load all repayment records from the CSV file.
  override def findAll: Either[String, List[Repayment]] =
    for
      // Make sure the data file is available before reading.
      _ <- ensureFileExists()

      // Read the CSV, skip the header and ignore empty rows.
      lines <- Try(
        Files
          .readAllLines(dataFile, StandardCharsets.UTF_8)
          .asScala
          .drop(1)
          .filter(_.trim.nonEmpty)
          .toList
      ).toEither.left.map(
        _ => "Unable to read repayment data."
      )

      // Convert CSV rows back into Repayment objects.
      repayments <- decodeAll(lines)
    yield repayments


  // Search for one repayment using its repayment ID.
  override def findById(
                         id: String
                       ): Either[String, Option[Repayment]] =
    findAll.map(
      _.find(_.repaymentId == id.trim)
    )


  // Save a new repayment or replace an existing repayment with the same ID.
  override def save(
                     item: Repayment
                   ): Either[String, Unit] =
    findAll.flatMap { repayments =>

      // Existing ID is replaced; otherwise the new record is appended.
      val updatedRepayments =
        if repayments.exists(
          _.repaymentId == item.repaymentId
        ) then
          repayments.map { repayment =>
            if repayment.repaymentId == item.repaymentId then
              item
            else
              repayment
          }
        else
          repayments :+ item

      writeAll(updatedRepayments)
    }


  // Remove the repayment with the matching ID.
  override def delete(
                       id: String
                     ): Either[String, Unit] =
    findAll.flatMap { repayments =>

      // Keep every repayment except the matching record.
      val remainingRepayments =
        repayments.filterNot(
          _.repaymentId == id.trim
        )

      writeAll(remainingRepayments)
    }


  // Return all repayment records that belong to one loan.
  def findByLoanId(
                    loanId: String
                  ): Either[String, List[Repayment]] =
    findAll.map(
      _.filter(_.loanId == loanId.trim)
    )


  // Prepare the data folder and CSV file before reading or writing.
  private def ensureFileExists(): Either[String, Unit] =
    Try {
      Files.createDirectories(dataFile.getParent)

      // Create the file with its header if it does not exist.
      if Files.notExists(dataFile) then
        Files.writeString(
          dataFile,
          header + System.lineSeparator(),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE
        )

      ()
    }.toEither.left.map(
      _ => "Unable to create the repayment data file."
    )


  // Rewrite the CSV file using the latest repayment list.
  private def writeAll(
                        repayments: List[Repayment]
                      ): Either[String, Unit] =

    // Convert each Repayment object into a CSV row.
    val lines =
      header :: repayments.map(encode)

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
      _ => "Unable to save repayment data."
    )


  // Convert one Repayment object into a CSV row.
  private def encode(
                      repayment: Repayment
                    ): String =
    List(
      repayment.repaymentId,
      repayment.loanId,
      repayment.amountPaid.toString,
      repayment.paymentDate.toString,
      cleanText(repayment.paymentMethod)
    ).mkString(",")


  // Decode every CSV row and build a List of Repayment objects.
  private def decodeAll(
                         lines: List[String]
                       ): Either[String, List[Repayment]] =
    lines.foldLeft[
      Either[String, List[Repayment]]
    ](Right(Nil)) { (result, line) =>
      for
        repayments <- result
        repayment <- decode(line)
      yield repayments :+ repayment
    }


  // Convert one CSV row back into a Repayment object.
  private def decode(
                      line: String
                    ): Either[String, Repayment] =

    // Split the CSV row into individual fields.
    val fields =
      line.split(",", -1).toList.map(_.trim)

    // Match the expected five repayment fields.
    fields match
      case repaymentId ::
        loanId ::
        amountText ::
        dateText ::
        paymentMethod ::
        Nil =>

        for
          // Safely convert the repayment amount to Double.
          amountPaid <-
            amountText
              .toDoubleOption
              .toRight(
                s"Invalid repayment amount in row: $line"
              )

          // Safely convert the stored date to LocalDate.
          paymentDate <-
            Try(
              LocalDate.parse(dateText)
            ).toEither.left.map(
              _ =>
                s"Invalid repayment date in row: $line"
            )

        // Create the Repayment object after successful conversion.
        yield Repayment(
          repaymentId,
          loanId,
          amountPaid,
          paymentDate,
          paymentMethod
        )

      // Reject rows that do not contain the expected structure.
      case _ =>
        Left(
          s"Invalid repayment row: $line"
        )


  // Remove commas from text so they do not break CSV columns.
  private def cleanText(
                         text: String
                       ): String =
    text.replace(",", " ").trim