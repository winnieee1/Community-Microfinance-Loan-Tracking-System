# Community Microfinance Loan Tracking System

## Project Overview

This project is a standalone desktop application developed using Scala 3 and ScalaFX for PRG2104 Object-Oriented Programming. It supports SDG 1: No Poverty by providing a system for community lenders and NGO loan officers to manage borrowers, microfinance loans, repayments, and repayment risks.

## Target Users

- Community lenders
- NGO loan officers
- Microfinance administrators

## Main Features

1. Register and view borrowers
2. Create and manage microfinance loans
3. Record loan repayments
4. Assess borrower repayment risk
5. Display dashboard summaries and reports

## Technology Used

- Scala 3.8.4
- ScalaFX 21
- Java 21
- sbt 1.12.0
- CSV file persistence
- JavaFX CSS

## Implemented Features

- Borrower registration with input validation and CSV persistence
- Emergency Loan and Business Loan creation and management
- Loan repayment recording with outstanding balance calculation
- Rule-based borrower repayment risk assessment
- Dashboard summary reporting for borrowers, loans, loan value and repayments
- Sidebar navigation between Dashboard, Borrowers, Loans and Repayments
- Persistent borrower, loan and repayment records
- Error handling using `Either`, `Option` and `Try`
- Responsive JavaFX CSS styling

## Data Persistence

The application stores its records in CSV files located in:

```text
src/main/resources/data/
├── borrowers.csv
├── loans.csv
└── repayments.csv
```

Borrower, loan and repayment records remain available after the application is closed and restarted.

`LoanAccount` is not stored separately. Its current state is reconstructed from the saved loan and repayment records to reduce the risk of inconsistent balance data.

## OOP Design

The project applies several object-oriented programming concepts:

- **Inheritance:** `Borrower` and `LoanOfficer` inherit from the abstract `Person` class.
- **Subtype Polymorphism:** `EmergencyLoan` and `BusinessLoan` implement the sealed `Loan` trait and provide different repayment calculations.
- **Generics:** `Repository[T]` provides a reusable repository structure for different domain models.
- **Encapsulation:** Shared contact information is protected in the `Person` class.
- **Immutability:** Domain models use immutable values instead of mutable state.
- **Safe Error Handling:** `Either`, `Option` and `Try` are used to handle validation, missing values and file operations safely.

## Run Instructions

### Requirements

- Java 21
- sbt 1.12.0
- Internet connection for the initial dependency download

### Run Using sbt

1. Open a terminal in the project root directory.
2. Compile the project:

```bash
sbt clean compile
```

3. Run the application:

```bash
sbt run
```

4. The Community Microfinance Loan Tracking System window will open.

### Run Using IntelliJ IDEA

1. Open `Community` in IntelliJ IDEA.
2. Wait for the sbt project and dependencies to load.
3. Open `MainApp.scala`.
4. Run `MainApp`.

## Third-Party Libraries and Licenses

### Scala 3.8.4

- License: Apache License 2.0
- Official website: https://www.scala-lang.org/

### ScalaFX 21.0.0-R32

- License: BSD 3-Clause License
- Official repository: https://github.com/scalafx/scalafx

### JavaFX / OpenJFX 21

- License: GNU General Public License Version 2 with the Classpath Exception
- Official website: https://openjfx.io/

### sbt 1.12.0

- License: Apache License 2.0
- Official website: https://www.scala-sbt.org/

Full citation and license details are also documented in `docs/citations.md`.
