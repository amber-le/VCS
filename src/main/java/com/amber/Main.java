package com.amber;

import com.amber.algo.Levenshtein;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException, GitAPIException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input the repo path:");
//        String repoPath = scanner.nextLine();
//        String repoPath = "/Users/fox/projects/parse-gitlog";
        String repoPath = "/Users/fox/projects/earendil";
        Repository repository = new RepositoryBuilder()
                .setGitDir(new File(repoPath + "/.git"))
                .build();

        Git git = new Git(repository);
        Map<String, BlameFileOutput> stringBlameFileOutputMap = gitLogByLine(repoPath, repository, git);
        System.out.println(stringBlameFileOutputMap);

        Map<String, AuthorInfo> allAuthorMapByEmail = getAllAuthorMapByEmail(git);
        System.out.println("Authors:");
        System.out.println(allAuthorMapByEmail);

//        StringBuilder outCsv = new StringBuilder();
//        outCsv.append("");
        Map<String, BlameFileOutput> blameOutput = gitBlame(repository, git);
        System.out.println(blameOutput);
//        outCsv.append("file_path, author, line_contributed\n");
//        for (Map.Entry<String, BlameFileOutput> blameStats : blameOutput.entrySet()) {
//            String key = blameStats.getKey();
//            BlameFileOutput value = blameStats.getValue();
//
//            for (Map.Entry<String, Integer> authorLineCount : value.authorLineCount.entrySet()) {
//                outCsv
//                        .append(key).append(",")
//                        .append(authorLineCount.getKey()).append(",")
//                        .append(authorLineCount.getValue()).append("\n");
//            }
//
//        }
//        outCsv.append("\n\n\n\n");
//        outCsv.append("author, line_added, line_removed, commit_count\n");
////        gitDiff(git, repoPath);
//        Map<String, AuthorStatsOutput> authorStatsOutput = getAuthorStatsOutput(repository, git);
//        for (Map.Entry<String, AuthorStatsOutput> authorStat : authorStatsOutput.entrySet()) {
//            authorStat.getKey();
//            outCsv
//                    .append(authorStat.getKey()).append(",")
//                    .append(authorStat.getValue().getLineAdded()).append(",")
//                    .append(authorStat.getValue().getLineRemoved()).append(",")
//                    .append(authorStat.getValue().getCommitCount()).append("\n");
//
//        }
//
//
//        System.out.println(outCsv.toString());

    }

    private static void gitDiff(Git git, String repoPath) throws GitAPIException, IOException {
        LogCommand log = git.log();
        Iterable<RevCommit> commits = log.call();
        String nextCommitId = "";

        for (RevCommit commit : commits) {

            if (!nextCommitId.isEmpty()) {
//                System.out.println(commit.getAuthorIdent());
                runDiffCommand(repoPath, commit.getId().name(), nextCommitId);
            }

            nextCommitId = commit.getId().name();
        }
    }

    private static Map<String, AuthorStatsOutput> getAuthorStatsOutput(Repository repository, Git git) throws GitAPIException, IOException {
        Map<String, AuthorStatsOutput> result = new HashMap<>();
        Iterable<RevCommit> commits = git.log().call();
        for (RevCommit commit : commits) {
            PersonIdent authorIdent = commit.getAuthorIdent();
            AuthorStatsOutput authorStatsOutput = result.get(authorIdent.getEmailAddress());
            if (authorStatsOutput == null) {
                authorStatsOutput = new AuthorStatsOutput(
                        new AuthorInfo(
                                authorIdent.getEmailAddress(),
                                authorIdent.getName()
                        )
                );
                result.put(authorIdent.getEmailAddress(), authorStatsOutput);
            }


            AbstractTreeIterator canonicalTreeParser = getCanonicalTreeParser(git, commit.getId());
            AbstractTreeIterator parentTreeParser = null;
            if (commit.getParentCount() > 0) {
                parentTreeParser = getCanonicalTreeParser(git, commit.getParent(0).getId());
            } else {
                parentTreeParser = new CanonicalTreeParser();
                parentTreeParser.reset();
            }

            OutputStream outputStream = DisabledOutputStream.INSTANCE;
            try (DiffFormatter formatter = new DiffFormatter(outputStream)) {
                formatter.setRepository(repository);
                List<DiffEntry> diffEntries = formatter.scan(parentTreeParser, canonicalTreeParser);
                for (DiffEntry diffEntry : diffEntries) {
//                    System.out.println(commit.getId());
//                    System.out.println(diffEntry.getChangeType());
//                    System.out.println(diffEntry.getNewPath());
//                    System.out.println(diffEntry.getDiffAttribute());
                    FileHeader fileHeader = formatter.toFileHeader(diffEntry);

                    EditList editList = fileHeader.toEditList();
                    int added = 0;
                    int removed = 0;
                    for (Edit edit : editList) {
//                        System.out.println(edit);
//                        System.out.println("deleted:");
//                        System.out.println(edit.getEndA() - edit.getBeginA());
                        removed += edit.getEndA() - edit.getBeginA();

//                        System.out.println("added:");
//                        System.out.println(edit.getEndB() - edit.getBeginB());
                        added += edit.getEndB() - edit.getBeginB();
                    }
                    authorStatsOutput.setLineAdded(authorStatsOutput.lineAdded + added);
                    authorStatsOutput.setLineRemoved(authorStatsOutput.lineRemoved + removed);
                }
            }


            authorStatsOutput.setCommitCount(authorStatsOutput.getCommitCount() + 1);
        }
        return result;
    }

    private static AbstractTreeIterator getCanonicalTreeParser(Git git, ObjectId commitId) throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit commit = walk.parseCommit(commitId);
            ObjectId treeId = commit.getTree().getId();
            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                return new CanonicalTreeParser(null, reader, treeId);
            }
        }
    }

    private static Map<String, AuthorInfo> getAllAuthorMapByEmail(Git git) throws GitAPIException {
        Map<String, AuthorInfo> result = new HashMap<>();
        Iterable<RevCommit> commits = git.log().call();
        for (RevCommit commit : commits) {
            PersonIdent authorIdent = commit.getAuthorIdent();
            result.put(
                    authorIdent.getEmailAddress(),
                    new AuthorInfo(
                            authorIdent.getEmailAddress(),
                            authorIdent.getName()
                    )
            );


        }
        return result;
    }

    private static Map<String, BlameFileOutput> gitLogByLine(String repoPath, Repository repository, Git git) throws IOException, GitAPIException {
        System.out.println("--- GIT Log By Line ---");
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.setRecursive(true);
        treeWalk.addTree(git.log().call().iterator().next().getTree());

        Levenshtein levenshtein = new Levenshtein();
        Map<String, BlameFileOutput> blameOutput = new HashMap<>();

        while (treeWalk.next()) {
            if (treeWalk.isSubtree()) {
                continue;
            }
            String filePathStr = treeWalk.getPathString();
            int noLines = countLines(repoPath + "/" + filePathStr);

            BlameFileOutput fileBlameOutput = new BlameFileOutput(filePathStr);
            blameOutput.put(filePathStr, fileBlameOutput);

            int curLine = 1;
            while (curLine <= noLines) {
                List<CommitInfo> commitInfos = runLogCommand(repoPath, filePathStr, curLine);

                for (CommitInfo commitInfo : commitInfos) {
                    double distance = levenshtein.distance(
                            String.join("\n", commitInfo.getLinesAdded()),
                            String.join("\n", commitInfo.getLinesRemoved())
                    );
                    if (distance > 0.5 || commitInfo.getLinesRemoved().isEmpty()) {
                        fileBlameOutput.addLine(commitInfo.getAuthor().email);
                        break;
                    }
                }
                curLine++;
            }
        }
        return blameOutput;
    }

    public static int countLines(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            int lines = 0;
            while (reader.readLine() != null) {
                lines++;
            }
            return lines;
        }
    }

    private static Map<String, BlameFileOutput> gitBlame(Repository repository, Git git) throws IOException, GitAPIException {
        System.out.println("--- GIT BLAME ---");
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.setRecursive(true);
        treeWalk.addTree(git.log().call().iterator().next().getTree());

        Map<String, BlameFileOutput> blameOutput = new HashMap<>();

        while (treeWalk.next()) {
            if (treeWalk.isSubtree()) {
                continue;
            }
            String filePathStr = treeWalk.getPathString();

            BlameCommand b = new BlameCommand(repository);
            b.setFilePath(filePathStr);

            BlameFileOutput fileBlameOutput = new BlameFileOutput(filePathStr);
            blameOutput.put(filePathStr, fileBlameOutput);

//            System.out.println("blame -- " + filePathStr);
            BlameResult blameCommand = b.call();
            blameCommand.computeAll();
//            System.out.println(blameCommand.lastLength());

            int i = 0;
            try {
                while (blameCommand.getSourceAuthor(i) != null) {
                    fileBlameOutput.addLine(blameCommand.getSourceAuthor(i).getEmailAddress());
//                    System.out.println(blameCommand.getSourceAuthor(i));
//                    System.out.println(blameCommand.getSourceCommit(i));
//                    System.out.println(blameCommand.getSourcePath(i));
//                    System.out.println(blameCommand.getSourceCommitter(i));
//                    System.out.println("---------");
                    i++;
                }
            } catch (IndexOutOfBoundsException e) {
            }
        }
        System.out.println("--- end GIT BLAME ---");
        return blameOutput;
    }

    private static void runDiffCommand(String repoPath, String previousCommitId, String currentCommitId) throws IOException {
        String gitCommand = "git diff --word-diff " + previousCommitId + " " + currentCommitId;
        System.out.println("GIT_COMMAND:" + gitCommand);
        System.out.println("changes in commit: " + currentCommitId);
        // Create a ProcessBuilder
        ProcessBuilder processBuilder = new ProcessBuilder(gitCommand.split(" "));
        processBuilder.redirectErrorStream(true);

        // Set the working directory to the Git repository directory
        processBuilder.directory(new File(repoPath));

        // Start the process
        Process process = processBuilder.start();

        // Capture and process the output
        List<String> diffOutput = captureProcessOutput(process.getInputStream());

        // Print the Git diff result
        for (String line : diffOutput) {
            System.out.println(line);
        }
    }

    private static List<CommitInfo> runLogCommand(String repoPath, String filePath, int lineNo) throws IOException {
        String gitCommand = "git log --no-color -L" + lineNo + ",+1:" + filePath;
//        System.out.println("GIT_LOG_COMMAND:" + gitCommand);
        // Create a ProcessBuilder

        ProcessBuilder processBuilder = new ProcessBuilder(gitCommand.split(" "));
        processBuilder.directory(new File(repoPath));
        processBuilder.redirectErrorStream(true);

        // Set the working directory to the Git repository directory
        processBuilder.directory(new File(repoPath));

        // Start the process
        Process process = processBuilder.start();

        // Capture and process the output
        List<String> diffOutput = captureProcessOutput(process.getInputStream());
        List<CommitInfo> commitInfos = parseGitLog(String.join("\n", diffOutput));
        return commitInfos;
    }

    private static List<String> captureProcessOutput(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.replaceAll("[^\\x00-\\x7F]+", ""));
            }
        }
        return lines;
    }


    public static class BlameFileOutput {
        String filePath;
        Map<String, Integer> authorLineCount = new HashMap<>();

        public BlameFileOutput(String filePath) {
            this.filePath = filePath;
        }

        public void addLine(String authorName) {
            if (!authorLineCount.containsKey(authorName)) {
                authorLineCount.put(authorName, 0);
            }

            authorLineCount.put(authorName, authorLineCount.get(authorName) + 1);
        }

        @Override
        public String toString() {
            return "BlameFileOutput{" +
                    "filePath='" + filePath + '\'' +
                    ", authorLineCount=" + authorLineCount +
                    '}';
        }
    }

    public static class AuthorStatsOutput {
        AuthorInfo authorInfo;
        int commitCount = 0;
        int lineAdded = 0;
        int lineRemoved = 0;


        public AuthorStatsOutput(AuthorInfo authorInfo) {
            this.authorInfo = authorInfo;
        }

        public int getCommitCount() {
            return commitCount;
        }

        public void setCommitCount(int commitCount) {
            this.commitCount = commitCount;
        }

        public int getLineAdded() {
            return lineAdded;
        }

        public void setLineAdded(int lineAdded) {
            this.lineAdded = lineAdded;
        }

        public int getLineRemoved() {
            return lineRemoved;
        }

        public void setLineRemoved(int lineRemoved) {
            this.lineRemoved = lineRemoved;
        }

        @Override
        public String toString() {
            return "AuthorStatsOutput{" +
                    "authorInfo=" + authorInfo +
                    ", commitCount=" + commitCount +
                    ", lineAdded=" + lineAdded +
                    ", lineRemoved=" + lineRemoved +
                    '}';
        }
    }

    public static class AuthorInfo {
        String email;
        String name;

        public AuthorInfo(String email, String name) {
            this.email = email;
            this.name = name;
        }

        @Override
        public String toString() {
            return "AuthorInfo{" +
                    "email='" + email + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    static List<CommitInfo> parseGitLog(String gitLog) {
        List<CommitInfo> commits = new ArrayList<>();

        String[] lines = gitLog.split("\n");
        CommitInfo currentCommit = null;

        Pattern commitPattern = Pattern.compile("^commit (\\w+)$");
        Pattern authorPattern = Pattern.compile("^Author: (.+)$");
        Pattern diffPattern = Pattern.compile("^[+\\-] (.*)$");

        for (String line : lines) {
            Matcher commitMatcher = commitPattern.matcher(line);
            Matcher authorMatcher = authorPattern.matcher(line);
            Matcher diffMatcher = diffPattern.matcher(line);

            if (commitMatcher.matches()) {
                if (currentCommit != null) {
                    commits.add(new CommitInfo(currentCommit));
                }
                currentCommit = new CommitInfo();
                currentCommit.setCommitId(commitMatcher.group(1));
            } else if (authorMatcher.matches()) {
                currentCommit.setAuthor(extractAuthorFromStr(authorMatcher.group(1)));
            } else if (line.startsWith("+") || line.startsWith("-")
            ) {
                if (!line.startsWith("+++") && !line.startsWith("---")) {
                    if (line.startsWith("+")) {
                        currentCommit.getLinesAdded().add(line.substring(1));
                    } else if (line.startsWith("-")) {
                        currentCommit.getLinesRemoved().add(line.substring(1));
                    }
                }
            }
        }

        // Add the last commit to the list
        if (currentCommit != null) {
            commits.add(new CommitInfo(currentCommit));
        }

        return commits;
    }

    private static AuthorInfo extractAuthorFromStr(String authorStr) {
        // Define the regex pattern
        Pattern pattern = Pattern.compile("([^<]+)\\s*<([^>]+)>");

        // Create a Matcher object
        Matcher matcher = pattern.matcher(authorStr);
        matcher.matches();

        // Check if the pattern matches
        // Extract the name and email
        String name = matcher.group(1).trim();
        String email = matcher.group(2).trim();
        return new AuthorInfo(email, name);
    }

    static class CommitInfo {
        private String commitId;
        private AuthorInfo author;
        private List<String> linesAdded;
        private List<String> linesRemoved;

        public CommitInfo() {
            linesAdded = new ArrayList<>();
            linesRemoved = new ArrayList<>();
        }

        public CommitInfo(CommitInfo other) {
            this.commitId = other.commitId;
            this.author = other.author;
            this.linesAdded = new ArrayList<>(other.linesAdded);
            this.linesRemoved = new ArrayList<>(other.linesRemoved);
        }

        public String getCommitId() {
            return commitId;
        }

        public void setCommitId(String commitId) {
            this.commitId = commitId;
        }

        public AuthorInfo getAuthor() {
            return author;
        }

        public void setAuthor(AuthorInfo author) {
            this.author = author;
        }

        public List<String> getLinesAdded() {
            return linesAdded;
        }

        public List<String> getLinesRemoved() {
            return linesRemoved;
        }

        @Override
        public String toString() {
            return "CommitInfo{" +
                    "commitId='" + commitId + '\'' +
                    ", author='" + author + '\'' +
                    ", linesAdded=" + linesAdded +
                    ", linesRemoved=" + linesRemoved +
                    '}';
        }
    }

}
