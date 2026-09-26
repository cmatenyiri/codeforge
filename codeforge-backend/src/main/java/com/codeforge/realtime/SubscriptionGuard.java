package com.codeforge.realtime;

import com.codeforge.repository.ContestRepository;
import com.codeforge.repository.InterviewRepository;
import com.codeforge.security.AuthenticatedUser;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Decides what a WebSocket client may do: subscribe to the topics it is
 * entitled to, and nothing else.
 *
 * <p>The HTTP handshake has already been through the security filter chain, so
 * every session belongs to a signed-in user — the JWT cookie authenticated it
 * like any other request, and the session carries that authentication as its
 * principal. What is left is per destination:
 *
 * <ul>
 *   <li>the lobby, to anybody;
 *   <li>a contest, once it is announced — a draft only to an author;
 *   <li>a contest's authoring feed, only to an author;
 *   <li>an interview, only to the person sitting it.
 * </ul>
 *
 * <p>Clients may not SEND at all. Every message on these topics comes from the
 * server; with the broker relayed straight to RabbitMQ, a client frame that got
 * through would reach every other subscriber as if the server had said it.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionGuard implements ChannelInterceptor {

    private final ContestRepository contestRepository;
    private final InterviewRepository interviewRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == StompCommand.SEND) {
            throw new AccessDeniedException("Clients cannot publish");
        }
        if (command == StompCommand.CONNECT) {
            requireUser(accessor.getUser());
        }
        if (command == StompCommand.SUBSCRIBE && !mayRead(requireUser(accessor.getUser()), accessor.getDestination())) {
            throw new AccessDeniedException("Not allowed to subscribe to " + accessor.getDestination());
        }
        return message;
    }

    private boolean mayRead(AuthenticatedUser user, String destination) {
        return switch (Topics.parse(destination)) {
            case Topics.Lobby lobby -> true;
            case Topics.Contest contest ->
                user.isAdmin() || contestRepository.existsByIdAndPublishedTrue(contest.contestId());
            case Topics.AdminContest adminContest -> user.isAdmin();
            case Topics.Interview interview -> interviewRepository.existsByIdAndUserId(interview.interviewId(), user.id());
            case Topics.Unknown unknown -> false;
        };
    }

    /** The user behind a session; see {@link com.codeforge.security.JwtAuthenticationFilter}. */
    private static AuthenticatedUser requireUser(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new AccessDeniedException("Not signed in");
    }
}
