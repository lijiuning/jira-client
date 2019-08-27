package com.ljn;

import net.rcarz.jiraclient.Issue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TesterView {
    public String getTester() {
        return tester;
    }

    public void setTester(String tester) {
        this.tester = tester;
    }

    private String tester = "";

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

    public TesterView(String tester){
        this.tester = tester;
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
        String formatStr = "[%s] Bug: %3.1fSP (%d) ER: %4.1fSP (%d) Task: %4.1fSP (%d)  <total: %4.1fSP (%d), ISD: %d, TF: %4.2f>";
        return String.format(formatStr, tester, bugStoryPoints(), bugCount(),
                featureStoryPoints(), featureCount(),
                taskStoryPoints(), taskCount(), storyPoints(), issueCount(), defectCount(), qualityFactor());
    }

    public void printReport(){
        System.out.println(this.toString());

        for(Issue i : issues)
            System.out.println(i);

        if(!defects.isEmpty()) {
            System.out.println("\tIn-sprint Defects: ");

            for (Issue i : defects)
                System.out.println(i);
        }

        System.out.println("");
    }

    public static Map<String, TesterView> getTesterView(List<Issue> issues, List<Issue> defects) {
        Map<String, TesterView> dvs = new HashMap<>();

        for(Issue i: issues){
            if(i.getTester() == null)
                continue;

            if(!dvs.containsKey(i.getTester().getDisplayName())){
                TesterView dv = new TesterView(i.getTester().getDisplayName());
                dvs.put(dv.getTester(), dv);
            }

            dvs.get(i.getTester().getDisplayName()).getIssues().add(i);
        }

        if(defects != null) {
            for (Issue i : defects) {

                if (!dvs.containsKey(i.getReporter().getDisplayName())) {
                    continue;
                }

                dvs.get(i.getReporter().getDisplayName()).getDefects().add(i);
            }
        }
        return dvs;
    }
}
