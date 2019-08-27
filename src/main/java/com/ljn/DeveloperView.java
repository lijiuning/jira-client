package com.ljn;

import net.rcarz.jiraclient.Issue;

import java.util.*;
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
        String formatStr = "%-16s Bug: %3.1fSP (%d) ER: %4.1fSP (%d) Task: %4.1fSP (%d)  <total: %4.1fSP (%d), ISD: %d, QF: %4.2f>";
        return String.format(formatStr, "[" + developer + "]", bugStoryPoints(), bugCount(),
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

    public static Map<String, DeveloperView> getDeveloperView(List<Issue> issues, List<Issue> defects) {
        Map<String, DeveloperView> dvs = new HashMap<>();

        for(Issue i: issues){
            if(i.getDeveloper() == null)
                continue;

            if(!dvs.containsKey(i.getDeveloper().getDisplayName())){
                DeveloperView dv = new DeveloperView(i.getDeveloper().getDisplayName());
                dvs.put(dv.getDeveloper(), dv);
            }

            dvs.get(i.getDeveloper().getDisplayName()).getIssues().add(i);
        }

        if(defects != null) {
            for (Issue i : defects) {
                if (i.getDefectIntroducedBy() == null) {
                    continue;
                }

                if (!dvs.containsKey(i.getDefectIntroducedBy().getDisplayName())) {
                    continue;
                }

                dvs.get(i.getDefectIntroducedBy().getDisplayName()).getDefects().add(i);
            }
        }

        return dvs;
    }


    public static Map<String, DeveloperView> getDeveloperViewByAssignee(List<Issue> issues) {
        Map<String, DeveloperView> dvs = new HashMap<>();

        for(Issue i: issues){
            if(i.getAssignee() == null)
                continue;

            if(!dvs.containsKey(i.getAssignee().getDisplayName())){
                DeveloperView dv = new DeveloperView(i.getAssignee().getDisplayName());
                dvs.put(dv.getDeveloper(), dv);
            }

            dvs.get(i.getAssignee().getDisplayName()).getIssues().add(i);
        }

        return dvs;
    }

    public static void printStatistics(Map<String, DeveloperView> dvs){
        DoubleSummaryStatistics iss = dvs.values().stream().mapToDouble((x)->x.storyPoints()).summaryStatistics();
        System.out.println("----------------------------------------------------");
        System.out.println(String.format("TOT. : %4.1f", iss.getSum()));
        System.out.println(String.format("AVG. : %4.1f", iss.getAverage()));

        System.out.println("----------------------------------------------------");

        DoubleSummaryStatistics tss = dvs.values().stream().mapToDouble((x)->x.testedStoryPoints()).summaryStatistics();
        System.out.println(String.format("TOT. (tested): %4.1f", tss.getSum()));
        System.out.println(String.format("AVG. (tested): %4.1f",  tss.getAverage()));

        DoubleSummaryStatistics fss = dvs.values().stream().mapToDouble((x)->x.featureStoryPoints()).summaryStatistics();
        System.out.println(String.format("TOT. (features): %4.1f", fss.getSum()));
        System.out.println(String.format("AVG. (features): %4.1f",  fss.getAverage()));

        DoubleSummaryStatistics tass = dvs.values().stream().mapToDouble((x)->x.taskStoryPoints()).summaryStatistics();
        System.out.println(String.format("TOT. (tasks): %4.1f", tass.getSum()));
        System.out.println(String.format("AVG. (tasks): %4.1f",  tass.getAverage()));

        DoubleSummaryStatistics bss = dvs.values().stream().mapToDouble((x)->x.bugStoryPoints()).summaryStatistics();
        System.out.println(String.format("TOT. (bugs): %4.1f", bss.getSum()));
        System.out.println(String.format("AVG. (bugs): %4.1f", bss.getAverage()));
    }

    public static void printStatistics(Map<String, DeveloperView> dvs, int days){
        
        int weeks = days > 0 ? days / 7 : 1;
        DoubleSummaryStatistics iss = dvs.values().stream().mapToDouble((x)->x.storyPoints()).summaryStatistics();

        System.out.println(String.format("TOT. : %4.1f", iss.getSum()));
        System.out.println(String.format("Weeks. : %d", weeks));
        System.out.println(String.format("AVG. : %4.1f", days > 0 ? iss.getSum() / weeks : iss.getAverage()));

        System.out.println("----------------------------------------------------");

        DoubleSummaryStatistics tss = dvs.values().stream().mapToDouble((x)->x.testedStoryPoints()).summaryStatistics();
        System.out.println(String.format("TOT. (tested): %4.1f", tss.getSum()));
        System.out.println(String.format("AVG (tested per week): %4.1f",  tss.getSum() / weeks));

        DoubleSummaryStatistics fss = dvs.values().stream().mapToDouble((x)->x.featureStoryPoints()).summaryStatistics();
        System.out.println(String.format("TOT. (features): %4.1f", fss.getSum()));

        DoubleSummaryStatistics tass = dvs.values().stream().mapToDouble((x)->x.taskStoryPoints()).summaryStatistics();
        System.out.println(String.format("TOT. (tasks): %4.1f", tass.getSum()));

        DoubleSummaryStatistics bss = dvs.values().stream().mapToDouble((x)->x.bugStoryPoints()).summaryStatistics();
        System.out.println(String.format("TOT. (bugs): %4.1f", bss.getSum()));
    }
}
