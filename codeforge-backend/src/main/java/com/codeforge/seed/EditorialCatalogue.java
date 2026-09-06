package com.codeforge.seed;

import com.codeforge.domain.Language;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The written solutions that back the Editorial tab.
 *
 * <p>Data, like {@link SeedCatalogue}, and separate from it for the same reason:
 * an editorial is by far the largest thing a problem owns — a walkthrough plus
 * four reference implementations — and mixing that volume into the problem
 * definitions would bury them.
 *
 * <p>Every solution here is the one the walkthrough describes, in the exact
 * shape the language's starter stub declares: a {@code Solution} class for Java
 * and Python, a free function for JavaScript and TypeScript. They are meant to
 * be pasted into the editor and submitted as-is, so they are written against the
 * runtimes the judge actually ships — Node 12 and TypeScript 3.7 — rather than
 * against current syntax.
 */
final class EditorialCatalogue {

    private EditorialCatalogue() {}

    private static final String TWO_SUM_MD =
            """
            ### The obvious attempt

            Check every pair. That is `n * (n - 1) / 2` comparisons — at the top of the
            constraints, around `4.5 * 10^10` of them, which is why the brute force passes
            the two examples here and then times out on submission.

            ### One pass with a hash map

            Turn the question around. Standing at index `i`, you are not looking for *any*
            pair — you are looking for exactly one number, `target - nums[i]`. If you have
            already walked past it, you are done.

            So walk once, and remember every value you have seen along with where you saw
            it. At each step, ask the map for the complement before inserting the current
            value; asking first is what stops an element from pairing with itself.

            Each value is inserted once and looked up once, so the whole scan is linear.

            **Watch for:** duplicates (`[3,3]`, target `6`) work because the lookup happens
            before the insert, and negative numbers need no special handling.
            """;

    private static final String TWO_SUM_JAVA =
            """
            import java.util.*;

            class Solution {
                public int[] twoSum(int[] nums, int target) {
                    Map<Integer, Integer> seen = new HashMap<>();
                    for (int i = 0; i < nums.length; i++) {
                        Integer j = seen.get(target - nums[i]);
                        if (j != null) {
                            return new int[] {j, i};
                        }
                        seen.put(nums[i], i);
                    }
                    return new int[0];
                }
            }
            """;

    private static final String TWO_SUM_PYTHON =
            """
            from typing import List


            class Solution:
                def twoSum(self, nums: List[int], target: int) -> List[int]:
                    seen = {}
                    for i, value in enumerate(nums):
                        if target - value in seen:
                            return [seen[target - value], i]
                        seen[value] = i
                    return []
            """;

    private static final String TWO_SUM_JS =
            """
            function twoSum(nums, target) {
              const seen = new Map();
              for (let i = 0; i < nums.length; i++) {
                const j = seen.get(target - nums[i]);
                if (j !== undefined) {
                  return [j, i];
                }
                seen.set(nums[i], i);
              }
              return [];
            }
            """;

    private static final String TWO_SUM_TS =
            """
            function twoSum(nums: number[], target: number): number[] {
              const seen = new Map<number, number>();
              for (let i = 0; i < nums.length; i++) {
                const j = seen.get(target - nums[i]);
                if (j !== undefined) {
                  return [j, i];
                }
                seen.set(nums[i], i);
              }
              return [];
            }
            """;

    private static final String VALID_PARENTHESES_MD =
            """
            ### What makes brackets valid

            Counting is not enough: `([)]` has the right number of each kind and is still
            wrong. What matters is *order* — the bracket you close must be the one you
            opened most recently.

            ### A stack

            "Most recently opened" is exactly what a stack gives you. Push every opening
            bracket. On a closing bracket, pop: if the stack is empty, or the top is not the
            matching opener, the string is invalid.

            At the end the stack must be empty. A leftover opener means something was never
            closed, which `(` alone shows.

            **Watch for:** the two ways to fail early — a closing bracket with nothing to
            match, and a mismatched pair — and the one way to fail late, an unclosed opener
            still on the stack.
            """;

    private static final String VALID_PARENTHESES_JAVA =
            """
            import java.util.*;

            class Solution {
                public boolean isValid(String s) {
                    Map<Character, Character> openerFor = Map.of(')', '(', ']', '[', '}', '{');
                    Deque<Character> stack = new ArrayDeque<>();

                    for (char bracket : s.toCharArray()) {
                        Character opener = openerFor.get(bracket);
                        if (opener == null) {
                            stack.push(bracket);
                        } else if (stack.isEmpty() || stack.pop() != opener.charValue()) {
                            return false;
                        }
                    }
                    return stack.isEmpty();
                }
            }
            """;

    private static final String VALID_PARENTHESES_PYTHON =
            """
            class Solution:
                def isValid(self, s: str) -> bool:
                    opener_for = {")": "(", "]": "[", "}": "{"}
                    stack = []

                    for bracket in s:
                        if bracket not in opener_for:
                            stack.append(bracket)
                        elif not stack or stack.pop() != opener_for[bracket]:
                            return False

                    return not stack
            """;

