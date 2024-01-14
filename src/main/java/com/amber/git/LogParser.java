package com.amber.git;

import com.amber.data.AuthorInfo;
import com.amber.data.CommitInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {

    public static List<CommitInfo> parseGitLog(String gitLog) {
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

}
