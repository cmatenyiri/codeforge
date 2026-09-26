package com.codeforge.execution.codegen;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Language;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/**
 * C++17 on GCC 9.2 (the newest C++ compiler Judge0 1.13.1 ships).
 *
 * <p>The stub has the shape C++ algorithm answers conventionally take: a
 * {@code class Solution} with a public member function, containers passed by
 * reference, and {@code <bits/stdc++.h>} plus {@code using namespace std} above
 * it. The harness relies on neither. It includes its own headers and spells out
 * every {@code std::}, so it still compiles when a solver trims that prelude,
 * and it keeps its helpers in one namespace so none of them can collide with a
 * name the solver chose.
 *
 * <p>The judge invokes a bare {@code g++}, which means GNU++14 and no
 * optimisation — hence {@link #compilerOptions()}.
 */
@Component
public class CppSupport implements LanguageSupport {

    @Override
    public Language language() {
        return Language.CPP;
    }

    @Override
    public String compilerOptions() {
        return "-std=c++17 -O2";
    }

    @Override
    public String starterCode(ProblemSignature signature) {
        StringJoiner arguments = new StringJoiner(", ");
        for (ProblemSignature.Parameter parameter : signature.parameters()) {
            arguments.add(parameterType(parameter.type()) + " " + parameter.name());
        }

        return """
                #include <bits/stdc++.h>
                using namespace std;

                class Solution {
                public:
                    %s %s(%s) {
                        // Write your solution here
                        %s
                    }
                };
                """
                .formatted(
                        unqualified(typeName(signature.returnType())),
                        signature.functionName(),
                        arguments.toString(),
                        placeholderReturn(signature.returnType()));
    }

    @Override
    public String buildProgram(String sourceCode, ProblemSignature signature) {
        return sourceCode + "\n\n" + harness(signature);
    }