    private static final String VALID_PARENTHESES_JS =
            """
            function isValid(s) {
              const openerFor = { ')': '(', ']': '[', '}': '{' };
              const stack = [];

              for (const bracket of s) {
                if (openerFor[bracket] === undefined) {
                  stack.push(bracket);
                } else if (stack.pop() !== openerFor[bracket]) {
                  return false;
                }
              }
              return stack.length === 0;
            }
            """;

    private static final String VALID_PARENTHESES_TS =
            """
            function isValid(s: string): boolean {
              const openerFor: { [bracket: string]: string } = { ')': '(', ']': '[', '}': '{' };
              const stack: string[] = [];

              for (const bracket of s) {
                if (openerFor[bracket] === undefined) {
                  stack.push(bracket);
                } else if (stack.pop() !== openerFor[bracket]) {
                  return false;
                }
              }
              return stack.length === 0;
            }
            """;

    private static final String MERGE_TWO_SORTED_LISTS_MD =
            """
            ### Both lists are already sorted

            That is the whole resource. The smallest value not yet taken is always at the
            front of one list or the other, so you never have to search — only compare two
            candidates.

            ### Walk both with one index each

            Keep a cursor into each list. Compare the two heads, take the smaller, advance
            that cursor. When one list runs out, everything left in the other is already
            sorted and larger, so append it wholesale.

            Taking the left value on a tie is what keeps the merge stable, which matters if
            the values carry anything besides their own order.

            **Watch for:** the drain loops after the main one. Forgetting them silently
            truncates the answer, and an empty input list makes the main loop exit
            immediately.
            """;

    private static final String MERGE_TWO_SORTED_LISTS_JAVA =
            """
            class Solution {
                public int[] mergeTwoLists(int[] list1, int[] list2) {
                    int[] merged = new int[list1.length + list2.length];
                    int i = 0;
                    int j = 0;
                    int out = 0;

                    while (i < list1.length && j < list2.length) {
                        merged[out++] = list1[i] <= list2[j] ? list1[i++] : list2[j++];
                    }
                    while (i < list1.length) {
                        merged[out++] = list1[i++];
                    }
                    while (j < list2.length) {
                        merged[out++] = list2[j++];
                    }
                    return merged;
                }
            }
            """;

    private static final String MERGE_TWO_SORTED_LISTS_PYTHON =
            """
            from typing import List


            class Solution:
                def mergeTwoLists(self, list1: List[int], list2: List[int]) -> List[int]:
                    merged = []
                    i = j = 0

                    while i < len(list1) and j < len(list2):
                        if list1[i] <= list2[j]:
                            merged.append(list1[i])
                            i += 1
                        else:
                            merged.append(list2[j])
                            j += 1

                    merged.extend(list1[i:])
                    merged.extend(list2[j:])
                    return merged
            """;

    private static final String MERGE_TWO_SORTED_LISTS_JS =
            """
            function mergeTwoLists(list1, list2) {
              const merged = [];
              let i = 0;
              let j = 0;

              while (i < list1.length && j < list2.length) {
                merged.push(list1[i] <= list2[j] ? list1[i++] : list2[j++]);
              }
              while (i < list1.length) {
                merged.push(list1[i++]);
              }
              while (j < list2.length) {
                merged.push(list2[j++]);
              }
              return merged;
            }
            """;

    private static final String MERGE_TWO_SORTED_LISTS_TS =
            """
            function mergeTwoLists(list1: number[], list2: number[]): number[] {
              const merged: number[] = [];
              let i = 0;
              let j = 0;

              while (i < list1.length && j < list2.length) {
                merged.push(list1[i] <= list2[j] ? list1[i++] : list2[j++]);
              }
              while (i < list1.length) {
                merged.push(list1[i++]);
              }
              while (j < list2.length) {
                merged.push(list2[j++]);
              }
              return merged;
            }
            """;

    private static final String BEST_TIME_TO_BUY_AND_SELL_STOCK_MD =
            """
            ### One transaction, in order

            You must buy before you sell. Comparing every buy day against every sell day is
            the direct reading of that, and it is quadratic — at 200,000 days it is
            `2 * 10^10` comparisons and will not finish inside the limit.

            ### Ask a smaller question

            Instead of "which pair is best", ask at each day: *if I sold today, what is the
            most I could have made?* That is today's price minus the cheapest price on any
            earlier day.

            So sweep left to right carrying one number — the lowest price seen so far — and
            one running answer. Both update in constant time, so the whole thing is a single
            pass and no extra memory.

            **Watch for:** a strictly falling market. Every candidate profit is negative, and
            the answer is `0` because you are allowed not to trade at all.
            """;

    private static final String BEST_TIME_TO_BUY_AND_SELL_STOCK_JAVA =
            """
            class Solution {
                public int maxProfit(int[] prices) {
                    int cheapestSoFar = Integer.MAX_VALUE;
                    int best = 0;

                    for (int price : prices) {
                        cheapestSoFar = Math.min(cheapestSoFar, price);
                        best = Math.max(best, price - cheapestSoFar);
                    }
                    return best;
                }
            }
            """;

