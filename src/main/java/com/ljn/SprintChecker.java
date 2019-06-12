package com.ljn;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.IssueType;
import net.rcarz.jiraclient.Resolution;
import net.rcarz.jiraclient.User;

import java.text.DateFormat;
import java.util.List;

public class SprintChecker {

    public static void checkFields(List<Issue> issues) {
        if (issues.isEmpty())
            return;

        System.out.println("========================: CHECKING SPRINT :====================================");
        System.out.println("Checking story points ....");
        for (Issue issue : issues) {
            checkStoryPointIsEmpty(issue);
        }

        System.out.println("Checking developers ....");
        for (Issue issue : issues) {
            checkDeveloperIsEmpty(issue);
        }

        System.out.println("Checking code reviewers ....");
        for (Issue issue : issues) {
            checkCodeReviewerIsEmpty(issue);
        }
        System.out.println("=======================: CHECKING SPRINT DONE :================================");
        System.out.println();
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
            printLog(issue, "Story point is empty! (Issue type: " + issue.getIssueType().getName() + ")");
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
        User dev = issue.getDeveloper();
        if (dev == null) {
            printLog(issue, "Developer is empty!");
        }
    }

    private static void CheckCodeReviewerIsEmpty(Issue issue) {
        User codereviewer = issue.getCodeReviewer();
        if (codereviewer == null) {
            printLog(issue, "Code reviewer is empty!");
        }
    }

    public static void printLog(Issue issue, String log) {
        String description = "";
        if(!issue.getIssueType().getName().contains("Sub"))
            description += String.format("(%.1f)", issue.getStoryPoints());

        String overdue_str = "";
        if(!issue.isDone() && issue.getDueDate() == null)
        {
            overdue_str +="\uD83D\uDD14 \t";
        }else if(issue.isOverdue()){
            overdue_str +="\uD83D\uDD14 <" + DateFormat.getDateInstance(DateFormat.DEFAULT).format(issue.getDueDate()) + "> ";
        }
        String format = String.format("%s%s/browse/%s %s %s %s %s %s", Emoji.Type(issue.getIssueType().getName()), JiraHelper.JIRA_URL, issue.getKey(), Emoji.Status(issue.getStatus().getName()),   description, issue.getSummary(),overdue_str, log);
        System.out.println(format);
    }
}
