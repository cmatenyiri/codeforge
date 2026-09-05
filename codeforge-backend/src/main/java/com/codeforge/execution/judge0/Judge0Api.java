package com.codeforge.execution.judge0;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * The Judge0 wire format.
 *
 * <p>Everything crossing the boundary is base64-encoded ({@code base64_encoded=true}).
 * That is not paranoia: source code and program output are arbitrary bytes, and
 * the plain-text mode of the API mangles anything that is not valid UTF-8.
 */
final class Judge0Api {

    private Judge0Api() {}

    /**
     * @param sourceCode base64 of the complete program
     * @param stdin base64 of the input for this case
     * @param compilerOptions extra compiler flags, omitted when null
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Submission(
            @JsonProperty("language_id") int languageId,
            @JsonProperty("source_code") String sourceCode,
            @JsonProperty("stdin") String stdin,
            @JsonProperty("compiler_options") String compilerOptions,
            @JsonProperty("cpu_time_limit") double cpuTimeLimit,
            @JsonProperty("wall_time_limit") double wallTimeLimit,
            @JsonProperty("memory_limit") int memoryLimit) {}

    record BatchRequest(@JsonProperty("submissions") List<Submission> submissions) {}

    /** A create response entry: a token on success, or a field-keyed error map. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CreatedToken(@JsonProperty("token") String token) {}

    /**
     * @param time seconds as a decimal string, e.g. {@code "0.392"}; null if it never ran
     * @param memory peak memory in KB; null if it never ran
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(
            @JsonProperty("token") String token,
            @JsonProperty("status") Status status,
            @JsonProperty("stdout") String stdout,
            @JsonProperty("stderr") String stderr,
            @JsonProperty("compile_output") String compileOutput,
            @JsonProperty("message") String message,
            @JsonProperty("time") String time,
            @JsonProperty("memory") Integer memory) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Status(@JsonProperty("id") int id, @JsonProperty("description") String description) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BatchResults(@JsonProperty("submissions") List<Result> submissions) {}
}