    private String harness(ProblemSignature signature) {
        StringBuilder body = new StringBuilder();
        StringJoiner arguments = new StringJoiner(", ");

        for (int i = 0; i < signature.parameters().size(); i++) {
            ProblemSignature.Parameter parameter = signature.parameters().get(i);
            body.append("    %s %s = codeforge_harness::%s(codeforge_harness::line(codeforge_lines, %d));%n"
                    .formatted(typeName(parameter.type()), parameter.name(), parseFunction(parameter.type()), i));
            arguments.add(parameter.name());
        }

        // Two habits of competitive C++ are allowed for below: `#define endl '\n'`
        // is why the answer is not ended with std::endl, and one-letter macros
        // such as `#define F first` are why no template parameter is one letter.
        return """
                // ── CodeForge harness (generated) ─────────────────────────────────────
                // Reads one line per parameter from stdin, calls Solution, and prints the
                // answer last so your own output stays visible above it.
                #include <cctype>
                #include <cstddef>
                #include <cstdio>
                #include <iostream>
                #include <stdexcept>
                #include <string>
                #include <vector>

                namespace codeforge_harness {

                const int SCALE = %d;

                std::vector<std::string> read_lines() {
                    std::vector<std::string> lines;
                    for (std::string read; std::getline(std::cin, read);) {
                        lines.push_back(read);
                    }
                    return lines;
                }

                std::string line(const std::vector<std::string>& lines, std::size_t index) {
                    return index < lines.size() ? lines[index] : std::string();
                }

                std::string trim(const std::string& s) {
                    std::size_t start = 0;
                    std::size_t end = s.size();
                    while (start < end && std::isspace(static_cast<unsigned char>(s[start]))) {
                        start++;
                    }
                    while (end > start && std::isspace(static_cast<unsigned char>(s[end - 1]))) {
                        end--;
                    }
                    return s.substr(start, end - start);
                }

                int int_value(const std::string& s) {
                    return std::stoi(trim(s));
                }

                long long long_value(const std::string& s) {
                    return std::stoll(trim(s));
                }

                double double_value(const std::string& s) {
                    return std::stod(trim(s));
                }

                bool bool_value(const std::string& s) {
                    std::string t = trim(s);
                    for (char& c : t) {
                        c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
                    }
                    return t == "true";
                }

                std::string string_value(const std::string& s) {
                    return s;
                }

                void append_utf8(std::string& out, unsigned long code) {
                    if (code < 0x80) {
                        out += static_cast<char>(code);
                    } else if (code < 0x800) {
                        out += static_cast<char>(0xC0 | (code >> 6));
                        out += static_cast<char>(0x80 | (code & 0x3F));
                    } else if (code < 0x10000) {
                        out += static_cast<char>(0xE0 | (code >> 12));
                        out += static_cast<char>(0x80 | ((code >> 6) & 0x3F));
                        out += static_cast<char>(0x80 | (code & 0x3F));
                    } else {
                        out += static_cast<char>(0xF0 | (code >> 18));
                        out += static_cast<char>(0x80 | ((code >> 12) & 0x3F));
                        out += static_cast<char>(0x80 | ((code >> 6) & 0x3F));
                        out += static_cast<char>(0x80 | (code & 0x3F));
                    }
                }

                // Walks the bracketed lists of the wire format: [1,2], ["a","b"], [[1],[2,3]].
                // Strings are read as JSON string literals, so they decode exactly as they
                // do in the Python and JavaScript harnesses.
                class Cursor {
                  public:
                    explicit Cursor(const std::string& text) : text_(text) {}

                    // Calls each() once per item of the list that starts here.
                    template <typename Each>
                    void list(Each each) {
                        expect('[');
                        skip();
                        if (peek() == ']') {
                            pos_++;
                            return;
                        }
                        while (true) {
                            each();
                            skip();
                            if (peek() != ',') {
                                break;
                            }
                            pos_++;
                        }
                        expect(']');
                    }

                    // A bare item such as a number: everything up to the next comma or bracket.
                    std::string token() {
                        skip();
                        std::size_t start = pos_;
                        while (pos_ < text_.size() && text_[pos_] != ',' && text_[pos_] != ']') {
                            pos_++;
                        }
                        return trim(text_.substr(start, pos_ - start));
                    }

                    std::string quoted() {
                        expect('"');
                        std::string out;
                        while (pos_ < text_.size() && text_[pos_] != '"') {
                            char c = text_[pos_++];
                            if (c != '\\\\' || pos_ >= text_.size()) {
                                out += c;
                                continue;
                            }
                            char escaped = text_[pos_++];
                            switch (escaped) {
                                case 'n': out += '\\n'; break;
                                case 't': out += '\\t'; break;
                                case 'r': out += '\\r'; break;
                                case 'b': out += '\\b'; break;
                                case 'f': out += '\\f'; break;
                                case 'u': {
                                    unsigned long code = hex4();
                                    // A surrogate pair spells one code point above U+FFFF.
                                    if (code >= 0xD800 && code < 0xDC00 && text_.compare(pos_, 2, "\\\\u") == 0) {
                                        pos_ += 2;
                                        code = 0x10000 + ((code - 0xD800) << 10) + (hex4() - 0xDC00);
                                    }
                                    append_utf8(out, code);
                                    break;
                                }
                                // Quote, backslash and slash stand for themselves.
                                default: out += escaped;
                            }
                        }
                        expect('"');
                        return out;
                    }

                  private:
                    const std::string& text_;
                    std::size_t pos_ = 0;

                    char peek() const {
                        return pos_ < text_.size() ? text_[pos_] : '\\0';
                    }

                    void skip() {
                        while (pos_ < text_.size() && std::isspace(static_cast<unsigned char>(text_[pos_]))) {
                            pos_++;
                        }
                    }

                    void expect(char c) {
                        skip();
                        if (peek() != c) {
                            throw std::runtime_error(std::string("malformed input: expected ") + c);
                        }
                        pos_++;
                    }

                    unsigned long hex4() {
                        if (pos_ + 4 > text_.size()) {
                            throw std::runtime_error("malformed input: truncated unicode escape");
                        }
                        unsigned long code = std::stoul(text_.substr(pos_, 4), nullptr, 16);
                        pos_ += 4;
                        return code;
                    }
                };

                // An empty line reads as an empty list, as it does in every other harness.
                template <typename Item, typename Read>
                std::vector<Item> read_list(const std::string& s, Read read) {
                    std::vector<Item> out;
                    if (trim(s).empty()) {
                        return out;
                    }
                    Cursor cursor(s);
                    cursor.list([&] { out.push_back(read(cursor)); });
                    return out;
                }

                std::vector<int> int_array(const std::string& s) {
                    return read_list<int>(s, [](Cursor& c) { return std::stoi(c.token()); });
                }

                std::vector<long long> long_array(const std::string& s) {
                    return read_list<long long>(s, [](Cursor& c) { return std::stoll(c.token()); });
                }

                std::vector<double> double_array(const std::string& s) {
                    return read_list<double>(s, [](Cursor& c) { return std::stod(c.token()); });
                }

                std::vector<std::string> string_array(const std::string& s) {
                    return read_list<std::string>(s, [](Cursor& c) { return c.quoted(); });
                }

                std::vector<std::vector<int>> int_matrix(const std::string& s) {
                    return read_list<std::vector<int>>(s, [](Cursor& c) {
                        std::vector<int> row;
                        c.list([&] { row.push_back(std::stoi(c.token())); });
                        return row;
                    });
                }

                // Rendering is chosen by the problem's declared return type, not by the
                // runtime value: a DOUBLE answer that happens to be whole still has to
                // print as 2.00000 rather than 2.
                std::string render(int v) {
                    return std::to_string(v);
                }

                std::string render(long long v) {
                    return std::to_string(v);
                }

                std::string render(bool v) {
                    return v ? "true" : "false";
                }

                std::string render(double v) {
                    char buffer[400];
                    std::snprintf(buffer, sizeof buffer, "%%.*f", SCALE, v);
                    return buffer;
                }

                std::string render(const std::string& v) {
                    return v;
                }

                // A JSON string literal, escaped the way JSON.stringify escapes one.
                std::string quote(const std::string& v) {
                    std::string out = "\\"";
                    for (char c : v) {
                        switch (c) {
                            case '"': out += "\\\\\\""; break;
                            case '\\\\': out += "\\\\\\\\"; break;
                            case '\\n': out += "\\\\n"; break;
                            case '\\r': out += "\\\\r"; break;
                            case '\\t': out += "\\\\t"; break;
                            case '\\b': out += "\\\\b"; break;
                            case '\\f': out += "\\\\f"; break;
                            default:
                                if (static_cast<unsigned char>(c) < 0x20) {
                                    char escaped[8];
                                    std::snprintf(escaped, sizeof escaped, "\\\\u%%04x", static_cast<unsigned>(c));
                                    out += escaped;
                                } else {
                                    out += c;
                                }
                        }
                    }
                    return out + "\\"";
                }

                template <typename Item, typename Render>
                std::string join(const std::vector<Item>& values, Render item) {
                    std::string out = "[";
                    for (std::size_t i = 0; i < values.size(); i++) {
                        if (i > 0) {
                            out += ',';
                        }
                        out += item(values[i]);
                    }
                    return out + "]";
                }

                std::string render(const std::vector<int>& v) {
                    return join(v, [](int x) { return render(x); });
                }

                std::string render(const std::vector<long long>& v) {
                    return join(v, [](long long x) { return render(x); });
                }

                std::string render(const std::vector<double>& v) {
                    return join(v, [](double x) { return render(x); });
                }

                std::string render(const std::vector<std::string>& v) {
                    return join(v, [](const std::string& x) { return quote(x); });
                }

                std::string render(const std::vector<std::vector<int>>& v) {
                    return join(v, [](const std::vector<int>& row) { return render(row); });
                }

                }  // namespace codeforge_harness

                int main() {
                    const std::vector<std::string> codeforge_lines = codeforge_harness::read_lines();
                %s
                    %s codeforge_answer = Solution().%s(%s);
                    std::cout << codeforge_harness::render(codeforge_answer) << '\\n';
                    return 0;
                }
                """
                .formatted(
                        DataFormat.DOUBLE_SCALE,
                        body.toString().stripTrailing(),
                        typeName(signature.returnType()),
                        signature.functionName(),
                        arguments.toString());
    }