    private static final String BEST_TIME_TO_BUY_AND_SELL_STOCK_PYTHON =
            """
            from typing import List


            class Solution:
                def maxProfit(self, prices: List[int]) -> int:
                    cheapest_so_far = float("inf")
                    best = 0

                    for price in prices:
                        cheapest_so_far = min(cheapest_so_far, price)
                        best = max(best, price - cheapest_so_far)

                    return best
            """;

    private static final String BEST_TIME_TO_BUY_AND_SELL_STOCK_JS =
            """
            function maxProfit(prices) {
              let cheapestSoFar = Infinity;
              let best = 0;

              for (const price of prices) {
                cheapestSoFar = Math.min(cheapestSoFar, price);
                best = Math.max(best, price - cheapestSoFar);
              }
              return best;
            }
            """;

    private static final String BEST_TIME_TO_BUY_AND_SELL_STOCK_TS =
            """
            function maxProfit(prices: number[]): number {
              let cheapestSoFar = Infinity;
              let best = 0;

              for (const price of prices) {
                cheapestSoFar = Math.min(cheapestSoFar, price);
                best = Math.max(best, price - cheapestSoFar);
              }
              return best;
            }
            """;

    private static final String LONGEST_SUBSTRING_WITHOUT_REPEATING_CHARACTERS_MD =
            """
            ### A window that only ever moves forward

            Checking every substring is `O(n^2)` candidates before you even test one for
            duplicates. The saving comes from noticing that neither end of the answer ever
            needs to move backwards.

            ### Sliding window

            Keep a window `[start, i]` that contains no repeat. Extend it by one character
            at a time. When the new character has been seen *inside the current window*, the
            window cannot keep both copies — so jump `start` to just past the previous
            occurrence.

            Remember the last index of every character in a map. The check "inside the
            current window" is then `previous >= start`, which is what stops a stale
            occurrence from dragging `start` backwards.

            **Watch for:** `abba`. When the second `a` arrives its last index is `0`, which
            is behind `start` by then; without the `>= start` test the window would reopen a
            duplicate.
            """;

    private static final String LONGEST_SUBSTRING_WITHOUT_REPEATING_CHARACTERS_JAVA =
            """
            import java.util.*;

            class Solution {
                public int lengthOfLongestSubstring(String s) {
                    Map<Character, Integer> lastIndex = new HashMap<>();
                    int best = 0;
                    int start = 0;

                    for (int i = 0; i < s.length(); i++) {
                        Integer previous = lastIndex.get(s.charAt(i));
                        if (previous != null && previous >= start) {
                            start = previous + 1;
                        }
                        lastIndex.put(s.charAt(i), i);
                        best = Math.max(best, i - start + 1);
                    }
                    return best;
                }
            }
            """;

    private static final String LONGEST_SUBSTRING_WITHOUT_REPEATING_CHARACTERS_PYTHON =
            """
            class Solution:
                def lengthOfLongestSubstring(self, s: str) -> int:
                    last_index = {}
                    best = 0
                    start = 0

                    for i, char in enumerate(s):
                        previous = last_index.get(char)
                        if previous is not None and previous >= start:
                            start = previous + 1
                        last_index[char] = i
                        best = max(best, i - start + 1)

                    return best
            """;

    private static final String LONGEST_SUBSTRING_WITHOUT_REPEATING_CHARACTERS_JS =
            """
            function lengthOfLongestSubstring(s) {
              const lastIndex = new Map();
              let best = 0;
              let start = 0;

              for (let i = 0; i < s.length; i++) {
                const previous = lastIndex.get(s[i]);
                if (previous !== undefined && previous >= start) {
                  start = previous + 1;
                }
                lastIndex.set(s[i], i);
                best = Math.max(best, i - start + 1);
              }
              return best;
            }
            """;

    private static final String LONGEST_SUBSTRING_WITHOUT_REPEATING_CHARACTERS_TS =
            """
            function lengthOfLongestSubstring(s: string): number {
              const lastIndex = new Map<string, number>();
              let best = 0;
              let start = 0;

              for (let i = 0; i < s.length; i++) {
                const previous = lastIndex.get(s[i]);
                if (previous !== undefined && previous >= start) {
                  start = previous + 1;
                }
                lastIndex.set(s[i], i);
                best = Math.max(best, i - start + 1);
              }
              return best;
            }
            """;

    private static final String COURSE_SCHEDULE_MD =
            """
            ### It is a graph question

            Draw an edge from each prerequisite to the course that needs it. You can finish
            everything exactly when that directed graph has no cycle — a cycle is a set of
            courses each waiting on another, and none of them can ever be first.

            ### Kahn's algorithm

            Count how many prerequisites each course still has: its in-degree. Every course
            with in-degree `0` can be taken now, so put those in a queue.

            Take courses off the queue one at a time. Taking a course satisfies one
            prerequisite of everything downstream, so decrement those in-degrees, and
            anything that drops to `0` joins the queue.

            If you manage to take every course, there was no cycle. If the queue empties
            early, whatever is left is knotted together.

            **Watch for:** a course listed as its own prerequisite. Its in-degree never
            reaches `0`, so it never enters the queue — which is the right answer, and needs
            no special case.
            """;

