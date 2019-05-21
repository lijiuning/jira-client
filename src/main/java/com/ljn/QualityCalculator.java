package com.ljn;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Resolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class QualityCalculator {

    public static Map<String, List<Issue>> Run(List<Issue> issues, boolean printlog){
        if(issues.isEmpty())
            return new java.util.HashMap<>();

        if(printlog)
            System.out.println("Total ISD: " + issues.size());

        List<Issue> invalidList = new ArrayList<>();
        Map<String, List<Issue>> validList = new TreeMap<>(String::compareTo);

        for(Issue issue : issues){
            Resolution ro = issue.getResolution();

            //unresolved
            if(ro == null)
                continue;

            if(!ro.getName().equals("Fixed"))
            {
                invalidList.add(issue);
                if(printlog && issue.getDefectIntroducedBy() != null && !issue.getDefectIntroducedBy().getDisplayName().equals("Not Applicable")){
                    com.ljn.SprintChecker.printLog(issue, " defect created by error: " + issue.getDefectIntroducedBy().getDisplayName());
                }
                continue;
            }else{
                if(issue.getDefectIntroducedBy() == null)
                {
                    if(printlog)
                        com.ljn.SprintChecker.printLog(issue, "defect introduced by is empty!");

                    continue;
                }else{
                    String dcb = issue.getDefectIntroducedBy().getDisplayName();

                    if(!validList.containsKey(dcb)){
                        validList.put(dcb, new java.util.ArrayList<Issue>());
                    }
                    validList.get(dcb).add(issue);
                }
            }
        }

        if(printlog) {
            System.out.println("Invalid ISD: " + invalidList.size());


            for (String dcb : validList.keySet()) {
                System.out.println(dcb + ", ISD count: " + validList.get(dcb).size());
            }
        }

        return validList;
    }
}
