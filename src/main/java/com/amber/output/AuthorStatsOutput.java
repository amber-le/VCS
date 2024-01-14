package com.amber.output;

import com.amber.data.AuthorInfo;

public class AuthorStatsOutput {
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