    private static final String COURSE_SCHEDULE_JAVA =
            """
            import java.util.*;

            class Solution {
                public boolean canFinish(int numCourses, int[][] prerequisites) {
                    List<List<Integer>> unlockedBy = new ArrayList<>();
                    for (int course = 0; course < numCourses; course++) {
                        unlockedBy.add(new ArrayList<>());
                    }

                    int[] remaining = new int[numCourses];
                    for (int[] pair : prerequisites) {
                        unlockedBy.get(pair[1]).add(pair[0]);
                        remaining[pair[0]]++;
                    }

                    Deque<Integer> ready = new ArrayDeque<>();
                    for (int course = 0; course < numCourses; course++) {
                        if (remaining[course] == 0) {
                            ready.add(course);
                        }
                    }

                    int taken = 0;
                    while (!ready.isEmpty()) {
                        int course = ready.poll();
                        taken++;
                        for (int dependent : unlockedBy.get(course)) {
                            if (--remaining[dependent] == 0) {
                                ready.add(dependent);
                            }
                        }
                    }
                    return taken == numCourses;
                }
            }
            """;

    private static final String COURSE_SCHEDULE_PYTHON =
            """
            from collections import deque
            from typing import List


            class Solution:
                def canFinish(self, numCourses: int, prerequisites: List[List[int]]) -> bool:
                    unlocked_by = [[] for _ in range(numCourses)]
                    remaining = [0] * numCourses

                    for course, needs in prerequisites:
                        unlocked_by[needs].append(course)
                        remaining[course] += 1

                    ready = deque(c for c in range(numCourses) if remaining[c] == 0)
                    taken = 0

                    while ready:
                        course = ready.popleft()
                        taken += 1
                        for dependent in unlocked_by[course]:
                            remaining[dependent] -= 1
                            if remaining[dependent] == 0:
                                ready.append(dependent)

                    return taken == numCourses
            """;

    private static final String COURSE_SCHEDULE_JS =
            """
            function canFinish(numCourses, prerequisites) {
              const unlockedBy = [];
              for (let course = 0; course < numCourses; course++) {
                unlockedBy.push([]);
              }
              const remaining = new Array(numCourses).fill(0);

              for (const pair of prerequisites) {
                unlockedBy[pair[1]].push(pair[0]);
                remaining[pair[0]]++;
              }

              const ready = [];
              for (let course = 0; course < numCourses; course++) {
                if (remaining[course] === 0) {
                  ready.push(course);
                }
              }

              let taken = 0;
              while (ready.length > 0) {
                const course = ready.pop();
                taken++;
                for (const dependent of unlockedBy[course]) {
                  remaining[dependent]--;
                  if (remaining[dependent] === 0) {
                    ready.push(dependent);
                  }
                }
              }
              return taken === numCourses;
            }
            """;

    private static final String COURSE_SCHEDULE_TS =
            """
            function canFinish(numCourses: number, prerequisites: number[][]): boolean {
              const unlockedBy: number[][] = [];
              for (let course = 0; course < numCourses; course++) {
                unlockedBy.push([]);
              }
              const remaining: number[] = new Array(numCourses).fill(0);

              for (const pair of prerequisites) {
                unlockedBy[pair[1]].push(pair[0]);
                remaining[pair[0]]++;
              }

              const ready: number[] = [];
              for (let course = 0; course < numCourses; course++) {
                if (remaining[course] === 0) {
                  ready.push(course);
                }
              }

              let taken = 0;
              while (ready.length > 0) {
                const course = ready.pop() as number;
                taken++;
                for (const dependent of unlockedBy[course]) {
                  remaining[dependent]--;
                  if (remaining[dependent] === 0) {
                    ready.push(dependent);
                  }
                }
              }
              return taken === numCourses;
            }
            """;

    private static final String LONGEST_INCREASING_SUBSEQUENCE_MD =
            """
            ### Why the recursion dies

            Take-or-skip at every index is `2^n` branches. At 2,500 values that is not slow,
            it is unfinishable — which is what the hidden case at the top of the constraints
            demonstrates. Memoising it gets you to `O(n^2)`, which is fine here; the method
            below is better still.

            ### Keep the smallest possible tail

            Maintain a list `tails`, where `tails[k]` is the smallest value that can end an
            increasing subsequence of length `k + 1`. That list is always sorted, which is
            the property everything else rests on.

            For each value, find the first tail that is not smaller than it and overwrite
            it. Overwriting keeps future options as open as possible — a smaller tail can be
            extended by more things. If the value is larger than every tail, it extends the
            longest subsequence, so append.

            `tails` is not itself a valid subsequence. Only its **length** is the answer.

            **Watch for:** strict increase. Use a lower bound (first tail `>=` value); an
            upper bound would quietly solve the non-strict problem instead.
            """;

