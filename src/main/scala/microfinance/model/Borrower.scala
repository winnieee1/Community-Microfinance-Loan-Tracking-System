package microfinance.model

// Represents a borrower in the system
// Extends Person -> reuses ID, name, phone
//  Inheritance -> reuse common Person fields/methods
// Standalone Borrower -> duplicated common fields/methods


// ai-assisted: #5
// why: Claude helped refine Borrower as an immutable Person subtype.

final class Borrower(
                      borrowerId: String,
                      borrowerFullName: String,
                      borrowerPhoneNumber: String,

                      // Borrower-specific information
                      val monthlyIncome: Double,
                      val numberOfDependents: Int,
                      val employmentStatus: String

                      // Reuse the common Person information
                    ) extends Person(
  borrowerId,
  borrowerFullName,
  borrowerPhoneNumber
):

  // Identify this Person subtype
  override def role: String =
    "Borrower"