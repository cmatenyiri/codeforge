package com.codeforge.seed;

import static com.codeforge.seed.ProblemSeeder.addExample;
import static com.codeforge.seed.ProblemSeeder.addHint;
import static com.codeforge.seed.ProblemSeeder.addTestCase;
import static com.codeforge.seed.ProblemSeeder.param;
import static com.codeforge.seed.ProblemSeeder.problem;
import static com.codeforge.seed.ProblemSeeder.signature;

import com.codeforge.domain.DataType;
import com.codeforge.domain.Difficulty;
import com.codeforge.domain.Problem;
import com.codeforge.domain.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * The starter problem set.
 *
 * <p>Separate from {@link ProblemSeeder} purely to keep content out of the seeding
 * logic — this file is data, and is expected to be deleted once problems are
 * authored through the admin UI.
 *
 * <p>Each problem carries two visible cases and a set of hidden ones. The hidden
 * set is what a submission is judged against, and it deliberately includes more
 * than corner cases. Six problems end with a generated input at the top of their
 * stated constraints. Five of those are sized so that the naive solution the
 * hints warn against runs out of CPU time rather than merely being slow —
 * nested loops against a hash map in Two Sum and Best Time to Buy and Sell
 * Stock, rescanning for the tallest bar in Trapping Rain Water, recounting the
 * array in Top K Frequent Elements, and enumerating subsequences in Longest
 * Increasing Subsequence. Passing the two samples and then being told "Time
 * Limit Exceeded" is what those cases exist to teach, and it cannot be taught by
 * a case small enough to print.
 *
 * <p>The sizes were measured against the judge, not guessed, and against Java —
 * the fastest of the four runtimes, and the one where a JIT-compiled quadratic
 * loop can vectorise into something surprisingly quick. What decisively
 * exceeds five CPU seconds there exceeds it by a wide margin in Python and
 * JavaScript.
 */
final class SeedCatalogue {

    /** Only tags the seeded problems actually use, so no filter leads to an empty list. */
    static final List<String> TAG_NAMES = List.of(
            "Arrays", "Hash Table", "Strings", "Stack", "Linked List", "Two Pointers", "Dynamic Programming", "Graphs", "Binary Search", "Heap");

    private SeedCatalogue() {}