    private static final String LONGEST_INCREASING_SUBSEQUENCE_JAVA =
            """
            import java.util.*;

            class Solution {
                public int lengthOfLIS(int[] nums) {
                    int[] tails = new int[nums.length];
                    int length = 0;

                    for (int value : nums) {
                        int position = Arrays.binarySearch(tails, 0, length, value);
                        if (position < 0) {
                            position = -(position + 1);
                        }
                        tails[position] = value;
                        if (position == length) {
                            length++;
                        }
                    }
                    return length;
                }
            }
            """;

    private static final String LONGEST_INCREASING_SUBSEQUENCE_PYTHON =
            """
            import bisect
            from typing import List


            class Solution:
                def lengthOfLIS(self, nums: List[int]) -> int:
                    tails = []

                    for value in nums:
                        position = bisect.bisect_left(tails, value)
                        if position == len(tails):
                            tails.append(value)
                        else:
                            tails[position] = value

                    return len(tails)
            """;

    private static final String LONGEST_INCREASING_SUBSEQUENCE_JS =
            """
            function lengthOfLIS(nums) {
              const tails = [];

              for (const value of nums) {
                let low = 0;
                let high = tails.length;
                while (low < high) {
                  const mid = (low + high) >> 1;
                  if (tails[mid] < value) {
                    low = mid + 1;
                  } else {
                    high = mid;
                  }
                }
                tails[low] = value;
              }
              return tails.length;
            }
            """;

    private static final String LONGEST_INCREASING_SUBSEQUENCE_TS =
            """
            function lengthOfLIS(nums: number[]): number {
              const tails: number[] = [];

              for (const value of nums) {
                let low = 0;
                let high = tails.length;
                while (low < high) {
                  const mid = (low + high) >> 1;
                  if (tails[mid] < value) {
                    low = mid + 1;
                  } else {
                    high = mid;
                  }
                }
                tails[low] = value;
              }
              return tails.length;
            }
            """;

    private static final String TOP_K_FREQUENT_ELEMENTS_MD =
            """
            ### Count first, then rank

            Two separate jobs. Counting by rescanning the array for each element is
            quadratic and will not survive the largest case; a hash map counts everything in
            one pass.

            ### Ranking

            With counts in hand there are far fewer distinct values than elements, so
            sorting them by frequency is cheap. Sort descending by count and take the first
            `k`.

            This judge compares your output exactly, so ties need a rule: when two values
            occur equally often, the smaller value comes first. Without a tie-break the
            answer depends on hash iteration order and will not reproduce.

            **Faster, if you want it:** bucket the values by count into an array of size
            `n + 1` and walk it downwards. That drops the ranking to `O(n)` — the sort is
            the only non-linear step here.
            """;

    private static final String TOP_K_FREQUENT_ELEMENTS_JAVA =
            """
            import java.util.*;

            class Solution {
                public int[] topKFrequent(int[] nums, int k) {
                    Map<Integer, Integer> counts = new HashMap<>();
                    for (int value : nums) {
                        counts.merge(value, 1, Integer::sum);
                    }

                    List<Integer> ranked = new ArrayList<>(counts.keySet());
                    // Most frequent first, then smallest value first so ties are reproducible.
                    ranked.sort((a, b) -> counts.get(a).equals(counts.get(b))
                            ? Integer.compare(a, b)
                            : Integer.compare(counts.get(b), counts.get(a)));

                    int[] answer = new int[Math.min(k, ranked.size())];
                    for (int i = 0; i < answer.length; i++) {
                        answer[i] = ranked.get(i);
                    }
                    return answer;
                }
            }
            """;

    private static final String TOP_K_FREQUENT_ELEMENTS_PYTHON =
            """
            from collections import Counter
            from typing import List


            class Solution:
                def topKFrequent(self, nums: List[int], k: int) -> List[int]:
                    counts = Counter(nums)
                    # Most frequent first, then smallest value first so ties are reproducible.
                    ranked = sorted(counts, key=lambda value: (-counts[value], value))
                    return ranked[:k]
            """;

    private static final String TOP_K_FREQUENT_ELEMENTS_JS =
            """
            function topKFrequent(nums, k) {
              const counts = new Map();
              for (const value of nums) {
                counts.set(value, (counts.get(value) || 0) + 1);
              }

              // Most frequent first, then smallest value first so ties are reproducible.
              const ranked = Array.from(counts.keys());
              ranked.sort(function (a, b) {
                return counts.get(b) - counts.get(a) || a - b;
              });

              return ranked.slice(0, k);
            }
            """;

    private static final String TOP_K_FREQUENT_ELEMENTS_TS =
            """
            function topKFrequent(nums: number[], k: number): number[] {
              const counts = new Map<number, number>();
              for (const value of nums) {
                counts.set(value, (counts.get(value) || 0) + 1);
              }

              // Most frequent first, then smallest value first so ties are reproducible.
              const ranked: number[] = Array.from(counts.keys());
              ranked.sort(function (a, b) {
                return (counts.get(b) as number) - (counts.get(a) as number) || a - b;
              });

              return ranked.slice(0, k);
            }
            """;

