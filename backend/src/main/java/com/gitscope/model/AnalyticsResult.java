package com.gitscope.model;

import java.util.List;
import java.util.Map;

/**
 * The complete analysis payload returned by {@code POST /api/analyze}. Every
 * insight the dashboard renders is contained here so the UI loads in a single
 * round-trip.
 */
public record AnalyticsResult(
        Summary summary,
        List<AuthorStat> authors,
        List<CountEntry> commitsByMonth,
        List<CountEntry> commitsByHour,
        List<CountEntry> commitsByWeekday,
        List<FileChurn> fileChurn,
        List<CountEntry> commitVerbs,
        List<BusFactorEntry> busFactor,
        List<CountEntry> fileTypes,
        List<LargestCommit> largestCommits,
        List<WeekdayByAuthor> weekdayByAuthor
) {

    /** Top-level repo summary numbers. */
    public record Summary(
            String name,
            long totalCommits,
            long totalAuthors,
            long totalLinesAdded,
            long totalLinesRemoved,
            long totalFiles,
            String firstCommitDate,
            String lastCommitDate,
            long activeDays
    ) {}

    /** Per-canonical-author statistics. */
    public record AuthorStat(
            String name,
            long commits,
            long added,
            long removed,
            long net,
            long filesTouched,
            long activeDays,
            List<String> aliases
    ) {}

    /** Generic labelled count, used for time/extension/verb breakdowns. */
    public record CountEntry(
            String key,
            long count
    ) {}

    /** A file and how many times it changed across history. */
    public record FileChurn(
            String path,
            long changes
    ) {}

    /** A file dominated entirely by one author (bus-factor risk). */
    public record BusFactorEntry(
            String path,
            String author,
            long commits
    ) {}

    /** One of the largest single commits by lines changed. */
    public record LargestCommit(
            String hash,
            String author,
            long total,
            String subject
    ) {}

    /** Per-author commit counts keyed by weekday (Mon..Sun). */
    public record WeekdayByAuthor(
            String author,
            Map<String, Long> counts
    ) {}
}
