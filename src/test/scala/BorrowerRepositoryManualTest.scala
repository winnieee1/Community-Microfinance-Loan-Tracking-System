package microfinance

import microfinance.model.Borrower
import microfinance.repository.BorrowerCsvRepository

object BorrowerRepositoryManualTest:

  def main(_args: Array[String]): Unit =
    val repository =
      new BorrowerCsvRepository

    val borrower =
      new Borrower(
        "B001",
        "Test Borrower",
        "0123456789",
        1800.0,
        2,
        "Employed"
      )

    repository.save(borrower) match
      case Left(errorMessage) =>
        println(s"Save failed: $errorMessage")

      case Right(_) =>
        println("Borrower saved successfully.")

        repository.findAll match
          case Left(errorMessage) =>
            println(s"Read failed: $errorMessage")

          case Right(borrowers) =>
            borrowers.foreach { savedBorrower =>
              println(
                s"${savedBorrower.personId}, " +
                  s"${savedBorrower.fullName}, " +
                  s"${savedBorrower.monthlyIncome}"
              )
            }