package com.amber.data;

import java.util.ArrayList;
import java.util.List;

public class CommitInfo {
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

    public void setLinesAdded(List<String> linesAdded) {
        this.linesAdded = linesAdded;
    }

    public void setLinesRemoved(List<String> linesRemoved) {
        this.linesRemoved = linesRemoved;
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
