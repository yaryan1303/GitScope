package com.gitscope.service;

import com.gitscope.model.AnalyticsResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Walks a repository's entire commit history exactly once and produces a
 * complete {@link AnalyticsResult}. Identity merging is delegated to
 * {@link IdentityResolver}.
 */
@Service
public class GitAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(GitAnalyticsService.class);

    private static final List<String> WEEKDAYS =
            List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final IdentityResolver identityResolver;

    public GitAnalyticsService(IdentityResolver identityResolver) {
        this.identityResolver = identityResolver;
    }

    /** Analyze a repository given an already-resolved working directory. */
    public AnalyticsResult analyzeLocal(String pathStr) {
        File dir = new File(pathStr);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Local path does not exist or is not a directory: " + pathStr);
        }
        try (Git git = Git.open(dir)) {
            return analyze(git, repoNameFromPath(dir));
        } catch (org.eclipse.jgit.errors.RepositoryNotFoundException e) {
            throw new IllegalArgumentException("No git repository found at: " + pathStr, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Not a valid git repository: " + pathStr, e);
        }
    }

    /** Clone a remote repository into a temp dir, analyze it, then delete it. */
    public AnalyticsResult analyzeRemote(String url) {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("gitscope-clone-");
        } catch (IOException e) {
            throw new RuntimeException("Could not create temp directory for clone", e);
        }
        try (Git git = Git.cloneRepository()
                .setURI(url)
                .setDirectory(tempDir.toFile())
                .setCloneAllBranches(true)
                .call()) {
            return analyze(git, repoNameFromUrl(url));
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone or analyze remote repo: " + e.getMessage(), e);
        } finally {
            deleteRecursively(tempDir.toFile());
        }
    }

    // ------------------------------------------------------------------
    // Core analysis
    // ------------------------------------------------------------------

    private AnalyticsResult analyze(Git git, String repoName) {
        Repository repo = git.getRepository();

        // Per-author aggregation keyed by canonical identity key.
        Map<String, AuthorAgg> authors = new HashMap<>();

        // Time/category breakdowns.
        Map<String, Long> byMonth = new TreeMap<>();           // YYYY-MM (sorted)
        long[] byHour = new long[24];
        Map<String, Long> byWeekday = new LinkedHashMap<>();
        WEEKDAYS.forEach(d -> byWeekday.put(d, 0L));

        Map<String, Long> fileChurn = new HashMap<>();
        Map<String, Long> verbs = new HashMap<>();
        // file path -> (canonical author key -> commits touching it)
        Map<String, Map<String, Long>> fileAuthorCounts = new HashMap<>();
        List<AnalyticsResult.LargestCommit> largest = new ArrayList<>();

        long totalAdded = 0;
        long totalRemoved = 0;
        long commitCount = 0;
        Set<LocalDate> repoActiveDays = new HashSet<>();
        ZonedDateTime firstDate = null;
        ZonedDateTime lastDate = null;

        try (RevWalk walk = new RevWalk(repo);
             ObjectReader reader = repo.newObjectReader();
             DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            df.setRepository(repo);
            df.setDiffComparator(RawTextComparator.DEFAULT);
            df.setDetectRenames(false);

            for (RevCommit raw : git.log().all().call()) {
                RevCommit commit = walk.parseCommit(raw.getId());

                // Skip merge commits.
                if (commit.getParentCount() > 1) {
                    continue;
                }

                PersonIdent who = commit.getAuthorIdent();
                String rawName = who.getName() == null ? "(unknown)" : who.getName();
                String key = identityResolver.key(rawName);

                ZonedDateTime when = who.getWhen().toInstant()
                        .atZone(who.getTimeZone().toZoneId());
                LocalDate day = when.toLocalDate();
                String weekday = WEEKDAYS.get(when.getDayOfWeek().getValue() - 1);

                // Diff against first parent (empty tree for the root commit).
                AbstractTreeIterator oldTree;
                if (commit.getParentCount() == 1) {
                    RevCommit parent = walk.parseCommit(commit.getParent(0).getId());
                    oldTree = treeIterator(reader, parent.getTree().getId());
                } else {
                    oldTree = new EmptyTreeIterator();
                }
                AbstractTreeIterator newTree = treeIterator(reader, commit.getTree().getId());

                long commitAdded = 0;
                long commitRemoved = 0;
                Set<String> filesInCommit = new HashSet<>();

                List<DiffEntry> diffs = df.scan(oldTree, newTree);
                for (DiffEntry de : diffs) {
                    String filePath = de.getChangeType() == DiffEntry.ChangeType.DELETE
                            ? de.getOldPath() : de.getNewPath();
                    filesInCommit.add(filePath);
                    fileChurn.merge(filePath, 1L, Long::sum);
                    fileAuthorCounts
                            .computeIfAbsent(filePath, k -> new HashMap<>())
                            .merge(key, 1L, Long::sum);

                    try {
                        FileHeader fh = df.toFileHeader(de);
                        for (Edit edit : fh.toEditList()) {
                            commitAdded += edit.getEndB() - edit.getBeginB();
                            commitRemoved += edit.getEndA() - edit.getBeginA();
                        }
                    } catch (IOException | RuntimeException ex) {
                        // Binary or unreadable blob — skip line counting for it.
                        log.debug("Skipping diff line-count for {} in {}: {}",
                                filePath, commit.getName(), ex.toString());
                    }
                }

                // ---- record everything ----
                commitCount++;
                totalAdded += commitAdded;
                totalRemoved += commitRemoved;
                repoActiveDays.add(day);

                if (firstDate == null || when.toInstant().isBefore(firstDate.toInstant())) {
                    firstDate = when;
                }
                if (lastDate == null || when.toInstant().isAfter(lastDate.toInstant())) {
                    lastDate = when;
                }

                String monthKey = String.format("%04d-%02d", when.getYear(), when.getMonthValue());
                byMonth.merge(monthKey, 1L, Long::sum);
                byHour[when.getHour()]++;
                byWeekday.merge(weekday, 1L, Long::sum);

                String verb = firstWord(commit.getShortMessage());
                if (!verb.isEmpty()) {
                    verbs.merge(verb, 1L, Long::sum);
                }

                largest.add(new AnalyticsResult.LargestCommit(
                        commit.getName().substring(0, 10),
                        key, // replaced with display name later
                        commitAdded + commitRemoved,
                        truncate(commit.getShortMessage(), 120)));

                AuthorAgg agg = authors.computeIfAbsent(key, k -> new AuthorAgg());
                agg.aliases.add(rawName);
                agg.commits++;
                agg.added += commitAdded;
                agg.removed += commitRemoved;
                agg.files.addAll(filesInCommit);
                agg.activeDays.add(day);
                agg.weekday.merge(weekday, 1L, Long::sum);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze repository history: " + e.getMessage(), e);
        }

        // Resolve canonical display names per author group.
        Map<String, String> keyToDisplay = new HashMap<>();
        for (Map.Entry<String, AuthorAgg> e : authors.entrySet()) {
            keyToDisplay.put(e.getKey(), identityResolver.displayName(e.getValue().aliases));
        }

        // Build author stats (sorted by net lines desc).
        List<AnalyticsResult.AuthorStat> authorStats = authors.entrySet().stream()
                .map(e -> {
                    AuthorAgg a = e.getValue();
                    List<String> aliasList = a.aliases.stream().sorted().collect(Collectors.toList());
                    return new AnalyticsResult.AuthorStat(
                            keyToDisplay.get(e.getKey()),
                            a.commits, a.added, a.removed, a.added - a.removed,
                            a.files.size(), a.activeDays.size(), aliasList);
                })
                .sorted(Comparator.comparingLong(AnalyticsResult.AuthorStat::net).reversed())
                .collect(Collectors.toList());

        // Map largest-commit author keys -> display names, keep top 15.
        List<AnalyticsResult.LargestCommit> largestCommits = largest.stream()
                .sorted(Comparator.comparingLong(AnalyticsResult.LargestCommit::total).reversed())
                .limit(15)
                .map(lc -> new AnalyticsResult.LargestCommit(
                        lc.hash(),
                        keyToDisplay.getOrDefault(lc.author(), lc.author()),
                        lc.total(),
                        lc.subject()))
                .collect(Collectors.toList());

        // File churn top 20.
        List<AnalyticsResult.FileChurn> churnTop = fileChurn.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .map(e -> new AnalyticsResult.FileChurn(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // Commit verbs top 15.
        List<AnalyticsResult.CountEntry> verbTop = verbs.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .map(e -> new AnalyticsResult.CountEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // Bus factor: files with a single 100%-dominant author, touched > once.
        List<AnalyticsResult.BusFactorEntry> busFactor = fileAuthorCounts.entrySet().stream()
                .filter(e -> e.getValue().size() == 1)
                .map(e -> {
                    String filePath = e.getKey();
                    Map.Entry<String, Long> only = e.getValue().entrySet().iterator().next();
                    return new AnalyticsResult.BusFactorEntry(
                            filePath, keyToDisplay.getOrDefault(only.getKey(), only.getKey()), only.getValue());
                })
                .filter(b -> b.commits() > 1)
                .sorted(Comparator.comparingLong(AnalyticsResult.BusFactorEntry::commits).reversed())
                .limit(50)
                .collect(Collectors.toList());

        // Time breakdowns -> ordered lists.
        List<AnalyticsResult.CountEntry> monthList = byMonth.entrySet().stream()
                .map(e -> new AnalyticsResult.CountEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        List<AnalyticsResult.CountEntry> hourList = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hourList.add(new AnalyticsResult.CountEntry(String.format("%02d", h), byHour[h]));
        }
        List<AnalyticsResult.CountEntry> weekdayList = WEEKDAYS.stream()
                .map(d -> new AnalyticsResult.CountEntry(d, byWeekday.getOrDefault(d, 0L)))
                .collect(Collectors.toList());

        // File types from the HEAD tree.
        FileTypeScan fts = scanHeadTree(repo);

        // Weekday-by-author (sorted by total commits desc).
        List<AnalyticsResult.WeekdayByAuthor> weekdayByAuthor = authors.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, AuthorAgg> e) -> e.getValue().commits).reversed())
                .map(e -> {
                    Map<String, Long> counts = new LinkedHashMap<>();
                    WEEKDAYS.forEach(d -> counts.put(d, e.getValue().weekday.getOrDefault(d, 0L)));
                    return new AnalyticsResult.WeekdayByAuthor(keyToDisplay.get(e.getKey()), counts);
                })
                .collect(Collectors.toList());

        AnalyticsResult.Summary summary = new AnalyticsResult.Summary(
                repoName,
                commitCount,
                authors.size(),
                totalAdded,
                totalRemoved,
                fts.totalFiles,
                firstDate == null ? null : firstDate.format(DATE_FMT),
                lastDate == null ? null : lastDate.format(DATE_FMT),
                repoActiveDays.size());

        return new AnalyticsResult(
                summary, authorStats, monthList, hourList, weekdayList,
                churnTop, verbTop, busFactor, fts.byExtension, largestCommits, weekdayByAuthor);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AbstractTreeIterator treeIterator(ObjectReader reader, ObjectId treeId) throws IOException {
        CanonicalTreeParser parser = new CanonicalTreeParser();
        parser.reset(reader, treeId);
        return parser;
    }

    private FileTypeScan scanHeadTree(Repository repo) {
        Map<String, Long> byExt = new HashMap<>();
        long total = 0;
        try {
            ObjectId head = repo.resolve("HEAD");
            if (head == null) {
                return new FileTypeScan(List.of(), 0);
            }
            try (RevWalk walk = new RevWalk(repo)) {
                RevCommit headCommit = walk.parseCommit(head);
                try (TreeWalk tw = new TreeWalk(repo)) {
                    tw.addTree(headCommit.getTree());
                    tw.setRecursive(true);
                    while (tw.next()) {
                        total++;
                        byExt.merge(extensionOf(tw.getPathString()), 1L, Long::sum);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not scan HEAD tree for file types: {}", e.toString());
        }
        List<AnalyticsResult.CountEntry> list = byExt.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new AnalyticsResult.CountEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        return new FileTypeScan(list, total);
    }

    private static String extensionOf(String path) {
        String name = path;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "(none)";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String firstWord(String message) {
        if (message == null) {
            return "";
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String first = trimmed.split("\\s+")[0];
        // Keep letters only, lower-cased (e.g. "Fixed:" -> "fixed").
        first = first.replaceAll("[^A-Za-z]", "").toLowerCase(Locale.ROOT);
        return first;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String repoNameFromPath(File dir) {
        try {
            File canonical = dir.getCanonicalFile();
            String name = canonical.getName();
            return name.isEmpty() ? "repository" : name;
        } catch (IOException e) {
            return dir.getName();
        }
    }

    private static String repoNameFromUrl(String url) {
        String u = url.trim();
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        if (u.endsWith(".git")) {
            u = u.substring(0, u.length() - 4);
        }
        int slash = Math.max(u.lastIndexOf('/'), u.lastIndexOf(':'));
        String name = slash >= 0 ? u.substring(slash + 1) : u;
        return name.isEmpty() ? "repository" : name;
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            file.deleteOnExit();
        }
    }

    /** Mutable per-author accumulator used during the single history walk. */
    private static final class AuthorAgg {
        final Set<String> aliases = new HashSet<>();
        final Set<String> files = new HashSet<>();
        final Set<LocalDate> activeDays = new HashSet<>();
        final Map<String, Long> weekday = new HashMap<>();
        long commits;
        long added;
        long removed;
    }

    /** Result of scanning the HEAD tree for file-type breakdown. */
    private record FileTypeScan(List<AnalyticsResult.CountEntry> byExtension, long totalFiles) {}
}
