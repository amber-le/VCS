package com.amber.reporter;

import com.amber.data.AuthorInfo;
import com.amber.output.AuthorStatsOutput;
import com.amber.output.FileLogOutput;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jgit.lib.*;

public class ReportOutputs {
    // fileOutput stores the file path, and the author line counts
    // the key is the file path
    Map<String, FileLogOutput> fileOutput;

    // authorStatsOutput stores the author email and the author stats
    // (line added, line removed, commit count)
    // the key is the author email
    Map<String, AuthorStatsOutput> authorStatsOutput;

    // allAuthorMapByEmail stores the author email and the author info
    // the key is the author email
    Map<String, AuthorInfo> allAuthorMapByEmail;

    StringBuilder outCsv = new StringBuilder();
    private final StringBuilder outHtml = new StringBuilder();

    public ReportOutputs(Map<String, FileLogOutput> fileOutput, Map<String, AuthorStatsOutput> authorStatsOutput,
                         Map<String, AuthorInfo> allAuthorMapByEmail) {
        this.fileOutput = fileOutput;
        this.authorStatsOutput = authorStatsOutput;
        this.allAuthorMapByEmail = allAuthorMapByEmail;
    }

    public void writeReport(String filePath) throws IOException {
        outHtml.append(
                "<style> body {padding-left: 30px; padding-right: 30px; display: flex; margin-left: auto; margin-right: auto; flex-direction: column; font-family: sans-serif; width: 70%;} table, th, td {  border: 1px solid #ddd; border-collapse: collapse; } " +
                        "th, td { padding: 15px; } h1{text-align:center; color: #19447B; text-transform: uppercase; font-size: 26px; } h2{font-size: 20px; color: #F47421} th{background-color: #F1F2F2;} table{font-size:14px}</style> ");

        //get the project name
        String[] path = filePath.split("/");
        String projectName = path[path.length - 1];
        projectName = projectName.substring(0, projectName.length() - 7);
        outHtml.append("<br/>");
        outCsv.append("Project Report - ").append(projectName).append("\n");
        outHtml.append("<h1> Project Report - ").append(projectName).append("</h1>");
        outCsv.append("\n\n\n\n");
        outHtml.append("<br/>");
        addProjectOverviewReport();
        outCsv.append("\n\n\n\n");
        outHtml.append("<br/><br/>");
        addFileDetailReport();
        outCsv.append("\n\n\n\n");
        outHtml.append("<br/><br/>");
        addAuthorContributionDetailReport();
        outCsv.append("\n\n\n\n");
        outHtml.append("<br/><br/>");
        addFileOwnerReport();

        // write a final report to a file
        FileWriter fileWriter = new FileWriter(filePath + ".csv");
        fileWriter.write(outCsv.toString());
        fileWriter.flush();

        // write a final report to a file
        FileWriter fileWriter1 = new FileWriter(filePath + ".html");
        fileWriter1.write(outHtml.toString());
        fileWriter1.flush();
    }

//    private void addAuthorDetail() {
//        // author list report
//        outCsv.append("List Of Authors, \n");
//        outCsv.append("authorName, authorEmail\n");
//        for (Map.Entry<String, AuthorInfo> authorEntry : allAuthorMapByEmail.entrySet()) {
//            AuthorInfo authorInfo = authorEntry.getValue();
//
//            outCsv.append(authorInfo.getName()).append(",")
//                    .append(authorInfo.getEmail())
//                    .append("\n");
//
//        }
//    }

