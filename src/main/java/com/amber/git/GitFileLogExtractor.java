package com.amber.git;

import com.amber.algo.Levenshtein;
import com.amber.data.CommitInfo;
import com.amber.output.FileLogOutput;
import com.amber.utils.Command;
import java.io.*;
import org.apache.tika.*;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitFileLogExtractor {
    String repoPath;
    Repository repository;
    Git git;

    public GitFileLogExtractor(String repoPath, Repository repository, Git git) {
        this.repoPath = repoPath;
        this.repository = repository;
        this.git = git;
    }

    public Map<String, FileLogOutput> getOutput() throws IOException, GitAPIException {
        System.out.println("--- GIT Log By Line ---");
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.setRecursive(true);
        treeWalk.addTree(git.log().call().iterator().next().getTree());

        Levenshtein levenshtein = new Levenshtein();
        Map<String, FileLogOutput> blameOutput = new HashMap<>();

        while (treeWalk.next()) {
            if (treeWalk.isSubtree()) {
                continue;
            }
            String filePathStr = treeWalk.getPathString();
            int noLines = countLines(repoPath + "/" + filePathStr);

            FileLogOutput fileBlameOutput = new FileLogOutput(filePathStr);
            blameOutput.put(filePathStr, fileBlameOutput);

            int curLine = 1;
            while (curLine <= noLines) {
                List<CommitInfo> commitInfos = runLogCommand(repoPath, filePathStr, curLine);

                if (!isTextFile(new File(repoPath + "/" + filePathStr))) {
                    fileBlameOutput.addLine(commitInfos.get(0).getAuthor().getEmail());
                    curLine = noLines + 1;
                    continue;
                }

                for (CommitInfo commitInfo : commitInfos) {
                    String addedStr = String.join("\n", commitInfo.getLinesAdded());
                    String removedStr = String.join("\n", commitInfo.getLinesRemoved());
                    double distance = levenshtein.distance(addedStr, removedStr);
                    // if new changes is more than 70% -> consider the author as owner of the line
                    double min = 0.7 * removedStr.length();
                    if (distance >= min || commitInfo.getLinesRemoved().isEmpty()) {
                        fileBlameOutput.addLine(commitInfo.getAuthor().getEmail());
                        break;
                    }
                }
                curLine++;
            }
        }
        return blameOutput;
    }

    private static boolean isTextFile(File file) {
        try {
            Tika tika = new Tika();
            String fileType = tika.detect(file);

            return fileType.startsWith("text") || fileType.startsWith("application/javascript");

        } catch (IOException e) {
            System.out.printf(file.getAbsolutePath());
            e.printStackTrace();
            return false;  // An error occurred, treat as non-text file
        }
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

    private List<CommitInfo> runLogCommand(String repoPath, String filePath, int lineNo) throws IOException {
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
        List<String> diffOutput = Command.captureProcessOutput(process.getInputStream());
        List<CommitInfo> commitInfos = LogParser.parseGitLog(String.join("\n", diffOutput));
        return commitInfos;
    }
}
