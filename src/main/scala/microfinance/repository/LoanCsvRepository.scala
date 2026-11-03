package microfinance.repository

import microfinance.model.{BusinessLoan, EmergencyLoan, Loan}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import scala.jdk.CollectionConverters.*
import scala.util.Try

// Stores and manages Loan records using CSV persistence.
// The common Loan type allows both EmergencyLoan and BusinessLoan records.
// CSV keeps persistence simple; a database sqlite could support larger datasets.

// ai-assisted: #19
// why: ChatGPT helped implement CSV persistence for different Loan subtypes using Either and immutable Lists.
final class LoanCsvRepository extends Repository[Loan]:

  // Loan records are stored in the resources/data folder so they remain
  // available after the application is restarted.
  private val dataFile: Path =
    Paths.get(
      "src",
      "main",
      "resources",
      "data",
      "loans.csv"
    )

  // Column structure used when storing loan records.
  private val header =
    "loanId,borrowerId,loanType,principalAmount,interestRate,repaymentMonths,purpose,processingFee"


  // Load all saved loan records from the CSV file.
  override def findAll: Either[String, List[Loan]] =
    ensureFileExists().flatMap { _ =>
      Try {
        Files
          .readAllLines(
            dataFile,
            StandardCharsets.UTF_8
          )
          .asScala
          .toList
          .drop(1)                    // Skip the CSV header.
          .filter(_.trim.nonEmpty)    // Ignore empty rows.
      }.toEither
        .left
        .map(error =>
          s"Unable to read loan records: ${error.getMessage}"
        )
        // Convert CSV rows back into Loan objects.
        .flatMap(decodeAll)
    }


  // Search the loaded loans using the given loan ID.
  override def findById(
                         id: String
                       ): Either[String, Option[Loan]] =
    findAll.map(
      _.find(_.loanId == id.trim)
    )


  // Save a new loan or replace an existing loan with the same ID.
  override def save(
                     item: Loan
                   ): Either[String, Unit] =
    findAll.flatMap { loans =>

      // Existing ID is replaced; otherwise the new loan is appended.
      val updatedLoans =
        if loans.exists(_.loanId == item.loanId) then
          loans.map { existingLoan =>
            if existingLoan.loanId == item.loanId then
              item
            else
              existingLoan
          }
        else
          loans :+ item

      writeAll(updatedLoans)
    }


  // Remove the loan with the matching ID.
  override def delete(
                       id: String
                     ): Either[String, Unit] =
    findAll.flatMap { loans =>

      // Keep all records except the matching loan.
      val updatedLoans =
        loans.filterNot(_.loanId == id.trim)

      writeAll(updatedLoans)
    }


  // Prepare the data folder and CSV file before reading or writing.
  private def ensureFileExists(): Either[String, Unit] =
    Try {
      val parentDirectory =
        dataFile.getParent

      if parentDirectory != null then
        Files.createDirectories(parentDirectory)

      // Create the file with its header if it does not exist.
      if !Files.exists(dataFile) then
        Files.write(
          dataFile,
          List(header).asJava,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE
        )

      ()
    }.toEither.left.map(error =>
      s"Unable to prepare loan data file: ${error.getMessage}"
    )


  // Rewrite the CSV using the latest immutable Loan list.
  private def writeAll(
                        loans: List[Loan]
                      ): Either[String, Unit] =
    Try {
      val lines =
        header :: loans.map(encode)

      Files.write(
        dataFile,
        lines.asJava,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )

      ()
    }.toEither.left.map(error =>
      s"Unable to save loan records: ${error.getMessage}"
    )


  // ai-assisted: #29
  // why: ChatGPT helped improve CSV text handling by removing commas from text fields before saving.

  // Convert each Loan subtype into its CSV representation.
  private def encode(
                      loan: Loan
                    ): String =
    loan match

      // EmergencyLoan includes its emergency reason.
      case emergencyLoan: EmergencyLoan =>
        List(
          emergencyLoan.loanId,
          emergencyLoan.borrowerId,
          "EmergencyLoan",
          emergencyLoan.principalAmount.toString,
          emergencyLoan.interestRate.toString,
          emergencyLoan.repaymentMonths.toString,
          cleanText(emergencyLoan.emergencyReason),
          "0.0"
        ).mkString(",")

      // BusinessLoan includes its business purpose and processing fee.
      case businessLoan: BusinessLoan =>
        List(
          businessLoan.loanId,
          businessLoan.borrowerId,
          "BusinessLoan",
          businessLoan.principalAmount.toString,
          businessLoan.interestRate.toString,
          businessLoan.repaymentMonths.toString,
          cleanText(businessLoan.businessPurpose),
          businessLoan.processingFee.toString
        ).mkString(",")


  // Decode every CSV row and build an immutable List of Loan objects.
  private def decodeAll(
                         lines: List[String]
                       ): Either[String, List[Loan]] =
    lines.foldRight(
      Right(Nil): Either[String, List[Loan]]
    ) { (line, result) =>
      for
        loan <- decode(line)
        loans <- result
      yield loan :: loans
    }


  // Convert one CSV row back into the correct Loan subtype.
  private def decode(
                      line: String
                    ): Either[String, Loan] =

    // Split the row into individual CSV fields.
    val values =
      line.split(",", -1).map(_.trim)

    // A valid loan record must contain eight fields.
    if values.length != 8 then
      Left(
        s"Invalid loan record: $line"
      )
    else
      for

        // Safely convert principal amount from String to Double.
        principalAmount <-
          values(3).toDoubleOption.toRight(
            s"Invalid principal amount in loan ${values(0)}."
          )

        // Safely convert interest rate from String to Double.
        interestRate <-
          values(4).toDoubleOption.toRight(
            s"Invalid interest rate in loan ${values(0)}."
          )

        // Safely convert repayment months from String to Int.
        repaymentMonths <-
          values(5).toIntOption.toRight(
            s"Invalid repayment months in loan ${values(0)}."
          )

        // Reconstruct the correct subtype using the stored loan type.
        loan <-
          values(2) match

            case "EmergencyLoan" =>
              Right(
                EmergencyLoan(
                  values(0),
                  values(1),
                  principalAmount,
                  interestRate,
                  repaymentMonths,
                  values(6)
                )
              )

            case "BusinessLoan" =>
              values(7).toDoubleOption
                .toRight(
                  s"Invalid processing fee in loan ${values(0)}."
                )
                .map { processingFee =>
                  BusinessLoan(
                    values(0),
                    values(1),
                    principalAmount,
                    interestRate,
                    repaymentMonths,
                    values(6),
                    processingFee
                  )
                }

            // Reject loan types that the system does not recognise.
            case unknownType =>
              Left(
                s"Unknown loan type: $unknownType"
              )

      yield loan


  // Remove commas from text fields so they do not break CSV columns.
  private def cleanText(
                         text: String
                       ): String =
    text.replace(",", " ").trim