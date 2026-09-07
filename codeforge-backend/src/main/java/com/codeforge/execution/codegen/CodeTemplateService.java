package com.codeforge.execution.codegen;

import com.codeforge.domain.Language;
import com.codeforge.domain.Problem;
import com.codeforge.exception.BusinessRuleException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * The entry point to code generation: looks up the right {@link LanguageSupport}
 * and turns a {@link Problem} into starter code or a runnable program.
 *
 * <p>Implementations are injected as a list and indexed by their own
 * {@code language()}, so supporting a fifth language is one new
 * {@code @Component} and nothing else.
 */
@Service
public class CodeTemplateService {

    private final Map<Language, LanguageSupport> byLanguage = new EnumMap<>(Language.class);

    public CodeTemplateService(List<LanguageSupport> supports) {
        supports.forEach(support -> byLanguage.put(support.language(), support));
    }

    /** The languages a solution can actually be written in. */
    public List<Language> supportedLanguages() {
        return List.copyOf(byLanguage.keySet());
    }

    /**
     * Starter code for every supported language.
     *
     * @return an empty map when the problem has no signature authored yet —
     *     the frontend reads that as "not solvable in the editor" and says so,
     *     rather than presenting an editor that could never run
     */
    public Map<Language, String> starterCode(Problem problem) {
        return starterCode(ProblemSignature.from(problem));
    }

    /**
     * The same, from a signature that was read somewhere else.
     *
     * <p>What an interview uses: its signature comes off the snapshot taken when
     * the round began, not off the problem as it stands now.
     *
     * @param signature null when none was authored, which both callers report the
     *     same way — an empty map the frontend reads as "not solvable in the
     *     editor"
     */
    public Map<Language, String> starterCode(ProblemSignature signature) {
        if (signature == null) {
            return Map.of();
        }

        Map<Language, String> starters = new EnumMap<>(Language.class);
        byLanguage.forEach((language, support) -> starters.put(language, support.starterCode(signature)));
        return starters;
    }

    /** The solver's code plus the harness that feeds it, ready to send to the judge. */
    public String buildProgram(Problem problem, Language language, String sourceCode) {
        return buildProgram(ProblemSignature.from(problem), problem.getSlug(), language, sourceCode);
    }

    /**
     * The same, from an already-read signature.
     *
     * @param slug named only so a missing signature can say which problem it was
     *     missing from
     */
    public String buildProgram(
            ProblemSignature signature, String slug, Language language, String sourceCode) {

        return support(language).buildProgram(sourceCode, requireSignature(signature, slug));
    }

    public String compilerOptions(Language language) {
        return support(language).compilerOptions();
    }

    private LanguageSupport support(Language language) {
        LanguageSupport support = byLanguage.get(language);
        if (support == null) {
            throw new BusinessRuleException("error.execution.languageUnsupported", "Unsupported language: " + language);
        }
        return support;
    }

    private static ProblemSignature requireSignature(ProblemSignature signature, String slug) {
        if (signature == null) {
            throw new BusinessRuleException(
                    "error.execution.noSignature", "Problem has no solution signature: " + slug);
        }
        return signature;
    }
}
