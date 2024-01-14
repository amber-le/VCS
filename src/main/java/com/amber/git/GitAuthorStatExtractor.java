package com.amber.git;

import com.amber.data.AuthorInfo;
import com.amber.output.AuthorStatsOutput;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitAuthorStatExtractor {
    Repository repository;
    Git git;

    public GitAuthorStatExtractor(Repository repository, Git git) {
        this.repository = repository;
        this.git = git;
    }

    public Map<String, AuthorStatsOutput> getAuthorStatsOutput() {
        Map<String, AuthorStatsOutput> result = new HashMap<>();
        try {
            Iterable<RevCommit> commits = null;
            commits = git.log().call();
            for (RevCommit commit : commits) {
                PersonIdent authorIdent = commit.getAuthorIdent();
                AuthorStatsOutput authorStatsOutput = result.get(authorIdent.getEmailAddress());
                if (authorStatsOutput == null) {
                    authorStatsOutput = new AuthorStatsOutput(
                            new AuthorInfo(
                                    authorIdent.getEmailAddress(),
                                    authorIdent.getName()
                            )
                    );
                    result.put(authorIdent.getEmailAddress(), authorStatsOutput);
                }


                AbstractTreeIterator canonicalTreeParser = getCanonicalTreeParser(git, commit.getId());
                AbstractTreeIterator parentTreeParser = null;
                if (commit.getParentCount() > 0) {
                    parentTreeParser = getCanonicalTreeParser(git, commit.getParent(0).getId());
                } else {
                    parentTreeParser = new CanonicalTreeParser();
                    parentTreeParser.reset();
                }

                OutputStream outputStream = DisabledOutputStream.INSTANCE;
                try (DiffFormatter formatter = new DiffFormatter(outputStream)) {
                    formatter.setRepository(repository);
                    List<DiffEntry> diffEntries = formatter.scan(parentTreeParser, canonicalTreeParser);
                    for (DiffEntry diffEntry : diffEntries) {
//                    System.out.println(commit.getId());
//                    System.out.println(diffEntry.getChangeType());
//                    System.out.println(diffEntry.getNewPath());
//                    System.out.println(diffEntry.getDiffAttribute());
                        FileHeader fileHeader = formatter.toFileHeader(diffEntry);

                        EditList editList = fileHeader.toEditList();
                        int added = 0;
                        int removed = 0;
                        for (Edit edit : editList) {
//                        System.out.println(edit);
//                        System.out.println("deleted:");
//                        System.out.println(edit.getEndA() - edit.getBeginA());
                            removed += edit.getEndA() - edit.getBeginA();

//                        System.out.println("added:");
//                        System.out.println(edit.getEndB() - edit.getBeginB());
                            added += edit.getEndB() - edit.getBeginB();
                        }
                        authorStatsOutput.setLineAdded(authorStatsOutput.getLineAdded() + added);
                        authorStatsOutput.setLineRemoved(authorStatsOutput.getLineRemoved() + removed);
                    }
                }


                authorStatsOutput.setCommitCount(authorStatsOutput.getCommitCount() + 1);
            }
            return result;
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        } catch (CorruptObjectException e) {
            throw new RuntimeException(e);
        } catch (MissingObjectException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static AbstractTreeIterator getCanonicalTreeParser(Git git, ObjectId commitId) throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit commit = walk.parseCommit(commitId);
            ObjectId treeId = commit.getTree().getId();
            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                return new CanonicalTreeParser(null, reader, treeId);
            }
        }
    }

}
