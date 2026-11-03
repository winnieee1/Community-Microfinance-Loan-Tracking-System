package microfinance.util

// Centralises common input validation so different Views can reuse it.
// Keeping validation here avoids repeating the same checks in each View.

// ai-assisted: #10
// why: ChatGPT helped centralise reusable input validation using Either.

object Validation:

  // Check that a required text field is not empty.
  def required(
                input: String,
                fieldName: String
              ): Either[String, String] =

    // Remove extra spaces before checking the input.
    val cleanedInput = input.trim

    if cleanedInput.nonEmpty then
      Right(cleanedInput)
    else
      Left(s"$fieldName cannot be empty.")


  // Convert input to Double and require a value greater than zero.
  def positiveDouble(
                      input: String,
                      fieldName: String
                    ): Either[String, Double] =

    // Safely attempt String -> Double conversion.
    input.trim.toDoubleOption match

      case Some(value) if value > 0 =>
        Right(value)

      // Conversion succeeded, but the value is zero or negative.
      case Some(_) =>
        Left(s"$fieldName must be greater than zero.")

      // Input cannot be converted to Double.
      case None =>
        Left(s"$fieldName must be a valid number.")


  // Convert input to Int and require a value greater than zero.
  def positiveInt(
                   input: String,
                   fieldName: String
                 ): Either[String, Int] =

    input.trim.toIntOption match

      case Some(value) if value > 0 =>
        Right(value)

      // Valid integer, but zero or negative.
      case Some(_) =>
        Left(s"$fieldName must be greater than zero.")

      // Input cannot be converted to Int.
      case None =>
        Left(s"$fieldName must be a whole number.")


  // Convert input to Int and allow zero, but reject negative values.
  def nonNegativeInt(
                      input: String,
                      fieldName: String
                    ): Either[String, Int] =

    input.trim.toIntOption match

      case Some(value) if value >= 0 =>
        Right(value)

      // Valid integer, but negative.
      case Some(_) =>
        Left(s"$fieldName cannot be negative.")

      // Input cannot be converted to Int.
      case None =>
        Left(s"$fieldName must be a whole number.")