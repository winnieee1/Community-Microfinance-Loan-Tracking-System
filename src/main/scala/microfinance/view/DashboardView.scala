package microfinance.view

import microfinance.repository.{
  BorrowerCsvRepository,
  LoanCsvRepository,
  RepaymentCsvRepository
}
import microfinance.service.RiskAssessor
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, VBox}

// Design:
// Combines summary information and repayment risk assessment in one dashboard.
// Repository data provides the summary values and RiskAssessor provides the risk result.
// Keeping both on one screen reduces navigation for the user.

// ai-assisted: #25
// why: ChatGPT helped connect the rule-based risk assessment to the Dashboard UI.
final class DashboardView extends VBox:

  // Repositories provide the latest borrower, loan and repayment data.
  private val borrowerRepository =
    new BorrowerCsvRepository

  private val loanRepository =
    new LoanCsvRepository

  private val repaymentRepository =
    new RepaymentCsvRepository

  // Summary values displayed on the dashboard.
  private val totalBorrowersValue =
    new Label("0"):
      styleClass += "summary-card-value"

  private val totalLoansValue =
    new Label("0"):
      styleClass += "summary-card-value"

  private val totalLoanValue =
    new Label("RM0.00"):
      styleClass += "summary-card-value"

  private val totalRepaymentsValue =
    new Label("RM0.00"):
      styleClass += "summary-card-value"

  // Reusable helper for creating summary cards.
  private def createSummaryCard(
                                 titleText: String,
                                 valueLabel: Label
                               ): VBox =
    val titleLabel =
      new Label(titleText):
        styleClass += "summary-card-title"

    new VBox:
      spacing = 4
      prefWidth = 205
      prefHeight = 65
      styleClass += "summary-card"

      children = Seq(
        titleLabel,
        valueLabel
      )

  private val borrowerCard =
    createSummaryCard(
      "TOTAL BORROWERS",
      totalBorrowersValue
    )

  private val loanCard =
    createSummaryCard(
      "TOTAL LOANS",
      totalLoansValue
    )

  private val loanValueCard =
    createSummaryCard(
      "TOTAL LOAN VALUE",
      totalLoanValue
    )

  private val repaymentsCard =
    createSummaryCard(
      "TOTAL REPAYMENTS",
      totalRepaymentsValue
    )

  private val summaryGrid =
    new GridPane:
      hgap = 12
      vgap = 12

      add(borrowerCard, 0, 0)
      add(loanCard, 1, 0)
      add(loanValueCard, 0, 1)
      add(repaymentsCard, 1, 1)

  // Borrower and loan selections used for risk assessment.
  private val borrowerIdBox =
    new ComboBox[String]:
      promptText = "Select borrower"

  private val loanIdBox =
    new ComboBox[String]:
      promptText = "Select loan"

  private val assessButton =
    new Button("Assess Risk"):
      defaultButton = true

  private val refreshButton =
    new Button("Refresh Data")

  // Risk assessment results.
  private val riskLevelLabel =
    new Label("Risk Level: -"):
      styleClass += "risk-result"

  private val riskScoreLabel =
    new Label("Risk Score: -"):
      styleClass += "risk-result"

  private val instalmentLabel =
    new Label("Monthly Instalment: -"):
      styleClass += "risk-result"

  private val repaymentRatioLabel =
    new Label("Income Used for Repayment: -"):
      styleClass += "risk-result"

  private val assessmentForm =
    new GridPane:
      hgap = 15
      vgap = 12

      add(
        new Label("Borrower ID:"),
        0,
        0
      )

      add(
        borrowerIdBox,
        1,
        0
      )

      add(
        new Label("Loan ID:"),
        0,
        1
      )

      add(
        loanIdBox,
        1,
        1
      )

  private val pageTitle =
    new Label("Dashboard"):
      styleClass += "page-title"

  private val pageSubtitle =
    new Label(
      "Community Microfinance Loan Tracking System"
    ):
      styleClass += "page-subtitle"

  private val summaryTitle =
    new Label("Summary"):
      styleClass += "section-title"

  private val riskTitle =
    new Label("Repayment Risk Assessment"):
      styleClass += "section-title"

  private val riskButtons =
    new HBox:
      spacing = 10
      children = Seq(
        assessButton,
        refreshButton
      )

  private val riskCard =
    new VBox:
      spacing = 10
      styleClass += "content-card"

      children = Seq(
        riskTitle,
        assessmentForm,
        riskButtons,
        riskLevelLabel,
        riskScoreLabel,
        instalmentLabel,
        repaymentRatioLabel
      )

  padding = Insets(20)
  spacing = 12

  children = Seq(
    pageTitle,
    pageSubtitle,
    summaryTitle,
    summaryGrid,
    riskCard
  )

  // Run risk assessment when the user clicks the button.
  assessButton.onAction = handle {
    assessRisk()
  }

  // Reload the latest repository data.
  refreshButton.onAction = handle {
    loadData()
  }

  // Load dashboard information when the View opens.
  loadData()


  // ai-assisted: #26
  // why: ChatGPT helped combine repository data into dashboard summary values.
  private def loadData(): Unit =

    // Load borrower, loan and repayment data together.
    val result =
      for
        borrowers <-
          borrowerRepository.findAll

        loans <-
          loanRepository.findAll

        repayments <-
          repaymentRepository.findAll

      yield (
        borrowers,
        loans,
        repayments
      )

    result match

      // Any repository error is displayed to the user.
      case Left(errorMessage) =>
        showAlert(
          Alert.AlertType.Error,
          "Load Failed",
          "Unable to load dashboard data",
          errorMessage
        )

      case Right(
        (borrowers, loans, repayments)
      ) =>

        // Update the available borrower and loan selections.
        borrowerIdBox.items =
          ObservableBuffer.from(
            borrowers.map(_.personId)
          )

        loanIdBox.items =
          ObservableBuffer.from(
            loans.map(_.loanId)
          )

        // Calculate total principal value of all loans.
        val loanValue =
          loans
            .map(_.principalAmount)
            .sum

        // Calculate total amount of all repayments.
        val repaymentValue =
          repayments
            .map(_.amountPaid)
            .sum

        // Update the four dashboard summary values.
        totalBorrowersValue.text =
          borrowers.size.toString

        totalLoansValue.text =
          loans.size.toString

        totalLoanValue.text =
          f"RM$loanValue%.2f"

        totalRepaymentsValue.text =
          f"RM$repaymentValue%.2f"


  // ai-assisted: #25
  // why: ChatGPT helped validate dashboard selections and connect them to RiskAssessor.
  private def assessRisk(): Unit =

    // Validate selections and find the matching borrower and loan.
    val result =
      for
        borrowerId <-
          Option(
            borrowerIdBox.value.value
          ).toRight(
            "Please select a borrower."
          )

        loanId <-
          Option(
            loanIdBox.value.value
          ).toRight(
            "Please select a loan."
          )

        borrowerOption <-
          borrowerRepository.findById(
            borrowerId
          )

        // Convert None into an error if the borrower does not exist.
        borrower <-
          borrowerOption.toRight(
            s"Borrower ID $borrowerId does not exist."
          )

        loanOption <-
          loanRepository.findById(
            loanId
          )

        // Convert None into an error if the loan does not exist.
        loan <-
          loanOption.toRight(
            s"Loan ID $loanId does not exist."
          )

        // Make sure the selected loan belongs to the selected borrower.
        _ <-
          if loan.borrowerId == borrower.personId then
            Right(())
          else
            Left(
              "The selected loan does not belong to the selected borrower."
            )

        // Pass the valid Borrower and Loan to the risk service.
        assessment <-
          RiskAssessor.assess(
            borrower,
            loan
          )

      yield assessment

    result match

      // Validation or assessment failed.
      case Left(errorMessage) =>
        showAlert(
          Alert.AlertType.Error,
          "Assessment Failed",
          "Unable to assess repayment risk",
          errorMessage
        )

      // Display the successful risk assessment.
      case Right(assessment) =>
        riskLevelLabel.text =
          s"Risk Level: ${assessment.level}"

        riskScoreLabel.text =
          s"Risk Score: ${assessment.score}"

        instalmentLabel.text =
          f"Monthly Instalment: RM${assessment.monthlyInstalment}%.2f"

        repaymentRatioLabel.text =
          f"Income Used for Repayment: ${assessment.repaymentRatio * 100}%.2f%%"


  // Reusable helper for displaying messages.
  private def showAlert(
                         alertType: Alert.AlertType,
                         titleText: String,
                         headerTextValue: String,
                         message: String
                       ): Unit =
    val alert =
      new Alert(alertType):
        title = titleText
        headerText = headerTextValue
        contentText = message

    alert.showAndWait()