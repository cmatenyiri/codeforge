package com.codeforge.repository;

import com.codeforge.domain.DailyChallenge;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, Long> {

    @Query("""
            select d from DailyChallenge d
            join fetch d.problem
            where d.date = :date
            """)
    Optional<DailyChallenge> findByDate(@Param("date") LocalDate date);

    /** A month's worth, for the calendar widget on the problem list. */
    @Query("""
            select d from DailyChallenge d
            join fetch d.problem
            where d.date between :from and :to
            order by d.date asc
            """)
    List<DailyChallenge> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Problems used as a daily in the recent past.
     *
     * <p>The rotation excludes these, so a catalogue larger than the cooldown
     * never repeats a problem within it — the same trick the mock interview uses
     * to keep a drawn set feeling unseen.
     */
    @Query("select d.problem.id from DailyChallenge d where d.date >= :since")
    List<Long> findProblemIdsSince(@Param("since") LocalDate since);

    /**
     * Every date this user solved that date's daily problem, newest first.
     *
     * <p>The streak, in one query. A solve only counts on the day itself: the
     * join pins the submission's own UTC date to the challenge's date, so
     * catching up on a problem three days later is a solve but not a streak day
     * — which is exactly what makes a streak worth having.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select distinct date_format(d.challenge_date, '%Y-%m-%d')
                    from daily_challenges d
                    join submissions s
                      on s.problem_id = d.problem_id
                     and s.user_id = :userId
                     and s.status = 'ACCEPTED'
                     and date(s.created_at) = d.challenge_date
                    order by 1 desc
                    """)
    List<String> findSolvedDates(@Param("userId") Long userId);

    /** Which of a date range this user solved on the day — for the calendar's ticks. */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select distinct date_format(d.challenge_date, '%Y-%m-%d')
                    from daily_challenges d
                    join submissions s
                      on s.problem_id = d.problem_id
                     and s.user_id = :userId
                     and s.status = 'ACCEPTED'
                     and date(s.created_at) = d.challenge_date
                    where d.challenge_date between :from and :to
                    """)
    List<String> findSolvedDatesBetween(
            @Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Per month: how many daily challenges it held, and how many this user
     * solved on the day.
     *
     * <p>The monthly badge is earned by clearing every one of them, so both
     * halves have to come from the same query — comparing a count of challenges
     * against a separately-fetched count of solves would award a badge whenever
     * the two happened to agree for unrelated reasons.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select date_format(d.challenge_date, '%Y-%m') as month,
                           count(*) as total,
                           sum(case when exists (
                                 select 1 from submissions s
                                 where s.problem_id = d.problem_id and s.user_id = :userId
                                   and s.status = 'ACCEPTED' and date(s.created_at) = d.challenge_date
                               ) then 1 else 0 end) as solved
                    from daily_challenges d
                    group by month
                    order by month asc
                    """)
    List<MonthProgress> findMonthlyProgress(@Param("userId") Long userId);

    /**
     * Per calendar year: how many days this user solved the daily on the day.
     *
     * <p>What the 50- and 100-day badges are counted from. Days, not
     * consecutive days — the annual badges reward turning up often across a
     * year rather than an unbroken run, which is what the streak is for.
     */
    @Query(
            nativeQuery = true,
            value =
                    """
                    select year(d.challenge_date) as year, count(*) as days
                    from daily_challenges d
                    where exists (
                        select 1 from submissions s
                        where s.problem_id = d.problem_id and s.user_id = :userId
                          and s.status = 'ACCEPTED' and date(s.created_at) = d.challenge_date)
                    group by year
                    order by year asc
                    """)
    List<YearProgress> findYearlyProgress(@Param("userId") Long userId);

    /** Projection for {@link #findMonthlyProgress}. */
    interface MonthProgress {
        /** ISO {@code yyyy-MM}. */
        String getMonth();

        long getTotal();

        long getSolved();
    }

    /** Projection for {@link #findYearlyProgress}. */
    interface YearProgress {
        int getYear();

        long getDays();
    }
}
