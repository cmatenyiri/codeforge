package com.codeforge.validation;

import com.codeforge.domain.Avatar;
import com.codeforge.web.dto.user.ChangeAvatarRequest;
import org.springframework.stereotype.Component;

/** Validates that the chosen avatar is one this build actually offers. */
@Component
public class ChangeAvatarRequestValidator {

    /**
     * @return the chosen avatar
     * @throws com.codeforge.exception.ValidationException if it is missing or not in the catalogue
     */
    public Avatar validate(ChangeAvatarRequest request) {
        ValidationErrors errors = new ValidationErrors();

        if (ValidationRules.isBlank(request.avatar())) {
            errors.add("avatar", "validation.avatar.required", "Choose an avatar");
            errors.throwIfAny();
        }

        return ValidationRules.parseAvatar(request.avatar()).orElseGet(() -> {
            errors.add("avatar", "validation.avatar.unknown", "That avatar is not available");
            errors.throwIfAny();
            throw new IllegalStateException("unreachable: throwIfAny always throws here");
        });
    }
}
