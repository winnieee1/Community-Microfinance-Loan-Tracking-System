package microfinance.view

import microfinance.model.{BusinessLoan, EmergencyLoan, Loan}
import microfinance.repository.{BorrowerCsvRepository, LoanCsvRepository}
import microfinance.util.Validation
import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, VBox}
// Manages loan input, validation, creation and table display.
// Flow: input -> validate -> check borrower -> create Loan subtype ->
// check duplicate ID -> repository -> refresh table.
// Keeping the processing in this View keeps the feature simple;
// a separate service class could improve separation of concerns.

// ai-assisted: #17
// why: ChatGPT helped create the initial Loan Management screen layout.
final class LoanView extends VBox:

  private val borrowerRepository =
    new BorrowerCsvRepository

  private val loanRepository =
    new LoanCsvRepository

  private val loanItems =
    ObservableBuffer.empty[Loan]

  private val loanIdField =
    new TextField:
      promptText = "Loan ID"

  private val borrowerIdField =
    new TextField:
      promptText = "Borrower ID"

  private val loanTypeBox =
    new ComboBox[String]:
      items = ObservableBuffer(
        "Emergency Loan",
        "Business Loan"
      )
      promptText = "Loan type"

  private val principalAmountField =
    new TextField:
      promptText = "Principal amount"

  private val interestRateField =
    new TextField:
      promptText = "Interest rate"

  private val repaymentMonthsField =
    new TextField:
      promptText = "Repayment months"

  private val purposeField =
    new TextField:
      promptText = "Emergency reason / Business purpose"

  private val processingFeeField =
    new TextField:
      promptText = "Business loan only"

  private val createButton =
    new Button("Create Loan"):
      defaultButton = true

  private val clearButton =
    new Button("Clear")

  private val loanIdColumn =
    new TableColumn[Loan, String]:
      text = "Loan ID"
      prefWidth = 85
      minWidth = 85

      cellValueFactory = data =>
        StringProperty(data.value.loanId)

  private val borrowerIdColumn =
    new TableColumn[Loan, String]:
      text = "Borrower ID"
      prefWidth = 110
      minWidth = 100

      cellValueFactory = data =>
        StringProperty(data.value.borrowerId)

  private val loanTypeColumn =
    new TableColumn[Loan, String]:
      text = "Type"
      prefWidth = 150
      minWidth = 130

      cellValueFactory = data =>
        StringProperty(data.value.loanType)

  private val principalColumn =
    new TableColumn[Loan, String]:
      text = "Principal"
      prefWidth = 110
      minWidth = 100

      cellValueFactory = data =>
        StringProperty(
          f"RM${data.value.principalAmount}%.2f"
        )

  private val interestColumn =
    new TableColumn[Loan, String]:
      text = "Interest"
      prefWidth = 90
      minWidth = 80

      cellValueFactory = data =>
        StringProperty(
          f"${data.value.interestRate}%.2f%%"
        )

  private val monthsColumn =
    new TableColumn[Loan, String]:
      text = "Months"
      prefWidth = 90
      minWidth = 80

      cellValueFactory = data =>
        StringProperty(
          data.value.repaymentMonths.toString
        )

  private val totalRepaymentColumn =
    new TableColumn[Loan, String]:
      text = "Total Repayment"
      prefWidth = 150
      minWidth = 140

      cellValueFactory = data =>
        StringProperty(
          f"RM${data.value.calculateTotalRepayment}%.2f"
        )

  // ai-assisted: #18
  // why: ChatGPT helped connect Loan objects to a ScalaFX TableView.
  private val loanTable =
    new TableView[Loan]:
      items = loanItems
      placeholder = new Label("No loans created.")
      prefHeight = 300
      maxWidth = Double.MaxValue

      columns ++= Seq(
        loanIdColumn,
        borrowerIdColumn,
        loanTypeColumn,
        principalColumn,
        interestColumn,
        monthsColumn,
        totalRepaymentColumn
      )

  private val loanForm =
    new GridPane:
      hgap = 10
      vgap = 10

      add(new Label("Loan ID:"), 0, 0)
      add(loanIdField, 1, 0)

      add(new Label("Borrower ID:"), 0, 1)
      add(borrowerIdField, 1, 1)

      add(new Label("Loan Type:"), 0, 2)
      add(loanTypeBox, 1, 2)

      add(new Label("Principal Amount:"), 0, 3)
      add(principalAmountField, 1, 3)

      add(new Label("Interest Rate (%):"), 0, 4)
      add(interestRateField, 1, 4)

      add(new Label("Repayment Months:"), 0, 5)
      add(repaymentMonthsField, 1, 5)

      add(new Label("Purpose / Reason:"), 0, 6)
      add(purposeField, 1, 6)

      add(new Label("Processing Fee:"), 0, 7)
      add(processingFeeField, 1, 7)

  padding = Insets(20)
  spacing = 15

  children = Seq(
    new Label("Loan Management"),
    loanForm,
    new HBox:
      spacing = 10
      children = Seq(
        createButton,
        clearButton
      ),
      loanTable
  )

  createButton.onAction = handle {
    createLoan()
  }

  clearButton.onAction = handle {
    clearForm()
  }

  loadLoans()

  // ai-assisted: #18
  // why: ChatGPT helped combine validation, borrower lookup, and loan subtype creation.
  private def validateLoan(): Either[String, Loan] =
    for
      loanId <-
        Validation.required(
          loanIdField.text.value,
          "Loan ID"
        )

      borrowerId <-
        Validation.required(
          borrowerIdField.text.value,
          "Borrower ID"
        )

      _ <-
        borrowerRepository.findById(borrowerId).flatMap:
          case Some(_) =>
            Right(())

          case None =>
            Left(
              s"Borrower ID $borrowerId does not exist."
            )

      loanType <-
        Option(loanTypeBox.value.value)
          .map(_.trim)
          .filter(_.nonEmpty)
          .toRight(
            "Loan type must be selected."
          )

      principalAmount <-
        Validation.positiveDouble(
          principalAmountField.text.value,
          "Principal amount"
        )

      interestRate <-
        Validation.positiveDouble(
          interestRateField.text.value,
          "Interest rate"
        )

      repaymentMonths <-
        Validation.positiveInt(
          repaymentMonthsField.text.value,
          "Repayment months"
        )

      purpose <-
        Validation.required(
          purposeField.text.value,
          "Purpose / reason"
        )

      loan <-
        loanType match
          case "Emergency Loan" =>
            Right(
              EmergencyLoan(
                loanId,
                borrowerId,
                principalAmount,
                interestRate,
                repaymentMonths,
                purpose
              )
            )

          case "Business Loan" =>
            Validation
              .positiveDouble(
                processingFeeField.text.value,
                "Processing fee"
              )
              .map { processingFee =>
                BusinessLoan(
                  loanId,
                  borrowerId,
                  principalAmount,
                  interestRate,
                  repaymentMonths,
                  purpose,
                  processingFee
                )
              }

          case _ =>
            Left(
              "Unsupported loan type."
            )

    yield loan

  // ai-assisted: #20
  // why: ChatGPT helped connect loan creation to CSV persistence and duplicate checking.
  private def createLoan(): Unit =
    validateLoan() match
      case Left(errorMessage) =>
        showAlert(
          Alert.AlertType.Error,
          "Invalid Input",
          "Unable to create loan",
          errorMessage
        )

      case Right(loan) =>
        loanRepository.findById(
          loan.loanId
        ) match
          case Left(errorMessage) =>
            showAlert(
              Alert.AlertType.Error,
              "Check Failed",
              "Unable to check loan ID",
              errorMessage
            )

          case Right(Some(_)) =>
            showAlert(
              Alert.AlertType.Error,
              "Duplicate Loan ID",
              "Unable to create loan",
              s"Loan ID ${loan.loanId} already exists."
            )

          case Right(None) =>
            loanRepository.save(loan) match
              case Left(errorMessage) =>
                showAlert(
                  Alert.AlertType.Error,
                  "Save Failed",
                  "Unable to save loan",
                  errorMessage
                )

              case Right(_) =>
                loadLoans()
                clearForm()

                showAlert(
                  Alert.AlertType.Information,
                  "Loan Created",
                  "Loan created successfully",
                  s"${loan.loanType} ${loan.loanId} has been saved."
                )

  private def loadLoans(): Unit =
    loanRepository.findAll match
      case Left(errorMessage) =>
        showAlert(
          Alert.AlertType.Error,
          "Load Failed",
          "Unable to load loan records",
          errorMessage
        )

      case Right(loans) =>
        loanItems.clear()
        loanItems ++= loans

  private def clearForm(): Unit =
    loanIdField.clear()
    borrowerIdField.clear()
    loanTypeBox.selectionModel().clearSelection()
    principalAmountField.clear()
    interestRateField.clear()
    repaymentMonthsField.clear()
    purposeField.clear()
    processingFeeField.clear()
    loanIdField.requestFocus()

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