    private static final String TRAPPING_RAIN_WATER_MD =
            """
            ### What holds water above one bar

            Water sitting on top of bar `i` rises to the lower of the two walls beside it:
            the tallest bar to its left and the tallest to its right. So the depth at `i` is
            `min(tallestLeft, tallestRight) - height[i]`.

            Computing those two maxima by rescanning at each index is quadratic and will not
            finish on the largest case. Two prefix arrays fix that in `O(n)` time and `O(n)`
            memory. You can do better.

            ### Two pointers

            Walk inwards from both ends, carrying the tallest bar seen from each side. The
            trick: if the left bar is shorter than the right one, then *whatever* is between
            them, the left side is the binding constraint — a taller wall exists somewhere
            on the right, so the left running maximum alone decides the water at the left
            pointer. Settle that column and step inward.

            That makes each step decidable with two numbers, so no arrays are needed at all.

            **Watch for:** the ends. The first and last bars have no outer wall, so they hold
            nothing — which falls out of the loop condition rather than needing a case.
            """;

    private static final String TRAPPING_RAIN_WATER_JAVA =
            """
            class Solution {
                public int trap(int[] height) {
                    int left = 0;
                    int right = height.length - 1;
                    int tallestLeft = 0;
                    int tallestRight = 0;
                    int water = 0;

                    while (left < right) {
                        if (height[left] < height[right]) {
                            tallestLeft = Math.max(tallestLeft, height[left]);
                            water += tallestLeft - height[left];
                            left++;
                        } else {
                            tallestRight = Math.max(tallestRight, height[right]);
                            water += tallestRight - height[right];
                            right--;
                        }
                    }
                    return water;
                }
            }
            """;

    private static final String TRAPPING_RAIN_WATER_PYTHON =
            """
            from typing import List


            class Solution:
                def trap(self, height: List[int]) -> int:
                    left, right = 0, len(height) - 1
                    tallest_left = tallest_right = 0
                    water = 0

                    while left < right:
                        if height[left] < height[right]:
                            tallest_left = max(tallest_left, height[left])
                            water += tallest_left - height[left]
                            left += 1
                        else:
                            tallest_right = max(tallest_right, height[right])
                            water += tallest_right - height[right]
                            right -= 1

                    return water
            """;

    private static final String TRAPPING_RAIN_WATER_JS =
            """
            function trap(height) {
              let left = 0;
              let right = height.length - 1;
              let tallestLeft = 0;
              let tallestRight = 0;
              let water = 0;

              while (left < right) {
                if (height[left] < height[right]) {
                  tallestLeft = Math.max(tallestLeft, height[left]);
                  water += tallestLeft - height[left];
                  left++;
                } else {
                  tallestRight = Math.max(tallestRight, height[right]);
                  water += tallestRight - height[right];
                  right--;
                }
              }
              return water;
            }
            """;

    private static final String TRAPPING_RAIN_WATER_TS =
            """
            function trap(height: number[]): number {
              let left = 0;
              let right = height.length - 1;
              let tallestLeft = 0;
              let tallestRight = 0;
              let water = 0;

              while (left < right) {
                if (height[left] < height[right]) {
                  tallestLeft = Math.max(tallestLeft, height[left]);
                  water += tallestLeft - height[left];
                  left++;
                } else {
                  tallestRight = Math.max(tallestRight, height[right]);
                  water += tallestRight - height[right];
                  right--;
                }
              }
              return water;
            }
            """;

    private static final String MEDIAN_OF_TWO_SORTED_ARRAYS_MD =
            """
            ### Do not merge

            Merging is `O(m + n)` and answers the question, but the problem asks for
            logarithmic time. That rules out touching every element, so you have to find the
            answer positionally instead.

            ### Binary search the split

            The median is defined by a *partition*: cut both arrays so that the combined
            left half has `(m + n + 1) / 2` elements and every value on the left is at most
            every value on the right.

            Choosing the cut in one array fixes it in the other, so there is only one
            variable. Binary search it over the shorter array — that is what makes the bound
            `log min(m, n)`.

            A cut is correct when `leftA <= rightB` and `leftB <= rightA`. If `leftA` is too
            big, move the cut left; otherwise move it right. Once it is correct: for an odd
            total the median is the larger left element, and for an even total it is the
            average of the larger left and the smaller right.

            **Watch for:** cuts at the very edge, where there is no element to compare. Treat
            a missing left element as negative infinity and a missing right element as
            positive infinity, and every comparison keeps working — which is how an empty
            input array needs no case of its own.
            """;

