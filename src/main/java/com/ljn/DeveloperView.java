package com.ljn;

import net.rcarz.jiraclient.Issue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DeveloperView {
    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    private String developer = "";

    public List<Issue> getIssues() {
        return issues;
    }

    public List<Issue> getDefects() {
        return defects;
    }

    private List<Issue> issues = new ArrayList<>();
    private List<Issue> defects = new ArrayList<>();

    public static final String BUG = "Bug";
    public static final String ENHANCEMENT = "Enhancement";
    public static final String FEATURE = "Requirement/User Story";
    public static final String TASK = "Task";

    public DeveloperView(String developer){
        this.developer = developer;
    }

    public long bugCount(){
        return issues.stream().filter(p->p.getIssueType().getName().equals(BUG)).count();
    }

    public long featureCount(){
        return issues.stream().filter(p->p.getIssueType().getName().equals(FEATURE)).count()
                + issues.stream().filter(p->p.getIssueType().getName().equals(ENHANCEMENT)).count();
    }

    public long taskCount(){
        return issues.stream().filter(p->p.getIssueType().getName().equals(TASK)).count();
    }

    public double bugStoryPoints(){
        List<Double> sp = issues.stream()
                .filter(p->p.getIssueType().getName().equals(BUG))
                .map(e -> e.getStoryPoints())
                .collect(Collectors.toList());
        return sp.stream().mapToDouble(Double::doubleValue).sum();
    }

    public double taskStoryPoints(){
        List<Double> sp = issues.stream()
                .filter(p->p.getIssueType().getName().equals(TASK))
                .map(e -> e.getStoryPoints())
                .collect(Collectors.toList());
        return sp.stream().mapToDouble(Double::doubleValue).sum();
    }

    public double featureStoryPoints(){
        List<Double> sp = issues.stream()
                .filter(p->p.getIssueType().getName().equals(FEATURE))
                .map(e -> e.getStoryPoints())
                .collect(Collectors.toList());
        return sp.stream().mapToDouble(Double::doubleValue).sum();
    }

    public double storyPoints(){
        List<Double> sp = issues.stream().map(e -> e.getStoryPoints()).collect(Collectors.toList());
        return sp.stream().mapToDouble(Double::doubleValue).sum();
    }

    public long issueCount(){
        return issues.size();
    }

    public long defectCount(){
        return defects.size();
    }

    public double testedStoryPoints(){
        return featureStoryPoints() + bugStoryPoints();
    }

    public double qualityFactor(){
        if(testedStoryPoints() == 0 || defectCount() == 0)
            return 0;

        return defectCount()/testedStoryPoints();
    }

    @Override
    public String toString() {
        String formatStr = "[%s] Bug: %3.1fSP (%d) ER: %4.1fSP (%d) Task: %4.1fSP (%d)  <total: %4.1fSP (%d), ISD: %d, QF: %4.2f>";
        return String.format(formatStr, developer, bugStoryPoints(), bugCount(),
                featureStoryPoints(), featureCount(),
                taskStoryPoints(), taskCount(), storyPoints(), issueCount(), defectCount(), qualityFactor());
    }

    public void printReport(){
        System.out.println(this.toString());

        for(Issue i : issues)
            System.out.println(i);

        if(!defects.isEmpty()) {
            System.out.println("\tIn-Sprint Defects: ");

            for (Issue i : defects)
                System.out.println(i);
        }

        System.out.println("");
    }

}
