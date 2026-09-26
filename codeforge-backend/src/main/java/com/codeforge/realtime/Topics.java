package com.codeforge.realtime;

/**
 * The STOMP destinations the browser can subscribe to.
 *
 * <p>Every one is a plain topic on the RabbitMQ broker, dot-separated because
 * that is how RabbitMQ reads a routing key. None of them carries data a
 * subscriber could not already read: a message only says that something
 * changed, and the page re-reads it through the REST API, which applies every
 * visibility rule it always has. What may be subscribed to at all is decided in
 * {@link SubscriptionGuard}.
 *
 * <p>Topics rather than per-user queues, deliberately. A topic message reaches
 * every subscriber through the broker whichever instance published it, which is
 * all a second instance needs — per-user destinations would additionally need
 * every instance to know which sessions every other one holds.
 */
public final class Topics {

    /** The lobby: any announced contest changed status or gained a registration. */
    public static final String CONTESTS = "/topic/contests";

    private static final String CONTEST_PREFIX = "/topic/contests.";
    private static final String ADMIN_CONTEST_PREFIX = "/topic/admin.contests.";
    private static final String INTERVIEW_PREFIX = "/topic/interviews.";

    private Topics() {}

    /** One contest: its status, its registrations, its standings. */
    public static String contest(Long contestId) {
        return CONTEST_PREFIX + contestId;
    }

    /** What only an author sees of one contest: how far a rejudge has got. */
    public static String adminContest(Long contestId) {
        return ADMIN_CONTEST_PREFIX + contestId;
    }

    /** One mock interview, for its owner's other tabs. */
    public static String interview(Long interviewId) {
        return INTERVIEW_PREFIX + interviewId;
    }

    /** A destination, read back into what it is about. */
    sealed interface Parsed permits Lobby, Contest, AdminContest, Interview, Unknown {}

    record Lobby() implements Parsed {}

    record Contest(Long contestId) implements Parsed {}

    record AdminContest(Long contestId) implements Parsed {}

    record Interview(Long interviewId) implements Parsed {}

    record Unknown() implements Parsed {}

    static Parsed parse(String destination) {
        if (destination == null) {
            return new Unknown();
        }
        if (destination.equals(CONTESTS)) {
            return new Lobby();
        }
        Long id;
        if ((id = idAfter(destination, CONTEST_PREFIX)) != null) {
            return new Contest(id);
        }
        if ((id = idAfter(destination, ADMIN_CONTEST_PREFIX)) != null) {
            return new AdminContest(id);
        }
        if ((id = idAfter(destination, INTERVIEW_PREFIX)) != null) {
            return new Interview(id);
        }
        return new Unknown();
    }

    /** The id after a prefix, or null unless the rest is exactly a number. */
    private static Long idAfter(String destination, String prefix) {
        if (!destination.startsWith(prefix)) {
            return null;
        }
        String rest = destination.substring(prefix.length());
        if (rest.isEmpty() || rest.length() > 18 || !rest.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Long.valueOf(rest);
    }
}
