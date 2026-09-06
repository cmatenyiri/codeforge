package com.codeforge.web.dto.admin;

import com.codeforge.domain.Language;
import java.util.Map;

/**
 * A problem's written solution, as authored.
 *
 * <p>Sent as part of the problem rather than through its own endpoint: an
 * editorial has no life without the problem it explains, and saving both in one
 * request is what keeps a reworked statement and its walkthrough from drifting
 * apart between two saves.
 *
 * @param solutions reference code per language; only the languages the author
 *     actually wrote are keys, and they are what the reader's picker offers
 */
public record EditorialPayload(
        String contentMarkdown, String timeComplexity, String spaceComplexity, Map<Language, String> solutions) {}