    /** As the harness spells it; the stub drops the {@code std::} via {@link #unqualified}. */
    private static String typeName(DataType type) {
        return switch (type) {
            case INT -> "int";
            case LONG -> "long long";
            case DOUBLE -> "double";
            case BOOLEAN -> "bool";
            case STRING -> "std::string";
            case INT_ARRAY -> "std::vector<int>";
            case LONG_ARRAY -> "std::vector<long long>";
            case DOUBLE_ARRAY -> "std::vector<double>";
            case STRING_ARRAY -> "std::vector<std::string>";
            case INT_MATRIX -> "std::vector<std::vector<int>>";
        };
    }

    /** The stub sits under {@code using namespace std}, so it reads without the prefix. */
    private static String unqualified(String typeName) {
        return typeName.replace("std::", "");
    }

    /** Containers by reference, as C++ answers conventionally take them; scalars and strings by value. */
    private static String parameterType(DataType type) {
        String name = unqualified(typeName(type));
        return switch (type) {
            case INT_ARRAY, LONG_ARRAY, DOUBLE_ARRAY, STRING_ARRAY, INT_MATRIX -> name + "&";
            case INT, LONG, DOUBLE, BOOLEAN, STRING -> name;
        };
    }

    private static String parseFunction(DataType type) {
        return switch (type) {
            case INT -> "int_value";
            case LONG -> "long_value";
            case DOUBLE -> "double_value";
            case BOOLEAN -> "bool_value";
            case STRING -> "string_value";
            case INT_ARRAY -> "int_array";
            case LONG_ARRAY -> "long_array";
            case DOUBLE_ARRAY -> "double_array";
            case STRING_ARRAY -> "string_array";
            case INT_MATRIX -> "int_matrix";
        };
    }

    /**
     * An untouched stub still has to run cleanly. C++ compiles a value-returning
     * function that falls off its end, but calling it is undefined behaviour —
     * under {@code -O2} that can be a crash rather than a wrong answer.
     */
    private static String placeholderReturn(DataType type) {
        return switch (type) {
            case INT, LONG -> "return 0;";
            case DOUBLE -> "return 0.0;";
            case BOOLEAN -> "return false;";
            case STRING -> "return \"\";";
            case INT_ARRAY, LONG_ARRAY, DOUBLE_ARRAY, STRING_ARRAY, INT_MATRIX -> "return {};";
        };
    }
}
