package microfinance.model

// Shared parent for Borrower and LoanOfficer.
// Centralises common ID, name and phone information.
// Separate standalone classes would repeat common fields and methods.

// ai-assisted: #5
// why: Claude helped design a shared abstract parent class for person subtypes.

/** Base type for people registered in the microfinance system.
 * An abstract class centralises the identity and contact information
 * shared by all person subtypes.
 */
abstract class Person(
                       val personId: String,
                       val fullName: String,
                       // Kept protected so subclasses can use the contact number
                       // while external classes cannot access the field directly.
                       protected val phoneNumber: String
                     ):

  // Each Person subtype provides its own role.
  def role: String

  // Public access to the protected phone number.
  def contactNumber: String =
    phoneNumber

  // Common formatted contact information.
  def contactSummary: String =
    s"$fullName ($role) - $phoneNumber"