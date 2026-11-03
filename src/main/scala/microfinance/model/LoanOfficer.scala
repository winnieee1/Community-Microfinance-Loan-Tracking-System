package microfinance.model

// Represents loan approval staff and reuses common Person information.
// Inheritance avoids repeating shared personal fields and methods.
// standalone class

// ai-assisted: #5
// why: Claude helped create a second Person subtype for loan approval roles.

/** Staff member responsible for reviewing and approving
 * microfinance loan applications.
 */
final class LoanOfficer(
                         officerId: String,
                         officerFullName: String,
                         officerPhoneNumber: String,

                         // Officer-specific information
                         val branchName: String,
                       ) extends Person(
  // Shared Person information
  officerId,
  officerFullName,
  officerPhoneNumber
):

  // Identify the Person subtype
  override def role: String =
    "Loan Officer"

  // Extended contact information for an officer
  override def contactSummary: String =
    s"$fullName ($role, $branchName branch) - $phoneNumber"