package com.amber.git;

import com.amber.data.AuthorInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.HashMap;
import java.util.Map;

public class GitAuthorExtractor {
    private final Git git;

    public GitAuthorExtractor(Git git) {
        this.git = git;
    }

    public Map<String, AuthorInfo> getAllAuthorMapByEmail() {
        try {
            Map<String, AuthorInfo> result = new HashMap<>();
            Iterable<RevCommit> commits = null;
            commits = this.git.log().call();
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
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
}
