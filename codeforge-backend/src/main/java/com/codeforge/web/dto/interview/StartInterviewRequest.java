package com.codeforge.web.dto.interview;

import com.codeforge.domain.InterviewFormat;

/**
 * Start a mock interview in the given format.
 *
 * <p>Deliberately the only thing a caller may choose. Which problems come up is
 * the server's business — being handed a question you did not pick is most of
 * what separates a mock from practice.
 */
public record StartInterviewRequest(InterviewFormat format) {}
