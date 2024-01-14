package com.amber.output;

import java.util.HashMap;
import java.util.Map;

public class FileLogOutput {
    String filePath;
    Map<String, Integer> authorLineCount = new HashMap<>();

    public FileLogOutput(String filePath) {
        this.filePath = filePath;
    }

    public void addLine(String authorName) {
        if (!authorLineCount.containsKey(authorName)) {
            authorLineCount.put(authorName, 0);
        }

        authorLineCount.put(authorName, authorLineCount.get(authorName) + 1);
    }

    public Map<String, Integer> getAuthorLineCount() {
        return authorLineCount;
    }

    public void setAuthorLineCount(Map<String, Integer> authorLineCount) {
        this.authorLineCount = authorLineCount;
    }

    @Override
    public String toString() {
        return "BlameFileOutput{" +
                "filePath='" + filePath + '\'' +
                ", authorLineCount=" + authorLineCount +
                '}';
    }
}
