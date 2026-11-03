package microfinance.model

// Uses fixed loan states to keep status values consistent.
// A String alternative could allow invalid or misspelled values.

// ai-assisted: #29
// why: ChatGPT helped simplify the loan statuses to match the states actually implemented.

enum LoanStatus:
  case Active   // Loan still has an outstanding balance
  case Closed   // Loan has been fully repaid