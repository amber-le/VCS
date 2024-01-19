package com.amber.git;

import com.amber.output.FileLogOutput;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// dùng để lấy thông tin người viết code của mỗi dòng code
public class GitBlameExtractor {
    Repository repository;
    Git git;

    public GitBlameExtractor(Repository repository, Git git) {
        this.repository = repository;
        this.git = git;
    }

    public Map<String, FileLogOutput> getOutput() {
        System.out.println("--- GIT BLAME ---");
        try {
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.setRecursive(true);
            treeWalk.addTree(git.log().call().iterator().next().getTree());
            Map<String, FileLogOutput> blameOutput = new HashMap<>();

            while (treeWalk.next()) {
                if (treeWalk.isSubtree()) {
                    continue;
                }
                String filePathStr = treeWalk.getPathString();

                BlameCommand b = new BlameCommand(repository);
                b.setFilePath(filePathStr);

                FileLogOutput fileBlameOutput = new FileLogOutput(filePathStr);
                blameOutput.put(filePathStr, fileBlameOutput);

//            System.out.println("blame -- " + filePathStr);
                BlameResult blameCommand = b.call();
                blameCommand.computeAll();
//            System.out.println(blameCommand.lastLength());

                int i = 0;
                try {
                    while (blameCommand.getSourceAuthor(i) != null) {
                        fileBlameOutput.addLine(blameCommand.getSourceAuthor(i).getEmailAddress());
//                    System.out.println(blameCommand.getSourceAuthor(i));
//                    System.out.println(blameCommand.getSourceCommit(i));
//                    System.out.println(blameCommand.getSourcePath(i));
//                    System.out.println(blameCommand.getSourceCommitter(i));
//                    System.out.println("---------");
                        i++;
                    }
                } catch (IndexOutOfBoundsException e) {
                }
            }
            System.out.println("--- end GIT BLAME ---");
            return blameOutput;

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
}
