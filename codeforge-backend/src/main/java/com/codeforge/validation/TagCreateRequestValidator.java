package com.codeforge.validation;

import com.codeforge.domain.Slugs;
import com.codeforge.repository.TagRepository;
import com.codeforge.web.dto.admin.TagCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates a new topic.
 *
 * <p>Uniqueness is checked on both the name and the slug: "Two Pointers" and
 * "two-pointers" are different names that reduce to the same URL, and letting
 * both exist would give the catalogue two filters that look distinct and are not.
 */
@Component
@RequiredArgsConstructor
public class TagCreateRequestValidator {

    private final TagRepository tagRepository;

    public void validate(TagCreateRequest request) {
        ValidationErrors errors = new ValidationErrors();
        String name = ValidationRules.trimToNull(request.name());

        if (name == null) {
            errors.add("name", "validation.tag.name.required", "A topic name is required");
        } else if (name.length() > ValidationRules.TAG_NAME_MAX_LENGTH) {
            errors.add(
                    "name",
                    "validation.tag.name.length",
                    "Topic name must be at most %d characters".formatted(ValidationRules.TAG_NAME_MAX_LENGTH));
        } else if (Slugs.slugify(name).isEmpty()) {
            errors.add("name", "validation.tag.name.format", "A topic name needs a letter or a digit");
        } else if (tagRepository.existsByNameIgnoreCase(name) || tagRepository.existsBySlug(Slugs.slugify(name))) {
            errors.add("name", "validation.tag.name.taken", "That topic already exists");
        }

        errors.throwIfAny();
    }
}
