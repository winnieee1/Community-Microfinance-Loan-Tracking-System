package microfinance.model

import java.time.LocalDate

// Represents one repayment transaction for a loan.
// A case class is suitable for simple immutable repayment records.
// normal class, case class convenient

final case class Repayment(
                            repaymentId: String,
                            loanId: String,
                            amountPaid: Double,
                            paymentDate: LocalDate,
                            paymentMethod: String
                          )