    private static final String MEDIAN_OF_TWO_SORTED_ARRAYS_JAVA =
            """
            class Solution {
                public double findMedianSortedArrays(int[] nums1, int[] nums2) {
                    // Search the shorter array, so the loop is O(log min(m, n)).
                    if (nums1.length > nums2.length) {
                        return findMedianSortedArrays(nums2, nums1);
                    }

                    int m = nums1.length;
                    int n = nums2.length;
                    int leftHalf = (m + n + 1) / 2;
                    int low = 0;
                    int high = m;

                    while (low <= high) {
                        int cutA = (low + high) / 2;
                        int cutB = leftHalf - cutA;

                        double leftA = cutA == 0 ? Double.NEGATIVE_INFINITY : nums1[cutA - 1];
                        double rightA = cutA == m ? Double.POSITIVE_INFINITY : nums1[cutA];
                        double leftB = cutB == 0 ? Double.NEGATIVE_INFINITY : nums2[cutB - 1];
                        double rightB = cutB == n ? Double.POSITIVE_INFINITY : nums2[cutB];

                        if (leftA <= rightB && leftB <= rightA) {
                            if ((m + n) % 2 == 1) {
                                return Math.max(leftA, leftB);
                            }
                            return (Math.max(leftA, leftB) + Math.min(rightA, rightB)) / 2.0;
                        }
                        if (leftA > rightB) {
                            high = cutA - 1;
                        } else {
                            low = cutA + 1;
                        }
                    }
                    return 0.0;
                }
            }
            """;

    private static final String MEDIAN_OF_TWO_SORTED_ARRAYS_PYTHON =
            """
            from typing import List


            class Solution:
                def findMedianSortedArrays(self, nums1: List[int], nums2: List[int]) -> float:
                    # Search the shorter array, so the loop is O(log min(m, n)).
                    if len(nums1) > len(nums2):
                        nums1, nums2 = nums2, nums1

                    m, n = len(nums1), len(nums2)
                    left_half = (m + n + 1) // 2
                    low, high = 0, m

                    while low <= high:
                        cut_a = (low + high) // 2
                        cut_b = left_half - cut_a

                        left_a = float("-inf") if cut_a == 0 else nums1[cut_a - 1]
                        right_a = float("inf") if cut_a == m else nums1[cut_a]
                        left_b = float("-inf") if cut_b == 0 else nums2[cut_b - 1]
                        right_b = float("inf") if cut_b == n else nums2[cut_b]

                        if left_a <= right_b and left_b <= right_a:
                            if (m + n) % 2 == 1:
                                return float(max(left_a, left_b))
                            return (max(left_a, left_b) + min(right_a, right_b)) / 2

                        if left_a > right_b:
                            high = cut_a - 1
                        else:
                            low = cut_a + 1

                    return 0.0
            """;

    private static final String MEDIAN_OF_TWO_SORTED_ARRAYS_JS =
            """
            function findMedianSortedArrays(nums1, nums2) {
              // Search the shorter array, so the loop is O(log min(m, n)).
              if (nums1.length > nums2.length) {
                return findMedianSortedArrays(nums2, nums1);
              }

              const m = nums1.length;
              const n = nums2.length;
              const leftHalf = (m + n + 1) >> 1;
              let low = 0;
              let high = m;

              while (low <= high) {
                const cutA = (low + high) >> 1;
                const cutB = leftHalf - cutA;

                const leftA = cutA === 0 ? -Infinity : nums1[cutA - 1];
                const rightA = cutA === m ? Infinity : nums1[cutA];
                const leftB = cutB === 0 ? -Infinity : nums2[cutB - 1];
                const rightB = cutB === n ? Infinity : nums2[cutB];

                if (leftA <= rightB && leftB <= rightA) {
                  if ((m + n) % 2 === 1) {
                    return Math.max(leftA, leftB);
                  }
                  return (Math.max(leftA, leftB) + Math.min(rightA, rightB)) / 2;
                }
                if (leftA > rightB) {
                  high = cutA - 1;
                } else {
                  low = cutA + 1;
                }
              }
              return 0;
            }
            """;

    private static final String MEDIAN_OF_TWO_SORTED_ARRAYS_TS =
            """
            function findMedianSortedArrays(nums1: number[], nums2: number[]): number {
              // Search the shorter array, so the loop is O(log min(m, n)).
              if (nums1.length > nums2.length) {
                return findMedianSortedArrays(nums2, nums1);
              }

              const m = nums1.length;
              const n = nums2.length;
              const leftHalf = (m + n + 1) >> 1;
              let low = 0;
              let high = m;

              while (low <= high) {
                const cutA = (low + high) >> 1;
                const cutB = leftHalf - cutA;

                const leftA = cutA === 0 ? -Infinity : nums1[cutA - 1];
                const rightA = cutA === m ? Infinity : nums1[cutA];
                const leftB = cutB === 0 ? -Infinity : nums2[cutB - 1];
                const rightB = cutB === n ? Infinity : nums2[cutB];

                if (leftA <= rightB && leftB <= rightA) {
                  if ((m + n) % 2 === 1) {
                    return Math.max(leftA, leftB);
                  }
                  return (Math.max(leftA, leftB) + Math.min(rightA, rightB)) / 2;
                }
                if (leftA > rightB) {
                  high = cutA - 1;
                } else {
                  low = cutA + 1;
                }
              }
              return 0;
            }
            """;

