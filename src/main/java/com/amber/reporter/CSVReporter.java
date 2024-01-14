package com.amber.reporter;

import com.amber.data.AuthorInfo;
import com.amber.output.AuthorStatsOutput;
import com.amber.output.FileLogOutput;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class CSVReporter {
    Map<String, FileLogOutput> fileOutput;
    Map<String, AuthorStatsOutput> authorStatsOutput;
    Map<String, AuthorInfo> allAuthorMapByEmail;

    StringBuilder outCsv = new StringBuilder();

    public CSVReporter(Map<String, FileLogOutput> fileOutput, Map<String, AuthorStatsOutput> authorStatsOutput, Map<String, AuthorInfo> allAuthorMapByEmail) {
        this.fileOutput = fileOutput;
        this.authorStatsOutput = authorStatsOutput;
        this.allAuthorMapByEmail = allAuthorMapByEmail;
    }

    public void writeReport(String filePath) throws IOException {

        addAuthorDetail();
        outCsv.append("\n\n\n\n");
        addFileAuthorLinedContributedReport();
        outCsv.append("\n\n\n\n");
        addAuthorContributionDetail();
        outCsv.append("\n\n\n\n");
        addFileOwnerReport();

        // write final report to a file
        FileWriter fileWriter = new FileWriter(filePath);
        fileWriter.write(outCsv.toString());
        fileWriter.flush();
    }

    private void addAuthorDetail() {
        // author list report
        outCsv.append("List Of Authors, \n");
        outCsv.append("authorName, authorEmail\n");
        for (Map.Entry<String, AuthorInfo> authorEntry : allAuthorMapByEmail.entrySet()) {
            AuthorInfo authorInfo = authorEntry.getValue();

            outCsv.append(authorInfo.getName()).append(",")
                    .append(authorInfo.getEmail())
                    .append("\n");

        }
    }

    private void addAuthorContributionDetail() {
        // write author line added/removed report
        outCsv.append("Author Contribution Detail, \n");
        outCsv.append("author, line_added, line_removed, commit_count\n");
        for (Map.Entry<String, AuthorStatsOutput> authorStat : authorStatsOutput.entrySet()) {
            outCsv
                    .append(authorStat.getKey()).append(",")
                    .append(authorStat.getValue().getLineAdded()).append(",")
                    .append(authorStat.getValue().getLineRemoved()).append(",")
                    .append(authorStat.getValue().getCommitCount()).append("\n");

        }
    }

    private void addFileAuthorLinedContributedReport() {
        // Write author report
        outCsv.append("File Author Details, \n");
        outCsv.append("file_path, author, line_contributed\n");
        for (Map.Entry<String, FileLogOutput> output : fileOutput.entrySet()) {
            String key = output.getKey();
            FileLogOutput value = output.getValue();

            for (Map.Entry<String, Integer> authorLineCount : value.getAuthorLineCount().entrySet()) {
                outCsv
                        .append(key).append(",")
                        .append(authorLineCount.getKey()).append(",")
                        .append(authorLineCount.getValue()).append("\n");
            }

        }
    }

    private void addFileOwnerReport() {
        // file owner report
        outCsv.append("File Owner Details, \n");
        outCsv.append("file_path, owner, %\n");
        for (Map.Entry<String, FileLogOutput> output : fileOutput.entrySet()) {
            String key = output.getKey();
            FileLogOutput value = output.getValue();

            int max = 0;
            int total = 0;
            String ownerEmail = "";
            for (Map.Entry<String, Integer> authorLineCount : value.getAuthorLineCount().entrySet()) {
                total += authorLineCount.getValue();
                if (authorLineCount.getValue() > max) {
                    max = authorLineCount.getValue();
                    ownerEmail = authorLineCount.getKey();
                }
            }
            if (total == 0) {
                continue;
            }

            Double percentage = total >= 0 ? (Double.valueOf(max * 100) / Double.valueOf(total)) : 0;
            outCsv
                    .append(key).append(",")
                    .append(ownerEmail).append(",")
                    .append(percentage.isNaN() ? "NaN" : String.format("%.2f", percentage))
                    .append("\n")
            ;

        }
    }
}
