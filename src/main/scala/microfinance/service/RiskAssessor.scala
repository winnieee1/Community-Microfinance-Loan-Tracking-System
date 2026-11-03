package microfinance.service

import microfinance.model.{Borrower, Loan}

// Uses borrower and loan information to produce an explainable risk level.
// Rule-based scoring keeps the assessment simple and transparent.
// A machine-learning alternative would require more data and complexity.

// ai-assisted: #24
// why: ChatGPT helped design a simple rule-based repayment risk assessment that uses existing borrower and loan data.

enum RiskLevel:
  case Low
  case Medium
  case High


// Stores the final result of a risk assessment.
final case class RiskAssessment(
                                 level: RiskLevel,
                                 score: Int,
                                 monthlyInstalment: Double,
                                 repaymentRatio: Double
                               )


object RiskAssessor:

  // Assess repayment risk using borrower and loan information.
  def assess(
              borrower: Borrower,
              loan: Loan
            ): Either[String, RiskAssessment] =

    // Income is required for the repayment ratio calculation.
    if borrower.monthlyIncome <= 0 then
      Left(
        "Borrower monthly income must be greater than zero."
      )
    else
      // Monthly instalment must be available before calculating risk.
      loan.calculateMonthlyInstalment
        .toRight(
          "Unable to calculate monthly instalment."
        )
        .map { monthlyInstalment =>

          // Compare the monthly repayment with borrower income.
          val repaymentRatio =
            monthlyInstalment / borrower.monthlyIncome


          // Lower monthly income adds risk.
          val incomeScore =
            if borrower.monthlyIncome < 1500 then
              1
            else
              0


          // More dependents add risk.
          val dependentScore =
            if borrower.numberOfDependents >= 4 then
              1
            else
              0


          // Employment stability affects the risk score.
          val employmentScore =
            borrower.employmentStatus
              .trim
              .toLowerCase match

              case "unemployed" =>
                2

              case "self-employed" =>
                1

              case _ =>
                0


          // Higher repayment burden adds more risk.
          val repaymentScore =
            if repaymentRatio > 0.50 then
              2
            else if repaymentRatio > 0.30 then
              1
            else
              0


          // Combine all risk factors into one score.
          val totalScore =
            incomeScore +
              dependentScore +
              employmentScore +
              repaymentScore


          // Convert the total score into a risk category.
          val riskLevel =
            if totalScore >= 4 then
              RiskLevel.High
            else if totalScore >= 2 then
              RiskLevel.Medium
            else
              RiskLevel.Low


          // Return the complete assessment result.
          RiskAssessment(
            level = riskLevel,
            score = totalScore,
            monthlyInstalment = monthlyInstalment,
            repaymentRatio = repaymentRatio
          )
        }