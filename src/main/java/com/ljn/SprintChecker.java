package com.ljn;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.IssueType;
import net.rcarz.jiraclient.Resolution;

import java.util.List;

public class SprintChecker {

    public static void checkFields(List<Issue> issues) {
        if (issues.isEmpty())
            return;

        System.out.println("=======================================");
        System.out.println("Checking Story Points ....");
        for (Issue issue : issues) {
            checkStoryPointIsEmpty(issue);
        }

        System.out.println("Checking Developer ....");
        for (Issue issue : issues) {
            checkDeveloperIsEmpty(issue);
        }

        System.out.println("Checking Code Reviewer ....");
        for (Issue issue : issues) {
            checkCodeReviewerIsEmpty(issue);
        }
        System.out.println("Checking Done.");
        System.out.println("=======================================");
    }

    private static void checkCodeReviewerIsEmpty(Issue issue) {
        IssueType type = issue.getIssueType();

        if (type != null) {
            switch (type.getName()) {
                case "Bug":
                    Resolution resolution = issue.getResolution();
                    if (resolution != null) {
                        if (resolution.getName().equals("Fixed") || resolution.getName().equals("Unresolved")) {
                            CheckCodeReviewerIsEmpty(issue);
                        }
                    }
                    break;
                case "Enhancement":
                case "Requirement/User Story":
                    CheckCodeReviewerIsEmpty(issue);
                    break;
                case "Task":
                    if (isDevTask(issue)) {
                        CheckCodeReviewerIsEmpty(issue);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static boolean isDevTask(Issue issue) {
        String category = issue.getTaskCategory();

        if (category != null) {
            if (category.equals("Development"))
                return true;
        }
        return false;
    }

    private static void checkStoryPointIsEmpty(Issue issue) {
        Double sp = issue.getStoryPoints();
        if (sp == null) {
            printLog(issue, "SP is empty! (Issue type: " + issue.getIssueType().getName() + ")");
        }
    }

    private static void checkDeveloperIsEmpty(Issue issue) {
        // skip the non-development tasks
        IssueType type = issue.getIssueType();
        if (type != null && type.getName().equals("Task")) {
            if (!isDevTask(issue)) {
                return;
            }
        }
        net.rcarz.jiraclient.User dev = issue.getDeveloper();
        if (dev == null) {
            printLog(issue, "Developer is empty!");
        }
    }

    private static void CheckCodeReviewerIsEmpty(Issue issue) {
        net.rcarz.jiraclient.User codereviewer = issue.getCodeReviewer();
        if (codereviewer == null) {
            printLog(issue, "Code reviewer is empty!");
        }
    }

    public static void printLog(Issue issue, String log) {
        String format = String.format("%s/browse/%s   [%s] %s  %s", JiraHelper.JIRA_URL, issue.getKey(), issue.getIssueType(), issue.getSummary(), log);
        System.out.println(format);
    }
}
