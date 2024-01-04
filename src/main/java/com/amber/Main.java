package com.amber;

import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.DiffCommand;
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
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.FooterLine;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.*;
import java.util.*;

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

        Map<String, AuthorInfo> allAuthorMapByEmail = getAllAuthorMapByEmail(git);
        System.out.println("Authors:");
        System.out.println(allAuthorMapByEmail);

        StringBuilder outCsv = new StringBuilder();
        outCsv.append("");
        Map<String, BlameFileOutput> blameOutput = gitBlame(repository, git);
        System.out.println(blameOutput);
        outCsv.append("file_path, author, line_contributed\n");
        for (Map.Entry<String, BlameFileOutput> blameStats : blameOutput.entrySet()) {
            String key = blameStats.getKey();
            BlameFileOutput value = blameStats.getValue();

            for (Map.Entry<String, Integer> authorLineCount : value.authorLineCount.entrySet()) {
                outCsv
                        .append(key).append(",")
                        .append(authorLineCount.getKey()).append(",")
                        .append(authorLineCount.getValue()).append("\n");
            }

        }
        outCsv.append("\n\n\n\n");
        outCsv.append("author, line_added, line_removed, commit_count\n");
//        gitDiff(git, repoPath);
        Map<String, AuthorStatsOutput> authorStatsOutput = getAuthorStatsOutput(repository, git);
        for (Map.Entry<String, AuthorStatsOutput> authorStat : authorStatsOutput.entrySet()) {
            authorStat.getKey();
            outCsv
                    .append(authorStat.getKey()).append(",")
                    .append(authorStat.getValue().getLineAdded()).append(",")
                    .append(authorStat.getValue().getLineRemoved()).append(",")
                    .append(authorStat.getValue().getCommitCount()).append("\n");

        }


        System.out.println(outCsv.toString());

    }

    private static void gitDiff(Git git, String repoPath) throws GitAPIException, IOException {
        LogCommand log = git.log();
        Iterable<RevCommit> commits = log.call();
        String nextCommitId = "";

        for (RevCommit commit : commits) {

            if (!nextCommitId.isEmpty()) {
                System.out.println(commit.getAuthorIdent());
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
                    System.out.println(commit.getId());
                    System.out.println(diffEntry.getChangeType());
                    System.out.println(diffEntry.getNewPath());
                    System.out.println(diffEntry.getDiffAttribute());
                    FileHeader fileHeader = formatter.toFileHeader(diffEntry);

                    EditList editList = fileHeader.toEditList();
                    for (HunkHeader hunk : fileHeader.getHunks()) {
                        System.out.println(hunk);
                    }
                    int added = 0;
                    int removed = 0;
                    for (Edit edit : editList) {
                        System.out.println(edit);
                        System.out.println("deleted:");
                        System.out.println(edit.getEndA() - edit.getBeginA());
                        removed += edit.getEndA() - edit.getBeginA();

                        System.out.println("added:");
                        System.out.println(edit.getEndB() - edit.getBeginB());
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

            System.out.println("blame -- " + filePathStr);
            BlameResult blameCommand = b.call();
            blameCommand.computeAll();
            System.out.println(blameCommand.lastLength());

            int i = 0;
            try {
                while (blameCommand.getSourceAuthor(i) != null) {
                    fileBlameOutput.addLine(blameCommand.getSourceAuthor(i).getEmailAddress());
                    System.out.println(blameCommand.getSourceAuthor(i));
                    System.out.println(blameCommand.getSourceCommit(i));
                    System.out.println(blameCommand.getSourcePath(i));
                    System.out.println(blameCommand.getSourceCommitter(i));
                    System.out.println("---------");
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
}
