package microfinance.model

import java.time.LocalDate

// LoanAccount represents the current repayment state of a loan.
// track loan. validates repayment using applyRepayment
// It is kept immutable and can be reconstructed from loan and repayment records.
// Persisting it separately could duplicate state and cause consistency issues.

final case class LoanAccount(
                              loan: Loan,
                              status: LoanStatus,
                              totalRepaid: Double = 0.0,
                              disbursementDate: Option[LocalDate] = None
                            ) {

  // Remaining amount cannot go below zero.
  def outstandingBalance: Double =
    math.max(0.0, loan.calculateTotalRepayment - totalRepaid)

  // Small tolerance is used for floating-point repayment values.
  def isFullyPaid: Boolean = outstandingBalance < 0.01

  // ai-assisted: #8
  // why: Claude helped refine the immutable repayment validation and loan-status update logic.

  def applyRepayment(amount: Double): Either[String, LoanAccount] =
    // Reject invalid repayment amounts.
    if (amount <= 0)
      Left("Repayment amount must be greater than zero.")

    // Prevent repayment above the remaining balance.
    else if (amount > outstandingBalance)
      Left(f"Repayment cannot exceed the outstanding balance ($outstandingBalance%.2f).")

    else {
      // Create an updated copy instead of modifying the existing account.
      val updated = this.copy(totalRepaid = totalRepaid + amount)

      // Update the status based on the remaining balance.
      Right(
        if (updated.isFullyPaid) updated.copy(status = LoanStatus.Closed)
        else updated.copy(status = LoanStatus.Active)
      )
    }
}