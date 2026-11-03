package microfinance.model

// Loan provides the common structure for different loan types.
// EmergencyLoan and BusinessLoan reuse the shared loan fields and behaviour.
// inheritance and polymorphism
// Separate standalone loan classes would duplicate the common structure.

// ai-assisted: #7
// why: ChatGPT helped design the keep loan hierarchy and shared repayment calculation.

sealed trait Loan:

  // Common loan information
  def loanId: String

  def borrowerId: String

  def principalAmount: Double

  // Percentage value: 5.0 represents 5%.
  def interestRate: Double

  def repaymentMonths: Int

  // Implemented differently by each loan subtype
  def loanType: String

  def calculateTotalRepayment: Double

  /** Returns None when the repayment period is invalid. */
  final def calculateMonthlyInstalment: Option[Double] =
    Option.when(repaymentMonths > 0):
      calculateTotalRepayment / repaymentMonths


// Emergency-specific loan information and calculation
final case class EmergencyLoan(
                                override val loanId: String,
                                override val borrowerId: String,
                                override val principalAmount: Double,
                                override val interestRate: Double,
                                override val repaymentMonths: Int,
                                emergencyReason: String
                              ) extends Loan:

  override def loanType: String =
    "Emergency Loan"

  override def calculateTotalRepayment: Double =
    principalAmount * (1 + interestRate / 100.0)


// Business-specific information includes purpose and processing fee
final case class BusinessLoan(
                               override val loanId: String,
                               override val borrowerId: String,
                               override val principalAmount: Double,
                               override val interestRate: Double,
                               override val repaymentMonths: Int,
                               businessPurpose: String,
                               processingFee: Double
                             ) extends Loan:

  override def loanType: String =
    "Business Loan"

  override def calculateTotalRepayment: Double =
    principalAmount * (1 + interestRate / 100.0) + processingFee