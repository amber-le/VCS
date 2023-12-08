package com.amber;

import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException, GitAPIException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input the repo path:");
        String repoPath = scanner.nextLine();
        Repository repository = new RepositoryBuilder()
                .setGitDir(new File(repoPath + "/.git"))
                .build();

        Git git = new Git(repository);
        LogCommand log = git.log();
        Iterable<RevCommit> commits = log.call();
        String nextCommitId = "";
        for (RevCommit commit : commits) {
            if (!nextCommitId.isEmpty()) {
                runDiffCommand(repoPath, commit.getId().name(), nextCommitId);
            }

            nextCommitId = commit.getId().name();
        }
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
}