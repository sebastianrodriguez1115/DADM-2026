package com.edu.unal.directorio.domain

/** Name error codes. Texts live in string resources. */
enum class NameError { Empty, TooLong }

/** Email error codes. */
enum class EmailError { Empty, Invalid }

/** Website error codes. */
enum class UrlError { Invalid }

/** Phone error codes. */
enum class PhoneError { Invalid }

/** Products and services error codes. */
enum class ProductsError { TooLong }

/** Classification error codes. */
enum class ClassificationError { NoneSelected }

/** One optional error code per field; `null` means the field is valid. */
data class ValidationErrors(
    val name: NameError? = null,
    val email: EmailError? = null,
    val url: UrlError? = null,
    val phone: PhoneError? = null,
    val products: ProductsError? = null,
    val classification: ClassificationError? = null,
) {
    val isValid: Boolean
        get() = name == null && email == null && url == null && phone == null &&
            products == null && classification == null
}

/**
 * Outcome of [CompanyValidator.validate]: the [draft] with every text field trimmed (ready to
 * save when [isValid]) and all [errors] at once.
 */
data class ValidationResult(
    val draft: CompanyDraft,
    val errors: ValidationErrors,
) {
    val isValid: Boolean get() = errors.isValid
}

/**
 * Pure field validation (rules in design D5). Uniqueness is NOT checked here: the repository does.
 */
object CompanyValidator {
    const val NAME_MAX = 100
    const val EMAIL_MAX = 254
    const val URL_MAX = 200
    const val PHONE_MIN_DIGITS = 7
    const val PHONE_MAX_DIGITS = 15
    const val PRODUCTS_MAX = 1000

    private val emailRegex =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    private val urlRegex = Regex(
        "^(https?://)?([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}(:[0-9]{1,5})?(/\\S*)?$",
        RegexOption.IGNORE_CASE,
    )
    private val phoneRegex = Regex("^\\+?[0-9 ().-]+$")

    /**
     * Trims every text field (internal line breaks of products are kept) and validates all
     * fields, reporting every error at once. The trimmed values are in [ValidationResult.draft].
     */
    fun validate(draft: CompanyDraft): ValidationResult {
        val trimmed = draft.copy(
            name = draft.name.trim(),
            email = draft.email.trim(),
            url = draft.url.trim(),
            phone = draft.phone.trim(),
            products = draft.products.trim(),
        )
        val errors = ValidationErrors(
            name = nameError(trimmed.name),
            email = emailError(trimmed.email),
            url = urlError(trimmed.url),
            phone = phoneError(trimmed.phone),
            products = if (trimmed.products.length > PRODUCTS_MAX) ProductsError.TooLong else null,
            classification =
                if (trimmed.classifications.isEmpty()) ClassificationError.NoneSelected else null,
        )
        return ValidationResult(trimmed, errors)
    }

    private fun nameError(name: String): NameError? = when {
        name.isEmpty() -> NameError.Empty
        name.length > NAME_MAX -> NameError.TooLong
        else -> null
    }

    private fun emailError(email: String): EmailError? = when {
        email.isEmpty() -> EmailError.Empty
        email.length > EMAIL_MAX || !emailRegex.matches(email) -> EmailError.Invalid
        else -> null
    }

    private fun urlError(url: String): UrlError? = when {
        url.isEmpty() -> null
        url.length > URL_MAX || !urlRegex.matches(url) -> UrlError.Invalid
        else -> null
    }

    private fun phoneError(phone: String): PhoneError? {
        if (phone.isEmpty()) return null
        val digits = phone.count { it in '0'..'9' }
        return if (phoneRegex.matches(phone) && digits in PHONE_MIN_DIGITS..PHONE_MAX_DIGITS) {
            null
        } else {
            PhoneError.Invalid
        }
    }
}
