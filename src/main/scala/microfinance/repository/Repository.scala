package microfinance.repository

// Defines common data operations that can be reused by different repositories.
// Generic type T allows the same contract to work with different model types.
// create separate interface data type, duplicate common method

// ai-assisted: #11
// why: ChatGPT helped refine the repository contract so file errors can be returned safely.
trait Repository[T]:

  // Return all stored records.
  def findAll: Either[String, List[T]]

  // Search for one record by ID.
  def findById(
                id: String
              ): Either[String, Option[T]]

  // Save a record; successful operation does not need to return data.
  def save(
            item: T
          ): Either[String, Unit]

  // Delete a record using its ID.
  def delete(
              id: String
            ): Either[String, Unit]