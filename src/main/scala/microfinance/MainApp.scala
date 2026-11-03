package microfinance

import microfinance.view.{BorrowerView, DashboardView, LoanView, RepaymentView}
import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.layout.{BorderPane, VBox}
import scalafx.scene.control.{Button, Label, ScrollPane}

// Main entry point of the application.
// Creates the main Views and connects them using sidebar navigation.
// A fixed sidebar keeps navigation simple;
// an alternative would be a top menu navigation.

object MainApp extends JFXApp3:

  // Start and build the main application window.
  override def start(): Unit =

    // Create the Dashboard screen.
    val dashboardView =
      new DashboardView

    // Dashboard uses ScrollPane so larger content can be scrolled.
    val dashboardScroll =
      new ScrollPane:
        content = dashboardView
        fitToWidth = true
        hbarPolicy = ScrollPane.ScrollBarPolicy.Never
        styleClass += "content-scroll"

    // Create the other main screens.
    val borrowerView =
      new BorrowerView

    val loanView =
      new LoanView

    val repaymentView =
      new RepaymentView

    // Main content area; Dashboard is shown first.
    val contentPane =
      new BorderPane:
        center = dashboardScroll

    // Sidebar navigation buttons.
    val dashboardButton =
      new Button("Dashboard"):
        maxWidth = Double.MaxValue

    val borrowerButton =
      new Button("Borrowers"):
        maxWidth = Double.MaxValue

    val loanButton =
      new Button("Loans"):
        maxWidth = Double.MaxValue

    val repaymentButton =
      new Button("Repayments"):
        maxWidth = Double.MaxValue

    // Apply the same CSS style to all navigation buttons.
    Seq(
      dashboardButton,
      borrowerButton,
      loanButton,
      repaymentButton
    ).foreach { button =>
      button.styleClass += "sidebar-button"
    }

    // ai-assisted: #16
    // why: ChatGPT helped connect the initial screens through sidebar navigation.

    // Switch the center content back to Dashboard.
    dashboardButton.onAction = handle {
      contentPane.center = dashboardScroll
    }

    // Switch the center content to Borrower Management.
    borrowerButton.onAction = handle {
      contentPane.center = borrowerView
    }

    // ai-assisted: #17
    // why: ChatGPT helped add the Loan Management screen to the existing navigation.

    // Switch the center content to Loan Management.
    loanButton.onAction = handle {
      contentPane.center = loanView
    }

    // ai-assisted: #21
    // why: ChatGPT helped add Repayment Management to the sidebar navigation.

    // Switch the center content to Repayment Management.
    repaymentButton.onAction = handle {
      contentPane.center = repaymentView
    }

    // Sidebar heading.
    val sidebarTitle =
      new Label("Microfinance"):
        styleClass += "sidebar-title"
        maxWidth = Double.MaxValue

    val navigationLabel =
      new Label("NAVIGATION"):
        styleClass += "nav-section-label"

    // Arrange navigation controls vertically.
    val sidebar =
      new VBox:
        padding = Insets(20)
        spacing = 10

        prefWidth = 220
        minWidth = 220
        maxWidth = 220

        styleClass += "sidebar"

        children = Seq(
          sidebarTitle,
          navigationLabel,
          dashboardButton,
          borrowerButton,
          loanButton,
          repaymentButton
        )

    // Create the main application window.
    stage = new JFXApp3.PrimaryStage:
      title = "Community Microfinance Loan Tracking System"
      width = 1100
      height = 700

      scene = new Scene:

        // Load the external CSS styling.
        stylesheets +=
          getClass
            .getResource("/styles.css")
            .toExternalForm

        // Sidebar stays on the left; selected View appears in the center.
        root = new BorderPane:
          left = sidebar
          center = contentPane