    private void addProjectOverviewReport() {
        // project overview report
        outCsv.append("1. Project Overview, \n");
        outCsv.append(
                "total_file, total_contributors, project_author, total_commit, total_line_added, total_line_removed, project_author\n");
        outHtml.append("<h2>1. Project Overview</h2>");
        outHtml.append("<table border=\"1\">");
        outHtml.append("<tr><th>Total Files</th><th>Total Contributors</th>" +
                               "<th>Total Commits</th><th>Total Lines Added</th><th>Total Lines Removed</th><th>Project Author</th></tr>");
        int totalFile = fileOutput.size();
        int totalContributors = authorStatsOutput.size();
        int totalCommit = 0;
        int totalLineAdded = 0;
        int totalLineRemoved = 0;
        String projectAuthor = "";

        Map<String, Integer> authorProject = new java.util.HashMap<>();

        for (Map.Entry<String, FileLogOutput> output : fileOutput.entrySet()) {
            int lineAdded = 0;
            int lineRemoved = 0;
            int commitCount = 0;
            for (Map.Entry<String, Integer> authorLineCount : output.getValue().getAuthorLineCount().entrySet()) {
                AuthorStatsOutput authorStats = authorStatsOutput.get(authorLineCount.getKey());
                lineAdded += authorStats.getLineAdded();
                lineRemoved += authorStats.getLineRemoved();
                commitCount += authorStats.getCommitCount();
                if (authorProject.containsKey(authorLineCount.getKey())) {
                    authorProject.put(authorLineCount.getKey(), authorProject.get(authorLineCount.getKey()) + 1);
                } else {
                    authorProject.put(authorLineCount.getKey(), 1);
                }
            }
            totalCommit += commitCount;
            totalLineAdded += lineAdded;
            totalLineRemoved += lineRemoved;
        }
        // if the author has the most line in the project, then he is the project author
        int max = 0;
        for (Map.Entry<String, Integer> authorProjectCount : authorProject.entrySet()) {
            if (authorProjectCount.getValue() > max) {
                max = authorProjectCount.getValue();
                projectAuthor = authorProjectCount.getKey();
            }
        }

        // csv report
        outCsv.append(totalFile).append(",").append(totalContributors).append(",").append(totalCommit).append(",")
              .append(totalLineAdded).append(",").append(totalLineRemoved).append(",")
              .append(allAuthorMapByEmail.get(projectAuthor).getName()).append("\n");
        outHtml.append("<tr style = text-align:center><td>").append(totalFile).append("</td><td>")
               .append(totalContributors).append("</td><td>").append(totalCommit).append("</td><td>")
               .append(totalLineAdded).append("</td><td>").append(totalLineRemoved).append("</td><td>")
               .append(allAuthorMapByEmail.get(projectAuthor).getName()).append("</td></tr>");
        outHtml.append("</table>");

    }

    private void addAuthorContributionDetailReport() {
        // write an author report
        Map<String, Object> ownerFileCount = addFileOwnerReport().get("fileOwnerCount");
        outCsv.append("3. Author Contribution Detail, \n");
        outCsv.append(" ,author_name, author_email, line_added, line_removed, file_own, commit_count\n");
        outHtml.append("<h2>3. Author Contribution Detail</h2>");
        outHtml.append("<table border=\"1\">");
        outHtml.append("<tr><th> </th><th>Author Name</th><th>Author Email</th><th>Lines Added</th>" +
                               "<th>Lines Removed</th><th>Files Owned</th><th>Commit Count</th></tr>");

        int stt = 0;
        for (Map.Entry<String, AuthorStatsOutput> authorStat : authorStatsOutput.entrySet()) {
            AuthorInfo authorInfo = allAuthorMapByEmail.get(authorStat.getKey());
            outCsv.append(++stt).append(",").append(authorInfo.getName()).append(",").append(authorInfo.getEmail())
                  .append(",").append(authorStat.getValue().getLineAdded()).append(",")
                  .append(authorStat.getValue().getLineRemoved()).append(",")
                  .append(ownerFileCount.get(authorInfo.getEmail())).append(",")
                  .append(authorStat.getValue().getCommitCount()).append("\n");
            outHtml.append("<tr><td style = text-align:center>").append(stt).append("</td><td>")
                   .append(authorInfo.getName()).append("</td><td style=word-break:break-word>").append(authorInfo.getEmail())
                   .append("</td><td style = text-align:right>").append(authorStat.getValue().getLineAdded())
                   .append("</td><td style = text-align:right>").append(authorStat.getValue().getLineRemoved())
                   .append("</td><td style = text-align:right>").append(ownerFileCount.get(authorInfo.getEmail()))
                   .append("</td><td style = text-align:right>").append(authorStat.getValue().getCommitCount())
                   .append("</td></tr>");

        }
        outHtml.append("</table>");
    }

