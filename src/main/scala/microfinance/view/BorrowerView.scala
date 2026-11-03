package microfinance.view

import microfinance.model.Borrower
import microfinance.repository.BorrowerCsvRepository
import microfinance.util.Validation
import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.{GridPane, HBox, VBox}

// Manages borrower input, validation, saving and table display.
// Flow: user input -> validation -> Borrower -> repository -> refresh table.
// Keeping this processing in the View keeps the feature simple;
// a separate service class could improve separation of concerns.

// ai-assisted: #14
// why: ChatGPT helped connect the borrower form, validation, CSV repository, and TableView.
final class BorrowerView extends VBox:

  // Repository handles persistent borrower data.
  private val borrowerRepository =
    new BorrowerCsvRepository

  // Observable data displayed by the TableView.
  private val borrowerItems =
    ObservableBuffer.empty[Borrower]

  private val borrowerIdField =
    new TextField:
      promptText = "Borrower ID"

  private val fullNameField =
    new TextField:
      promptText = "Full name"

  private val phoneNumberField =
    new TextField:
      promptText = "Phone number"

  private val monthlyIncomeField =
    new TextField:
      promptText = "Monthly income"

  private val dependentsField =
    new TextField:
      promptText = "Number of dependents"

  // Restricts employment status to predefined choices.
  private val employmentStatusBox =
    new ComboBox[String]:
      items = ObservableBuffer(
        "Employed",
        "Self-employed",
        "Unemployed"
      )
      promptText = "Employment status"

  private val saveButton =
    new Button("Save Borrower"):
      defaultButton = true

  private val clearButton =
    new Button("Clear")

  // Displays the current borrower records.
  private val borrowerTable =
    new TableView[Borrower]:
      items = borrowerItems
      placeholder = new Label("No borrowers registered.")
      prefHeight = 300

      columns ++= Seq(
        new TableColumn[Borrower, String]:
          text = "ID"
          cellValueFactory = data =>
            StringProperty(data.value.personId)
        ,
        new TableColumn[Borrower, String]:
          text = "Full Name"
          cellValueFactory = data =>
            StringProperty(data.value.fullName)
        ,
        new TableColumn[Borrower, String]:
          text = "Phone"
          cellValueFactory = data =>
            StringProperty(data.value.contactNumber)
        ,
        new TableColumn[Borrower, String]:
          text = "Monthly Income"
          cellValueFactory = data =>
            StringProperty(
              f"RM${data.value.monthlyIncome}%.2f"
            )
        ,
        new TableColumn[Borrower, String]:
          text = "Dependents"
          cellValueFactory = data =>
            StringProperty(
              data.value.numberOfDependents.toString
            )
        ,
        new TableColumn[Borrower, String]:
          text = "Employment"
          cellValueFactory = data =>
            StringProperty(data.value.employmentStatus)
      )

  // Arrange the borrower input fields.
  private val borrowerForm =
    new GridPane:
      hgap = 10
      vgap = 10

      add(new Label("Borrower ID:"), 0, 0)
      add(borrowerIdField, 1, 0)

      add(new Label("Full Name:"), 0, 1)
      add(fullNameField, 1, 1)

      add(new Label("Phone Number:"), 0, 2)
      add(phoneNumberField, 1, 2)

      add(new Label("Monthly Income:"), 0, 3)
      add(monthlyIncomeField, 1, 3)

      add(new Label("Dependents:"), 0, 4)
      add(dependentsField, 1, 4)

      add(new Label("Employment Status:"), 0, 5)
      add(employmentStatusBox, 1, 5)

  padding = Insets(20)
  spacing = 15

  children = Seq(
    new Label("Borrower Management"),
    borrowerForm,
    new HBox:
      spacing = 10
      children = Seq(saveButton, clearButton),
      borrowerTable
  )

  // Save button starts validation and persistence.
  saveButton.onAction = handle {
    saveBorrower()
  }

  // Clear button resets all form inputs.
  clearButton.onAction = handle {
    clearForm()
  }

  // Load existing records when the View opens.
  loadBorrowers()

  // Validate all inputs before constructing a Borrower.
  private def validateBorrower(): Either[String, Borrower] =
    for
      borrowerId <-
        Validation.required(
          borrowerIdField.text.value,
          "Borrower ID"
        )

      fullName <-
        Validation.required(
          fullNameField.text.value,
          "Full name"
        )

      phoneNumber <-
        Validation.required(
          phoneNumberField.text.value,
          "Phone number"
        )

      monthlyIncome <-
        Validation.positiveDouble(
          monthlyIncomeField.text.value,
          "Monthly income"
        )

      numberOfDependents <-
        Validation.nonNegativeInt(
          dependentsField.text.value,
          "Number of dependents"
        )

      // Employment status must be selected and non-empty.
      employmentStatus <-
        Option(employmentStatusBox.value.value)
          .map(_.trim)
          .filter(_.nonEmpty)
          .toRight(
            "Employment status cannot be empty."
          )

    // Only create the Borrower when all validation succeeds.
    yield
      new Borrower(
        borrowerId,
        fullName,
        phoneNumber,
        monthlyIncome,
        numberOfDependents,
        employmentStatus
      )

  // Validate -> save -> refresh -> clear.
  private def saveBorrower(): Unit =
    validateBorrower() match

      // Validation failed.
      case Left(errorMessage) =>
        showAlert(
          Alert.AlertType.Error,
          "Invalid Input",
          "Unable to save borrower",
          errorMessage
        )

      // Validation succeeded.
      case Right(borrower) =>
        borrowerRepository.save(borrower) match

          // Repository could not save the record.
          case Left(errorMessage) =>
            showAlert(
              Alert.AlertType.Error,
              "Save Failed",
              "Unable to save borrower",
              errorMessage
            )

          // Save succeeded: reload data and reset the form.
          case Right(_) =>
            loadBorrowers()
            clearForm()

            showAlert(
              Alert.AlertType.Information,
              "Borrower Saved",
              "Borrower record saved successfully",
              s"${borrower.fullName} has been saved."
            )

  // Reload the latest borrower records from the repository.
  private def loadBorrowers(): Unit =
    borrowerRepository.findAll match

      case Left(errorMessage) =>
        showAlert(
          Alert.AlertType.Error,
          "Load Failed",
          "Unable to load borrower records",
          errorMessage
        )

      // Replace the TableView data with the latest records.
      case Right(borrowers) =>
        borrowerItems.clear()
        borrowerItems ++= borrowers

  // Reset all borrower input fields.
  private def clearForm(): Unit =
    borrowerIdField.clear()
    fullNameField.clear()
    phoneNumberField.clear()
    monthlyIncomeField.clear()
    dependentsField.clear()
    employmentStatusBox.selectionModel().clearSelection()
    borrowerIdField.requestFocus()

  // Reusable helper for displaying messages to the user.
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