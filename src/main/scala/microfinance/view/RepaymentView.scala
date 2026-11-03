package microfinance.view

import microfinance.model.{LoanAccount, LoanStatus, Repayment}
import microfinance.repository.{LoanCsvRepository, RepaymentCsvRepository}
import microfinance.util.Validation
import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, VBox}

import java.time.LocalDate

// Design:
// Manages repayment input, validation, loan lookup and repayment records.
// Flow: input -> validate -> find loan -> previous repayments ->
// LoanAccount -> apply repayment -> save -> refresh table.
// Keeping the processing in this View keeps the feature simple;
// a separate service class could improve separation of concerns.

// ai-assisted: #21
// why: ChatGPT helped create the initial Repayment Management screen layout.
final class RepaymentView extends VBox:

  // Repositories provide loan and repayment data.
  private val loanRepository =
    new LoanCsvRepository

  private val repaymentRepository =
    new RepaymentCsvRepository

  // Observable repayment records displayed by the TableView.
  private val repaymentItems =
    ObservableBuffer.empty[Repayment]

  private val repaymentIdField =
    new TextField:
      promptText = "Repayment ID"

  private val loanIdField =
    new TextField:
      promptText = "Loan ID"

  private val amountPaidField =
    new TextField:
      promptText = "Amount paid"

  // Restricts payment method to predefined choices.
  private val paymentMethodBox =
    new ComboBox[String]:
      items = ObservableBuffer(
        "Cash",
        "Bank Transfer",
        "Online Banking"
      )
      promptText = "Payment method"

  private val recordButton =
    new Button("Record Repayment"):
      defaultButton = true

  private val clearButton =
    new Button("Clear")

  // Table columns display each part of a Repayment record.
  private val repaymentIdColumn =
    new TableColumn[Repayment, String]:
      text = "Repayment ID"
      cellValueFactory = data =>
        StringProperty(data.value.repaymentId)

  private val loanIdColumn =
    new TableColumn[Repayment, String]:
      text = "Loan ID"
      cellValueFactory = data =>
        StringProperty(data.value.loanId)

  private val amountColumn =
    new TableColumn[Repayment, String]:
      text = "Amount Paid"
      cellValueFactory = data =>
        StringProperty(
          f"RM${data.value.amountPaid}%.2f"
        )

  private val dateColumn =
    new TableColumn[Repayment, String]:
      text = "Date"
      cellValueFactory = data =>
        StringProperty(
          data.value.paymentDate.toString
        )

  private val methodColumn =
    new TableColumn[Repayment, String]:
      text = "Payment Method"
      cellValueFactory = data =>
        StringProperty(
          data.value.paymentMethod
        )

  // ai-assisted: #23
  // why: ChatGPT helped connect repayment records to a ScalaFX TableView.

  // Display the current repayment records.
  private val repaymentTable =
    new TableView[Repayment]:
      items = repaymentItems
      placeholder =
        new Label("No repayments recorded.")
      prefHeight = 300

      columns ++= Seq(
        repaymentIdColumn,
        loanIdColumn,
        amountColumn,
        dateColumn,
        methodColumn
      )

  // Arrange the repayment input fields.
  private val repaymentForm =
    new GridPane:
      hgap = 10
      vgap = 10

      add(
        new Label("Repayment ID:"),
        0,
        0
      )
      add(
        repaymentIdField,
        1,
        0
      )

      add(
        new Label("Loan ID:"),
        0,
        1
      )
      add(
        loanIdField,
        1,
        1
      )

      add(
        new Label("Amount Paid:"),
        0,
        2
      )
      add(
        amountPaidField,
        1,
        2
      )

      add(
        new Label("Payment Method:"),
        0,
        3
      )
      add(
        paymentMethodBox,
        1,
        3
      )

  padding = Insets(20)
  spacing = 15

  children = Seq(
    new Label("Repayment Management"),
    repaymentForm,
    new HBox:
      spacing = 10
      children = Seq(
        recordButton,
        clearButton
      ),
      repaymentTable
  )

  // Start repayment validation and saving.
  recordButton.onAction = handle {
    recordRepayment()
  }

  // Reset the repayment form.
  clearButton.onAction = handle {
    clearForm()
  }

  // Load existing repayment records when the View opens.
  loadRepayments()

  // ai-assisted: #23
  // why: ChatGPT helped combine validation, loan lookup, previous repayments, and LoanAccount repayment logic.

  // Validate input and calculate the updated repayment state.
  private def validateRepayment()
  : Either[String, (Repayment, LoanAccount)] =
    for

      // Repayment ID cannot be empty.
      repaymentId <-
        Validation.required(
          repaymentIdField.text.value,
          "Repayment ID"
        )

      // Loan ID cannot be empty.
      loanId <-
        Validation.required(
          loanIdField.text.value,
          "Loan ID"
        )

      // Find the selected loan.
      loanOption <-
        loanRepository.findById(
          loanId
        )

      // Convert None into an error if the loan does not exist.
      loan <-
        loanOption.toRight(
          s"Loan ID $loanId does not exist."
        )

      // Repayment amount must be a positive Double.
      amountPaid <-
        Validation.positiveDouble(
          amountPaidField.text.value,
          "Amount paid"
        )

      // Payment method must be selected.
      paymentMethod <-
        Option(
          paymentMethodBox.value.value
        )
          .map(_.trim)
          .filter(_.nonEmpty)
          .toRight(
            "Payment method must be selected."
          )

      // Load all previous repayments for this loan.
      previousRepayments <-
        repaymentRepository.findByLoanId(
          loanId
        )

      // Calculate how much has already been repaid.
      totalPreviouslyPaid =
        previousRepayments
          .map(_.amountPaid)
          .sum

      // Reconstruct the current loan repayment state.
      currentAccount =
        LoanAccount(
          loan = loan,
          status = LoanStatus.Active,
          totalRepaid = totalPreviouslyPaid
        )

      // Validate the new payment and calculate the updated account.
      updatedAccount <-
        currentAccount.applyRepayment(
          amountPaid
        )

      // Create the new repayment transaction.
      repayment =
        Repayment(
          repaymentId = repaymentId,
          loanId = loanId,
          amountPaid = amountPaid,
          paymentDate = LocalDate.now(),
          paymentMethod = paymentMethod
        )

    // Return both the repayment record and updated account state.
    yield (
      repayment,
      updatedAccount
    )

  // Validate -> check duplicate ID -> save -> refresh.
  private def recordRepayment(): Unit =
    validateRepayment() match

      // Repayment validation failed.
      case Left(errorMessage) =>
        showAlert(
          Alert.AlertType.Error,
          "Invalid Repayment",
          "Unable to record repayment",
          errorMessage
        )

      // Validation succeeded.
      case Right(
        (repayment, updatedAccount)
      ) =>

        // Check whether the repayment ID already exists.
        repaymentRepository
          .findById(
            repayment.repaymentId
          ) match

          case Left(errorMessage) =>
            showAlert(
              Alert.AlertType.Error,
              "Check Failed",
              "Unable to check repayment ID",
              errorMessage
            )

          // Reject duplicate repayment IDs.
          case Right(Some(_)) =>
            showAlert(
              Alert.AlertType.Error,
              "Duplicate Repayment ID",
              "Unable to record repayment",
              s"Repayment ID ${repayment.repaymentId} already exists."
            )

          // ID is available, so save the repayment.
          case Right(None) =>
            repaymentRepository
              .save(repayment) match

              case Left(errorMessage) =>
                showAlert(
                  Alert.AlertType.Error,
                  "Save Failed",
                  "Unable to save repayment",
                  errorMessage
                )

              // Save succeeded: refresh, clear and show balance.
              case Right(_) =>
                loadRepayments()
                clearForm()

                showAlert(
                  Alert.AlertType.Information,
                  "Repayment Recorded",
                  "Repayment saved successfully",
                  f"Remaining balance: RM${updatedAccount.outstandingBalance}%.2f"
                )

  // Reload the latest repayment records.
  private def loadRepayments(): Unit =
    repaymentRepository.findAll match

      case Left(errorMessage) =>
        showAlert(
          Alert.AlertType.Error,
          "Load Failed",
          "Unable to load repayment records",
          errorMessage
        )

      // Replace the TableView data with the latest records.
      case Right(repayments) =>
        repaymentItems.clear()
        repaymentItems ++= repayments

  // Reset all repayment input fields.
  private def clearForm(): Unit =
    repaymentIdField.clear()
    loanIdField.clear()
    amountPaidField.clear()
    paymentMethodBox
      .selectionModel()
      .clearSelection()
    repaymentIdField.requestFocus()

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