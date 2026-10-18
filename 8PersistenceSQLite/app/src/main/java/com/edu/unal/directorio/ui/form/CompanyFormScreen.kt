package com.edu.unal.directorio.ui.form

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.edu.unal.directorio.R
import com.edu.unal.directorio.domain.Classification
import com.edu.unal.directorio.domain.ClassificationError
import com.edu.unal.directorio.domain.EmailError
import com.edu.unal.directorio.domain.NameError
import com.edu.unal.directorio.domain.PhoneError
import com.edu.unal.directorio.domain.ProductsError
import com.edu.unal.directorio.domain.UrlError
import com.edu.unal.directorio.ui.labelRes

const val FORM_NAME_TAG = "form_name"
const val FORM_EMAIL_TAG = "form_email"
const val FORM_URL_TAG = "form_url"
const val FORM_PHONE_TAG = "form_phone"
const val FORM_PRODUCTS_TAG = "form_products"
const val FORM_SAVE_TAG = "form_save"
const val FORM_CANCEL_TAG = "form_cancel"

/** Test tag of the checkbox row of [classification]. */
fun formClassificationTag(classification: Classification) = "form_class_${classification.code}"

// The single place where error codes become text (every code maps to a string resource).
@StringRes
private fun CompanyFormErrors.nameRes(): Int? = when {
    name == NameError.Empty -> R.string.form_error_name_empty
    name == NameError.TooLong -> R.string.form_error_name_too_long
    nameTaken -> R.string.form_error_name_taken
    else -> null
}

@StringRes
private fun CompanyFormErrors.emailRes(): Int? = when {
    email == EmailError.Empty -> R.string.form_error_email_empty
    email == EmailError.Invalid -> R.string.form_error_email_invalid
    emailTaken -> R.string.form_error_email_taken
    else -> null
}

@StringRes
private fun CompanyFormErrors.urlRes(): Int? =
    if (url == UrlError.Invalid) R.string.form_error_website_invalid else null

@StringRes
private fun CompanyFormErrors.phoneRes(): Int? =
    if (phone == PhoneError.Invalid) R.string.form_error_phone_invalid else null

@StringRes
private fun CompanyFormErrors.productsRes(): Int? =
    if (products == ProductsError.TooLong) R.string.form_error_products_too_long else null

@StringRes
private fun CompanyFormErrors.classificationRes(): Int? =
    if (classification == ClassificationError.NoneSelected) {
        R.string.form_error_classification_required
    } else {
        null
    }

/**
 * Create/edit form. State and callbacks come as parameters; there is no ViewModel in here.
 * The column scrolls and keeps clear of the keyboard and of the system bars (design D9).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyFormScreen(
    state: CompanyFormUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onProductsChange: (String) -> Unit,
    onToggleClassification: (Classification) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errors = state.errors
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.form_title_edit else R.string.form_title_create,
                        ),
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormField(
                value = state.name,
                onValueChange = onNameChange,
                label = R.string.form_label_name,
                error = errors.nameRes(),
                tag = FORM_NAME_TAG,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            FormField(
                value = state.email,
                onValueChange = onEmailChange,
                label = R.string.form_label_email,
                error = errors.emailRes(),
                tag = FORM_EMAIL_TAG,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            FormField(
                value = state.url,
                onValueChange = onUrlChange,
                label = R.string.form_label_website,
                error = errors.urlRes(),
                tag = FORM_URL_TAG,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
            )
            FormField(
                value = state.phone,
                onValueChange = onPhoneChange,
                label = R.string.form_label_phone,
                error = errors.phoneRes(),
                tag = FORM_PHONE_TAG,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
            )
            FormField(
                value = state.products,
                onValueChange = onProductsChange,
                label = R.string.form_label_products,
                error = errors.productsRes(),
                tag = FORM_PRODUCTS_TAG,
                singleLine = false,
                minLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
            )

            Text(
                stringResource(R.string.form_section_classification),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Classification.entries.forEach { classification ->
                val checked = classification in state.classifications
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = checked,
                            role = Role.Checkbox,
                            onValueChange = { onToggleClassification(classification) },
                        )
                        .testTag(formClassificationTag(classification)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Text(
                        stringResource(classification.labelRes()),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            errors.classificationRes()?.let {
                Text(
                    stringResource(it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }

            if (state.saveFailed) {
                Text(
                    stringResource(R.string.form_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.testTag(FORM_CANCEL_TAG)) {
                    Text(stringResource(R.string.form_cancel))
                }
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving && !state.isLoading,
                    modifier = Modifier.testTag(FORM_SAVE_TAG),
                ) {
                    Text(stringResource(R.string.form_save))
                }
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes label: Int,
    @StringRes error: Int?,
    tag: String,
    keyboardOptions: KeyboardOptions,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}
