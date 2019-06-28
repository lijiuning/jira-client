package com.ljn;

import com.ljn.utils.Emoji;
import net.rcarz.jiraclient.Issue;
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

        System.out.println("Checking testers ....");
        for (Issue issue : issues) {
            checkTesterIsEmpty(issue);
        }

        System.out.println("=======================: CHECKING SPRINT DONE :================================");
        System.out.println();
    }

    private static void checkCodeReviewerIsEmpty(Issue issue) {
        if(issue.isCodeReviewerRequired() && issue.getCodeReviewer() == null){
            printLog(issue, "Code reviewer is empty!" + (issue.getDeveloper() == null? "[EmptyDeveloper]" : "[" + issue.getDeveloper().getDisplayName() + "]"));
        }
    }

    private static void checkTesterIsEmpty(Issue issue){
        if(issue.isTesterRequired() && issue.getTester() == null){
            printLog(issue, "Tester is empty!");
        }
    }

    private static void checkStoryPointIsEmpty(Issue issue) {
        Double sp = issue.getStoryPoints();
        if (sp == null) {
            printLog(issue, "Story point is empty! (Issue type: " + issue.getIssueType().getName() + ")");
        }
    }

    private static void checkDeveloperIsEmpty(Issue issue) {
        // skip the non-development tasks
        if(!issue.isDevTask())
            return;

        //skip the dev misc
        if(issue.isDevTask() && issue.getSummary().toLowerCase().contains("misc")){
            return;
        }

        User dev = issue.getDeveloper();
        if (dev == null) {
            printLog(issue, "Developer is empty!");
        }
    }


    public static void printLog(Issue issue, String log){
        printLog(issue, log, 0);
    }

    public static void printLog(Issue issue, String log, int level) {
        String level_str = "";
        while(level-->0){
            level_str+='\t';
        }
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
        String format = String.format(level_str + "%s\t%s/browse/%s %s %s %s %s %s", Emoji.Type(issue.getIssueType().getName()), JiraHelper.JIRA_URL, issue.getKey(), Emoji.Status(issue.getStatus().getName()),   description, issue.getSummary(),overdue_str, log);
        System.out.println(format);
    }
}
