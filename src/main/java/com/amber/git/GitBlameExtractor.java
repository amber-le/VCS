package com.amber.git;

import com.amber.algo.Levenshtein;
import com.amber.output.FileLogOutput;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

import javax.sound.sampled.Line;
import java.io.IOException;
import java.util.*;

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


                LogCommand logCommand = git.log();
                logCommand.addPath(filePathStr);
                Iterable<RevCommit> log = logCommand.call();

                FileLogOutput fileBlameOutput = new FileLogOutput(filePathStr);
                blameOutput.put(filePathStr, fileBlameOutput);


//                System.out.println("blame -- " + filePathStr);
                List<LineInfo> lines = new ArrayList<>();
                Set<Integer> foundOwner = new HashSet<>();
                for (RevCommit rev : log) {
//                    System.out.println("blame  at commit: " + rev.getId());
//                    System.out.println(rev.getAuthorIdent().getName());

                    BlameCommand blameCommand = new BlameCommand(repository);
                    blameCommand.setFilePath(filePathStr);
                    blameCommand.setStartCommit(rev);

                    BlameResult blameResult = blameCommand.call();
                    if (blameResult == null) {
                        continue;
                    }
                    blameResult.computeAll();
//                    System.out.println(blameResult.lastLength());

                    int i = 0;
                    List<LineInfo> tempLines = new ArrayList<>();
                    try {
                        while (blameResult.getSourceAuthor(i) != null) {
                            fileBlameOutput.addLine(blameResult.getSourceAuthor(i).getEmailAddress());
//                            StringBuilder builder = new StringBuilder();
//                            builder.append(i).append(" - ").append(blameResult.getSourceAuthor(i));
//                            System.out.println(builder);
                            tempLines.add(new LineInfo(blameResult.getSourceAuthor(i).getEmailAddress(), i, blameResult.getResultContents().getString(i)));
                            i++;
                        }
                    } catch (IndexOutOfBoundsException e) {
                    }

                    if (lines.isEmpty()) {
                        lines = new ArrayList<>(tempLines);
                    } else {
                        findOwner(tempLines, lines, foundOwner);
                    }
                    if (foundOwner.size() == lines.size()) {
                        break;
                    }
                }
                for (LineInfo line : lines) {
                    if (line.authorEmail != null && !line.authorEmail.isEmpty()) {
                        System.out.println(line.authorEmail + " - " + line.getLineNumber() + " - " + line.lineContent);
                        fileBlameOutput.addLine(line.authorEmail);
                    }
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

    private void findOwner(List<LineInfo> currentLines, List<LineInfo> oldLines, Set<Integer> foundOwner) {
        for (LineInfo currentLine : currentLines) {
            if (foundOwner.contains(currentLine.lineNumber)) {
                continue;
            }

            MostSimilarLine mostSimilarLine = findMostSimilar(currentLine, oldLines);
            if (mostSimilarLine.distance > 0.5) {
                foundOwner.add(mostSimilarLine.mostSimilar.lineNumber);
            } else {
                currentLine.setLineContent(mostSimilarLine.mostSimilar.lineContent);
                currentLine.setAuthorEmail(mostSimilarLine.mostSimilar.authorEmail);
            }
        }
    }


    private MostSimilarLine findMostSimilar(LineInfo line, List<LineInfo> lines) {
        Levenshtein levenshtein = new Levenshtein();
        double minDistance = Double.MAX_VALUE;
        LineInfo mostSimilar = null;
        for (LineInfo otherLine : lines) {
            double distance = levenshtein.distance(line.lineContent, otherLine.lineContent);
            if (distance < minDistance) {
                minDistance = distance;
                mostSimilar = otherLine;
            } else if (distance == minDistance) {
                if (line.authorEmail.equals(otherLine.authorEmail)) {
                    mostSimilar = otherLine;
                }
            }
        }
        return new MostSimilarLine(line, mostSimilar, minDistance);
    }

    private static class MostSimilarLine {
        public MostSimilarLine(LineInfo line, LineInfo mostSimilar, double distance) {
            this.line = line;
            this.mostSimilar = mostSimilar;
            this.distance = distance;
        }

        LineInfo line;
        LineInfo mostSimilar;
        double distance;
    }

    private static class LineInfo {
        String authorEmail;
        int lineNumber;
        String lineContent;

        public LineInfo(String authorEmail, int lineNumber, String lineContent) {
            this.authorEmail = authorEmail;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
        }

        public String getAuthorEmail() {
            return authorEmail;
        }

        public void setAuthorEmail(String authorEmail) {
            this.authorEmail = authorEmail;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getLineContent() {
            return lineContent;
        }

        public void setLineContent(String lineContent) {
            this.lineContent = lineContent;
        }
    }
}