    static List<Problem> build(Map<String, Tag> tags) {
        List<Problem> problems = new ArrayList<>();
        Problem problem;

        // Two Sum
        problem = problem("Two Sum", Difficulty.EASY,
                "Given an array of integers `nums` and an integer `target`, return the **indices** of the two numbers such that they add up to `target`.\n\nYou may assume that each input has exactly one solution, and you may not use the same element twice. You can return the answer in any order.",
                "- `2 <= nums.length <= 3 * 10^5`\n- `-10^9 <= nums[i] <= 10^9`\n- `-10^9 <= target <= 10^9`\n- Exactly one valid answer exists.",
                tags.get("Arrays"), tags.get("Hash Table"));
        signature(problem, "twoSum", DataType.INT_ARRAY, param("nums", DataType.INT_ARRAY), param("target", DataType.INT));
        addExample(problem, "nums = [2,7,11,15], target = 9", "[0,1]", "nums[0] + nums[1] == 9, so the answer is [0, 1].");
        addExample(problem, "nums = [3,2,4], target = 6", "[1,2]", null);
        addHint(problem, "A brute force approach is O(n²). Can you trade memory for time?");
        addHint(problem, "Store each value you have seen in a hash map keyed by its complement.");
        addTestCase(problem, "[2,7,11,15]\n9", "[0,1]", false);
        addTestCase(problem, "[3,2,4]\n6", "[1,2]", false);
        addTestCase(problem, "[3,3]\n6", "[0,1]", true);
        addTestCase(problem, "[-1,-2,-3,-4,-5]\n-8", "[2,4]", true);
        addTestCase(problem, "[0,4,3,0]\n0", "[0,3]", true);
        addTestCase(problem, "[1,2,3,4,5,6,7,8,9,10]\n19", "[8,9]", true);
        addTestCase(problem, "[2,5,5,11]\n10", "[1,2]", true);
        // 1..300000, with the only pair that sums to the target in the last two
        // slots. The nested-loop solution has to walk almost the whole square
        // before it finds them — 4.5 * 10^10 comparisons, far past the judge's
        // five CPU seconds. A hash map finds it in one pass.
        addTestCase(problem, intList(i -> i + 1, 300_000) + "\n599999", "[299998,299999]", true);
        problems.add(problem);

        // Valid Parentheses
        problem = problem("Valid Parentheses", Difficulty.EASY,
                "Given a string `s` containing just the characters `(`, `)`, `{`, `}`, `[` and `]`, determine if the input string is valid.\n\nAn input string is valid if open brackets are closed by the same type of bracket, and in the correct order.",
                "- `1 <= s.length <= 10^4`\n- `s` consists of bracket characters only.",
                tags.get("Strings"), tags.get("Stack"));
        signature(problem, "isValid", DataType.BOOLEAN, param("s", DataType.STRING));
        addExample(problem, "s = \"()[]{}\"", "true", null);
        addExample(problem, "s = \"(]\"", "false", "The closing bracket does not match the most recent opening one.");
        addHint(problem, "What data structure naturally reverses order?");
        addHint(problem, "Push opening brackets; on a closing bracket, the top of the stack must match.");
        addTestCase(problem, "()[]{}", "true", false);
        addTestCase(problem, "(]", "false", false);
        addTestCase(problem, "{[()]}", "true", true);
        addTestCase(problem, "(", "false", true);
        addTestCase(problem, "]", "false", true);
        addTestCase(problem, "([)]", "false", true);
        addTestCase(problem, "((((()))))", "true", true);
        addTestCase(problem, "(){}[]{}()", "true", true);
        problems.add(problem);

        // Merge Two Sorted Lists
        problem = problem("Merge Two Sorted Lists", Difficulty.EASY,
                "You are given the heads of two sorted linked lists `list1` and `list2`.\n\nMerge the two lists into one **sorted** list. The result should be made by splicing together the nodes of the first two lists.",
                "- The number of nodes in both lists is in the range `[0, 50]`.\n- `-100 <= Node.val <= 100`\n- Both lists are sorted in non-decreasing order.",
                tags.get("Linked List"), tags.get("Two Pointers"));
        // The lists are exchanged as arrays: a linked list has no canonical text
        // form, and the algorithm being tested is the merge, not the plumbing.
        signature(problem, "mergeTwoLists", DataType.INT_ARRAY, param("list1", DataType.INT_ARRAY), param("list2", DataType.INT_ARRAY));
        addExample(problem, "list1 = [1,2,4], list2 = [1,3,4]", "[1,1,2,3,4,4]", null);
        addExample(problem, "list1 = [], list2 = [0]", "[0]", "An empty list merges to the other list unchanged.");
        addHint(problem, "A dummy head node removes the special case for the first element.");
        addHint(problem, "Advance whichever list currently has the smaller head.");
        addTestCase(problem, "[1,2,4]\n[1,3,4]", "[1,1,2,3,4,4]", false);
        addTestCase(problem, "[]\n[0]", "[0]", false);
        addTestCase(problem, "[]\n[]", "[]", true);
        addTestCase(problem, "[1]\n[]", "[1]", true);
        addTestCase(problem, "[1,2,3]\n[4,5,6]", "[1,2,3,4,5,6]", true);
        addTestCase(problem, "[-100,0,100]\n[-50,50]", "[-100,-50,0,50,100]", true);
        addTestCase(problem, "[5]\n[1,2,4]", "[1,2,4,5]", true);
        addTestCase(problem, "[1,1,1]\n[1,1]", "[1,1,1,1,1]", true);
        problems.add(problem);

        // Best Time to Buy and Sell Stock
        problem = problem("Best Time to Buy and Sell Stock", Difficulty.EASY,
                "You are given an array `prices` where `prices[i]` is the price of a given stock on the *i*-th day.\n\nMaximise your profit by choosing a single day to buy and a different day in the future to sell. Return the maximum profit, or `0` if no profit is possible.",
                "- `1 <= prices.length <= 2 * 10^5`\n- `0 <= prices[i] <= 10^4`",
                tags.get("Arrays"), tags.get("Dynamic Programming"));
        signature(problem, "maxProfit", DataType.INT, param("prices", DataType.INT_ARRAY));
        addExample(problem, "prices = [7,1,5,3,6,4]", "5", "Buy on day 2 (price = 1) and sell on day 5 (price = 6).");
        addExample(problem, "prices = [7,6,4,3,1]", "0", "No transaction is profitable.");
        addHint(problem, "Track the lowest price seen so far as you sweep left to right.");
        addHint(problem, "At every day, the best sale today is today's price minus the running minimum.");
        addTestCase(problem, "[7,1,5,3,6,4]", "5", false);
        addTestCase(problem, "[7,6,4,3,1]", "0", false);
        addTestCase(problem, "[1,2]", "1", true);
        addTestCase(problem, "[1]", "0", true);
        addTestCase(problem, "[2,4,1]", "2", true);
        addTestCase(problem, "[3,3,5,0,0,3,1,4]", "4", true);
        addTestCase(problem, "[10,9,8,7,6,5]", "0", true);
        addTestCase(problem, "[0,10000]", "10000", true);
        // Falls for 100k days, then climbs for 100k. The one profitable trade
        // spans the trough, so comparing every buy day against every sell day
        // means 2 * 10^10 comparisons — measured well past the CPU limit —
        // while tracking the running minimum needs a single pass.
        addTestCase(
                problem,
                intList(i -> i < 100_000 ? 10_000 - i / 10 : (i - 100_000) / 10, 200_000),
                "9999",
                true);
        problems.add(problem);

        // Longest Substring Without Repeating Characters
        problem = problem("Longest Substring Without Repeating Characters", Difficulty.MEDIUM,
                "Given a string `s`, find the length of the **longest substring** without repeating characters.",
                "- `0 <= s.length <= 5 * 10^4`\n- `s` consists of English letters, digits, symbols and spaces.",
                tags.get("Strings"), tags.get("Hash Table"), tags.get("Two Pointers"));
        signature(problem, "lengthOfLongestSubstring", DataType.INT, param("s", DataType.STRING));
        addExample(problem, "s = \"abcabcbb\"", "3", "The answer is \"abc\", with length 3.");
        addExample(problem, "s = \"bbbbb\"", "1", "The answer is \"b\".");
        addHint(problem, "A sliding window only ever needs to move forward.");
        addHint(problem, "When you meet a repeat, jump the left edge past its previous occurrence.");
        addTestCase(problem, "abcabcbb", "3", false);
        addTestCase(problem, "bbbbb", "1", false);
        addTestCase(problem, "pwwkew", "3", true);
        addTestCase(problem, "", "0", true);
        addTestCase(problem, "au", "2", true);
        addTestCase(problem, "dvdf", "3", true);
        addTestCase(problem, "abba", "2", true);
        addTestCase(problem, "tmmzuxt", "5", true);
        // "abcdefghij" 5000 times. Not a time-limit case for a compiled language
        // — the alphabet bounds any window to a few characters, so even a
        // restart-at-every-index scan stays linear in practice — but it is the
        // one case here that a solution allocating per character, or building
        // substrings, does not survive.
        addTestCase(problem, "abcdefghij".repeat(5_000), "10", true);
        problems.add(problem);

        // Course Schedule
        problem = problem("Course Schedule", Difficulty.MEDIUM,
                "There are `numCourses` courses labelled `0` to `numCourses - 1`. You are given `prerequisites` where `prerequisites[i] = [a, b]` means you must take course `b` before course `a`.\n\nReturn `true` if you can finish all courses.",
                "- `1 <= numCourses <= 2000`\n- `0 <= prerequisites.length <= 5000`\n- All prerequisite pairs are distinct.",
                tags.get("Graphs"));
        signature(problem, "canFinish", DataType.BOOLEAN, param("numCourses", DataType.INT), param("prerequisites", DataType.INT_MATRIX));
        addExample(problem, "numCourses = 2, prerequisites = [[1,0]]", "true", "Take course 0, then course 1.");
        addExample(problem, "numCourses = 2, prerequisites = [[1,0],[0,1]]", "false", "The two courses depend on each other.");
        addHint(problem, "This is cycle detection on a directed graph.");
        addHint(problem, "Kahn's algorithm: repeatedly remove nodes whose in-degree is zero.");
        addTestCase(problem, "2\n[[1,0]]", "true", false);
        addTestCase(problem, "2\n[[1,0],[0,1]]", "false", false);
        addTestCase(problem, "3\n[[1,0],[2,1]]", "true", true);
        addTestCase(problem, "1\n[]", "true", true);
        addTestCase(problem, "5\n[[1,0],[2,1],[3,2],[4,3]]", "true", true);
        addTestCase(problem, "4\n[[1,0],[2,1],[0,2],[3,3]]", "false", true);
        addTestCase(problem, "3\n[[0,1],[0,2],[1,2]]", "true", true);
        addTestCase(problem, "2\n[[0,1],[1,0]]", "false", true);
        problems.add(problem);

        // Longest Increasing Subsequence
        problem = problem("Longest Increasing Subsequence", Difficulty.MEDIUM,
                "Given an integer array `nums`, return the length of the longest **strictly increasing subsequence**.",
                "- `1 <= nums.length <= 2500`\n- `-10^4 <= nums[i] <= 10^4`",
                tags.get("Dynamic Programming"), tags.get("Binary Search"));
        signature(problem, "lengthOfLIS", DataType.INT, param("nums", DataType.INT_ARRAY));
        addExample(problem, "nums = [10,9,2,5,3,7,101,18]", "4", "The subsequence is [2,3,7,101].");
        addExample(problem, "nums = [7,7,7,7]", "1", "No strictly increasing pair exists.");
        addHint(problem, "The O(n²) dynamic programme is a good starting point.");
        addHint(problem, "Keep the smallest tail for each length and binary search it for O(n log n).");
        addTestCase(problem, "[10,9,2,5,3,7,101,18]", "4", false);
        addTestCase(problem, "[7,7,7,7]", "1", false);
        addTestCase(problem, "[0,1,0,3,2,3]", "4", true);
        addTestCase(problem, "[1]", "1", true);
        addTestCase(problem, "[4,10,4,3,8,9]", "3", true);
        addTestCase(problem, "[1,3,6,7,9,4,10,5,6]", "6", true);
        addTestCase(problem, "[-2,-1]", "2", true);
        addTestCase(problem, "[10,9,8,7]", "1", true);
        // 2500 values with no order to exploit. The hinted O(n²) table handles
        // this in a few million steps; enumerating subsequences does not finish
        // in any amount of time, which is the difference the case exists to show.
        addTestCase(problem, intList(i -> (i * 7919) % 20_001 - 10_000, 2_500), "56", true);
        problems.add(problem);

        // Top K Frequent Elements
        problem = problem("Top K Frequent Elements", Difficulty.MEDIUM,
                "Given an integer array `nums` and an integer `k`, return the `k` most frequent elements. You may return the answer in any order.",
                "- `1 <= nums.length <= 10^5`\n- `k` is in the range `[1, number of unique elements]`.\n- The answer is guaranteed to be unique.",
                tags.get("Arrays"), tags.get("Hash Table"), tags.get("Heap"));
        signature(problem, "topKFrequent", DataType.INT_ARRAY, param("nums", DataType.INT_ARRAY), param("k", DataType.INT));
        addExample(problem, "nums = [1,1,1,2,2,3], k = 2", "[1,2]", "1 appears three times and 2 appears twice.");
        addExample(problem, "nums = [1], k = 1", "[1]", null);
        addHint(problem, "Count first, then rank.");
        addHint(problem, "Bucket by frequency to get O(n) without a heap.");
        addTestCase(problem, "[1,1,1,2,2,3]\n2", "[1,2]", false);
        addTestCase(problem, "[1]\n1", "[1]", false);
        addTestCase(problem, "[4,1,-1,2,-1,2,3]\n2", "[-1,2]", true);
        addTestCase(problem, "[1,1,1,1,2,2,2,3,3,4]\n3", "[1,2,3]", true);
        addTestCase(problem, "[5,5,5,4,4,3]\n2", "[5,4]", true);
        addTestCase(problem, "[7]\n1", "[7]", true);
        addTestCase(problem, "[9,9,8,8,8,7,7,7,7]\n2", "[7,8]", true);
        addTestCase(problem, "[-1,-1,-1,-2,-2,-3]\n3", "[-1,-2,-3]", true);
        // Value v appears exactly v times, so the ranking is total and the
        // expected answer unique. Counting each element by rescanning the array
        // is 10^10 comparisons — measured past the CPU limit — while counting
        // into a map is one pass.
        addTestCase(problem, repeatedByValue(446) + "\n3", "[446,445,444]", true);
        problems.add(problem);

        // Trapping Rain Water
        problem = problem("Trapping Rain Water", Difficulty.HARD,
                "Given `n` non-negative integers representing an elevation map where the width of each bar is `1`, compute how much water it can trap after raining.",
                "- `n == height.length`\n- `1 <= n <= 2 * 10^5`\n- `0 <= height[i] <= 10^5`",
                tags.get("Arrays"), tags.get("Two Pointers"));
        signature(problem, "trap", DataType.INT, param("height", DataType.INT_ARRAY));
        addExample(problem, "height = [0,1,0,2,1,0,1,3,2,1,2,1]", "6", "Six units of rain water are trapped.");
        addExample(problem, "height = [4,2,0,3,2,5]", "9", null);
        addHint(problem, "Water above a bar is bounded by the tallest bar on each side.");
        addHint(problem, "Two pointers moving inward keep both maxima up to date in a single pass.");
        addTestCase(problem, "[0,1,0,2,1,0,1,3,2,1,2,1]", "6", false);
        addTestCase(problem, "[4,2,0,3,2,5]", "9", false);
        addTestCase(problem, "[2,0,2]", "2", true);
        addTestCase(problem, "[3,0,3]", "3", true);
        addTestCase(problem, "[5]", "0", true);
        addTestCase(problem, "[4,2,3]", "1", true);
        addTestCase(problem, "[0,0,0]", "0", true);
        addTestCase(problem, "[5,4,1,2]", "1", true);
        // A comb of alternating 1s and 0s. Each dip holds exactly one unit, and
        // the last one holds none for want of a right wall — but only after the
        // whole array has been walked, which rules out scanning for the tallest
        // bar on each side of every index: that is 1.3 * 10^10 comparisons here,
        // measured at well over the judge's five CPU seconds. The two-pointer
        // sweep does it in one pass.
        addTestCase(problem, intList(i -> i % 2 == 0 ? 1 : 0, 160_000), "79999", true);
        problems.add(problem);

        // Median of Two Sorted Arrays
        problem = problem("Median of Two Sorted Arrays", Difficulty.HARD,
                "Given two sorted arrays `nums1` and `nums2` of size `m` and `n`, return the median of the two sorted arrays.\n\nThe overall run time complexity should be `O(log (m+n))`.",
                "- `0 <= m, n <= 1000`\n- `1 <= m + n <= 2000`\n- Both arrays are sorted in non-decreasing order.",
                tags.get("Arrays"), tags.get("Binary Search"));
        signature(problem, "findMedianSortedArrays", DataType.DOUBLE, param("nums1", DataType.INT_ARRAY), param("nums2", DataType.INT_ARRAY));
        addExample(problem, "nums1 = [1,3], nums2 = [2]", "2.00000", "The merged array is [1,2,3], so the median is 2.");
        addExample(problem, "nums1 = [1,2], nums2 = [3,4]", "2.50000", "The merged array is [1,2,3,4].");
        addHint(problem, "Merging is O(m+n) — too slow. Binary search the partition instead.");
        addHint(problem, "Search over the shorter array for the split that balances both halves.");
        addTestCase(problem, "[1,3]\n[2]", "2.00000", false);
        addTestCase(problem, "[1,2]\n[3,4]", "2.50000", false);
        addTestCase(problem, "[]\n[1]", "1.00000", true);
        addTestCase(problem, "[1,2,3,4,5]\n[6,7,8,9,10]", "5.50000", true);
        addTestCase(problem, "[2]\n[]", "2.00000", true);
        addTestCase(problem, "[1,3]\n[2,7]", "2.50000", true);
        addTestCase(problem, "[0,0]\n[0,0]", "0.00000", true);
        addTestCase(problem, "[1,2,3]\n[4]", "2.50000", true);
        problems.add(problem);

        return problems;
    }

    /**
     * {@code [1, 2,2, 3,3,3, …]} up to {@code max} — every value repeated as many
     * times as its own magnitude, which makes the frequency ranking total and so
     * the expected answer unique.
     */
    private static String repeatedByValue(int max) {
        StringBuilder builder = new StringBuilder(max * max * 4).append('[');
        for (int value = 1; value <= max; value++) {
            for (int repeat = 0; repeat < value; repeat++) {
                if (builder.length() > 1) {
                    builder.append(',');
                }
                builder.append(value);
            }
        }
        return builder.append(']').toString();
    }

    /**
     * A canonical {@code [a,b,c]} array of {@code count} values.
     *
     * <p>Built here rather than written out: a hundred thousand numbers is not
     * something to paste into a source file, and a generated input is auditable
     * — the rule that produced it is right next to the expected answer.
     */
    private static String intList(IntUnaryOperator value, int count) {
        StringBuilder builder = new StringBuilder(count * 7).append('[');
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(value.applyAsInt(i));
        }
        return builder.append(']').toString();
    }
}