    /** Editorials by problem slug, in catalogue order. */
    static Map<String, Draft> build() {
        Map<String, Draft> editorials = new LinkedHashMap<>();

        editorials.put(
                "two-sum",
                new Draft(
                        TWO_SUM_MD,
                        "O(n)",
                        "O(n)",
                        solutions(TWO_SUM_JAVA, TWO_SUM_PYTHON, TWO_SUM_JS, TWO_SUM_TS)));

        editorials.put(
                "valid-parentheses",
                new Draft(
                        VALID_PARENTHESES_MD,
                        "O(n)",
                        "O(n)",
                        solutions(VALID_PARENTHESES_JAVA, VALID_PARENTHESES_PYTHON, VALID_PARENTHESES_JS, VALID_PARENTHESES_TS)));

        editorials.put(
                "merge-two-sorted-lists",
                new Draft(
                        MERGE_TWO_SORTED_LISTS_MD,
                        "O(m + n)",
                        "O(m + n)",
                        solutions(MERGE_TWO_SORTED_LISTS_JAVA, MERGE_TWO_SORTED_LISTS_PYTHON, MERGE_TWO_SORTED_LISTS_JS, MERGE_TWO_SORTED_LISTS_TS)));

        editorials.put(
                "best-time-to-buy-and-sell-stock",
                new Draft(
                        BEST_TIME_TO_BUY_AND_SELL_STOCK_MD,
                        "O(n)",
                        "O(1)",
                        solutions(BEST_TIME_TO_BUY_AND_SELL_STOCK_JAVA, BEST_TIME_TO_BUY_AND_SELL_STOCK_PYTHON, BEST_TIME_TO_BUY_AND_SELL_STOCK_JS, BEST_TIME_TO_BUY_AND_SELL_STOCK_TS)));

        editorials.put(
                "longest-substring-without-repeating-characters",
                new Draft(
                        LONGEST_SUBSTRING_WITHOUT_REPEATING_CHARACTERS_MD,
                        "O(n)",
                        "O(k), for k distinct characters",
                        solutions(LONGEST_SUBSTRING_WITHOUT_REPEATING_CHARACTERS_JAVA, LONGEST_SUBSTRING_WITHOUT_REPEATING_CHARACTERS_PYTHON, LONGEST_SUBSTRING_WITHOUT_REPEATING_CHARACTERS_JS, LONGEST_SUBSTRING_WITHOUT_REPEATING_CHARACTERS_TS)));

        editorials.put(
                "course-schedule",
                new Draft(
                        COURSE_SCHEDULE_MD,
                        "O(V + E)",
                        "O(V + E)",
                        solutions(COURSE_SCHEDULE_JAVA, COURSE_SCHEDULE_PYTHON, COURSE_SCHEDULE_JS, COURSE_SCHEDULE_TS)));

        editorials.put(
                "longest-increasing-subsequence",
                new Draft(
                        LONGEST_INCREASING_SUBSEQUENCE_MD,
                        "O(n log n)",
                        "O(n)",
                        solutions(LONGEST_INCREASING_SUBSEQUENCE_JAVA, LONGEST_INCREASING_SUBSEQUENCE_PYTHON, LONGEST_INCREASING_SUBSEQUENCE_JS, LONGEST_INCREASING_SUBSEQUENCE_TS)));

        editorials.put(
                "top-k-frequent-elements",
                new Draft(
                        TOP_K_FREQUENT_ELEMENTS_MD,
                        "O(n log n)",
                        "O(n)",
                        solutions(TOP_K_FREQUENT_ELEMENTS_JAVA, TOP_K_FREQUENT_ELEMENTS_PYTHON, TOP_K_FREQUENT_ELEMENTS_JS, TOP_K_FREQUENT_ELEMENTS_TS)));

        editorials.put(
                "trapping-rain-water",
                new Draft(
                        TRAPPING_RAIN_WATER_MD,
                        "O(n)",
                        "O(1)",
                        solutions(TRAPPING_RAIN_WATER_JAVA, TRAPPING_RAIN_WATER_PYTHON, TRAPPING_RAIN_WATER_JS, TRAPPING_RAIN_WATER_TS)));

        editorials.put(
                "median-of-two-sorted-arrays",
                new Draft(
                        MEDIAN_OF_TWO_SORTED_ARRAYS_MD,
                        "O(log min(m, n))",
                        "O(1)",
                        solutions(MEDIAN_OF_TWO_SORTED_ARRAYS_JAVA, MEDIAN_OF_TWO_SORTED_ARRAYS_PYTHON, MEDIAN_OF_TWO_SORTED_ARRAYS_JS, MEDIAN_OF_TWO_SORTED_ARRAYS_TS)));

        return editorials;
    }

    private static Map<Language, String> solutions(String java, String python, String javascript, String typescript) {
        Map<Language, String> byLanguage = new EnumMap<>(Language.class);
        byLanguage.put(Language.JAVA, java);
        byLanguage.put(Language.PYTHON, python);
        byLanguage.put(Language.JAVASCRIPT, javascript);
        byLanguage.put(Language.TYPESCRIPT, typescript);
        return byLanguage;
    }

    /** One authored editorial, before it is attached to a problem. */
    record Draft(String contentMarkdown, String timeComplexity, String spaceComplexity,
            Map<Language, String> solutions) {}
}
