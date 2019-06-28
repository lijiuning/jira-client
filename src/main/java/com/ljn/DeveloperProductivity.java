package com.ljn;

import net.rcarz.jiraclient.Issue;

import java.util.ArrayList;
import java.util.List;

public class DeveloperProductivity {
    String developer;
    int taskCount;
    int erCount;
    int bugCount;
    List<Issue> issues;
    List<Issue> defects;
    int isdCount;
    double taskStoryPoints;
    double erStoryPoints;
    double bugStoryPoints;


    public DeveloperProductivity(String name, double ts, double bs, double es) {
        this.developer = name;
        this.taskStoryPoints = ts;
        this.bugStoryPoints = bs;
        this.erStoryPoints = es;
        issues = new ArrayList<>();
        defects = new ArrayList<>();
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }

    public List<Issue> getDefects() {
        return defects;
    }

    public void setDefects(List<Issue> defects) {
        this.defects = defects;
    }

    public int getIsdCount() {
        return isdCount;
    }

    public void setIsdCount(int isdCount) {
        this.isdCount = isdCount;
    }



    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public int getErCount() {
        return erCount;
    }

    public void setErCount(int erCount) {
        this.erCount = erCount;
    }

    public int getBugCount() {
        return bugCount;
    }

    public void setBugCount(int bugCount) {
        this.bugCount = bugCount;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public double getTaskStoryPoints() {
        return taskStoryPoints;
    }

    public double getTotalSP() {
        return bugStoryPoints + taskStoryPoints + erStoryPoints;
    }

    public double getTestedSP(){
        return bugStoryPoints + erStoryPoints;
    }

    public int getTotalCount() {
        return bugCount + taskCount + erCount;
    }

    public double getISDPerSP() {
        if (getTestedSP() <= 0 || getIsdCount()<=0) {
            return 0;
        } else {
            return (double)getIsdCount()/(double)getTestedSP();
        }
    }

    public void setTaskStoryPoints(double taskStoryPoints) {
        this.taskStoryPoints = taskStoryPoints;
    }

    public double getErStoryPoints() {
        return erStoryPoints;
    }

    public void setErStoryPoints(double erStoryPoints) {
        this.erStoryPoints = erStoryPoints;
    }

    public double getBugStoryPoints() {
        return bugStoryPoints;
    }

    public void setBugStoryPoints(double bugStoryPoints) {
        this.bugStoryPoints = bugStoryPoints;
    }



    @Override
    public String toString() {
        String formatStr = "[%s] Bug: %.1fSP (%d), ER: %.1fSP (%d), Task: %.1fSP (%d)  <total: %.1fSP (%d), ISD: %d, QF: %.2f>";
        return String.format(formatStr, developer, bugStoryPoints, bugCount, erStoryPoints, erCount, taskStoryPoints, taskCount, getTotalSP(), getTotalCount(), isdCount, getISDPerSP());
    }
}