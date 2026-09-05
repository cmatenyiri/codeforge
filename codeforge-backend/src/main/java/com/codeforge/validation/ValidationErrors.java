package com.codeforge.validation;

import com.codeforge.exception.ValidationException;
import com.codeforge.web.dto.error.ApiFieldError;
import java.util.ArrayList;
import java.util.List;

/**
 * Accumulator used by the validator classes.
 *
 * <p>Collecting every problem before throwing means a form comes back fully
 * annotated in one round trip, instead of the user fixing one field at a time.
 */
public class ValidationErrors {

    private final List<ApiFieldError> errors = new ArrayList<>();

    /** Records a problem against a field. {@code code} is the i18n key the frontend resolves. */
    public ValidationErrors add(String field, String code, String message) {
        errors.add(new ApiFieldError(field, code, message));
        return this;
    }

    /** Records a problem only when {@code condition} holds. */
    public ValidationErrors addIf(boolean condition, String field, String code, String message) {
        if (condition) {
            add(field, code, message);
        }
        return this;
    }

    /** True once {@code field} already has a problem, so cheap checks can short-circuit expensive ones. */
    public boolean hasErrorOn(String field) {
        return errors.stream().anyMatch(error -> error.field().equals(field));
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public List<ApiFieldError> asList() {
        return List.copyOf(errors);
    }

    /** Terminates the validator when anything was collected. */
    public void throwIfAny() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