    private void addFileDetailReport() {
        // Write a File report
        Map<String, Object> percentContributed = addFileOwnerReport().get("percentContributed");
        Map<String, Object> lineContributed = addFileOwnerReport().get("lineContributed");
        Map<String, Object> countFileLine = addFileOwnerReport().get("countFileLine");
        Map<String, Object> fileOwner = addFileOwnerReport().get("fileOwner");
        outCsv.append("2. File Details, \n");
        outCsv.append(" , file_path, total_file_lines author_name, author_email, %_contributed, line_contributed\n");
        outHtml.append("<h2>2. File Details</h2>");
        outHtml.append("<table border=\"1\">");
        outHtml.append("<tr><th> </th><th>File Path</th><th>Total File Lines</th><th>Author Name</th>" +
                               "<th>Author Email</th><th>% Contributed</th><th>Line Contributed</th></tr>");
        int stt = 0;
        for (Map.Entry<String, FileLogOutput> output : fileOutput.entrySet()) {
            String key = output.getKey();
            FileLogOutput value = output.getValue();
            String ownerEmail = String.valueOf(fileOwner.get(key));

            AuthorInfo authorInfo = allAuthorMapByEmail.get(ownerEmail);
                outCsv.append(++stt).append(",").append(key).append(",").append(countFileLine.get(key)).append(",")
                      .append(authorInfo.getName()).append(",").append(ownerEmail).append(",")
                      .append(percentContributed.get(key)).append(",")
                      .append(lineContributed.get(key)).append("\n");
                outHtml.append("<tr><td style = text-align:center>").append(stt).append("</td><td style=word-break:break-word>").append(key)
                       .append("</td><td style = text-align:right>").append(countFileLine.get(key)).append("</td><td style=word-break:break-word>")
                       .append(authorInfo.getName()).append("</td><td>").append(countFileLine.get(key))
                       .append("</td><td style = text-align:right>")
                       .append(percentContributed.get(key))
                       .append("</td><td style = text-align:right>").append(lineContributed.get(key))
                       .append("</td></tr>");

        }
        outHtml.append("</table>");
    }

    private Map<String, Map<String, Object>> addFileOwnerReport() {
        // file owner report
        Map<String, Object> percentContributed = new java.util.HashMap<>();
        Map<String, Object> lineContributed = new java.util.HashMap<>();
        Map<String, Object> fileOwner = new java.util.HashMap<>();
        Map<String, Object> fileOwnerCount = new java.util.HashMap<>();
        Map<String, Object> countFileLine = new java.util.HashMap<>();
        for (Map.Entry<String, FileLogOutput> output : fileOutput.entrySet()) {
            String key = output.getKey();
            FileLogOutput value = output.getValue();

            int max = 0;
            int total = 0;
            int fileCount = 0;
            String ownerEmail = "";
            for (Map.Entry<String, Integer> authorLineCount : value.getAuthorLineCount().entrySet()) {
                total += authorLineCount.getValue();
                if (authorLineCount.getValue() >= max) {
                    max = authorLineCount.getValue();
                    ownerEmail = authorLineCount.getKey();
                }
            }

            countFileLine.put(key, total);
            if (fileOwnerCount.containsKey(ownerEmail)) {
                fileOwnerCount.put(ownerEmail, (Integer)fileOwnerCount.get(ownerEmail) + 1);
            } else {
                fileOwnerCount.put(ownerEmail, 1);
            }
            fileOwner.put(key, ownerEmail);

            double percentage = total >= 0 ? (Double.valueOf(max * 100) / Double.valueOf(total)) : 0;
            percentContributed.put(key, (int) percentage);
            lineContributed.put(key, max);
            fileOwner.put(key, ownerEmail);

//            outCsv.append(key).append(",").append(ownerEmail).append(",")
//                  .append(percentage.isNaN() ? "NaN" : String.format("%.2f", percentage)).append("\n");
        }
        Map<String, Map<String, Object>> numberInfo = new java.util.HashMap<>();
        numberInfo.put("percentContributed", percentContributed);
        numberInfo.put("lineContributed", lineContributed);
        numberInfo.put("fileOwnerCount", fileOwnerCount);
        numberInfo.put("countFileLine", countFileLine);
        numberInfo.put("fileOwner", fileOwner);
        return numberInfo;
    }

}
