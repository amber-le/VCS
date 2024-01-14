package com.amber;

import com.amber.data.AuthorInfo;
import com.amber.git.GitAuthorStatExtractor;
import com.amber.git.GitFileLogExtractor;
import com.amber.git.GitAuthorExtractor;
import com.amber.output.AuthorStatsOutput;
import com.amber.output.FileLogOutput;
import com.amber.reporter.ReportOutputs;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException, GitAPIException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please input the repo full path:");
        String defaultRepoPath = "/Users/craby/Documents/Documents - Wolf/TIC3901 Industrial Practice/Project/Example/duke";
        String repoPath = scanner.nextLine();
        if (Objects.equals(repoPath, "")) {
            repoPath = defaultRepoPath;
        }

        File repoFolder = new File(repoPath);
        Repository repository = new RepositoryBuilder()
                .setGitDir(new File(repoPath + "/.git"))
                .build();

        Git git = new Git(repository);


        // extract outputs
        Map<String, FileLogOutput> fileOutput = new GitFileLogExtractor(repoPath, repository, git).getOutput();
        Map<String, AuthorInfo> allAuthorMapByEmail = new GitAuthorExtractor(git).getAllAuthorMapByEmail();
        Map<String, AuthorStatsOutput> authorStatsOutput = new GitAuthorStatExtractor(repository, git).getAuthorStatsOutput();


        // write report
        ReportOutputs csvReporter = new ReportOutputs(fileOutput, authorStatsOutput, allAuthorMapByEmail);
        String reportFilePath = "./" + repoFolder.getName() + "-report";
        csvReporter.writeReport(reportFilePath);
        System.out.println();
        System.out.println("The report has been written to " + reportFilePath);
    }

}
