package com.ljn;

import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.IssueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProductivityCalculator {

    public static List<DeveloperProductivity> Run(List<Issue> issues){
        if (issues.isEmpty())
            return new ArrayList<>();

        HashMap<String, ArrayList<ProductivityUnit>> map = new java.util.HashMap<>();

        //group by developer
        for (Issue issue : issues) {
            String developer = "Unknown";

            //empty developer
            if(issue.getDeveloper() == null)
            {
                if(issue.getStoryPoints() == null || issue.getStoryPoints() <= 0){
                    continue;
                }else{
                    if(issue.getTaskCategory() != null && issue.getTaskCategory().equals("QA")){
                        developer = "QA";
                    }
                }
            }else {
                developer = issue.getDeveloper().getDisplayName();
            }


            if(!map.containsKey(developer)){
                map.put(developer, new ArrayList<>());
            }

            ProductivityUnit pu = new ProductivityUnit();
            IssueType type = issue.getIssueType();
            pu.setDeveloper(developer);
            pu.setType(type.getName());
            pu.setStoryPoints(issue.getStoryPoints());
            pu.setIssue(issue);

            map.get(developer).add(pu);

        }

        // calculate the story points
        ArrayList<DeveloperProductivity> developerPro = new ArrayList<>();

        for(String dev : map.keySet()) {
            ArrayList<ProductivityUnit> list = map.get(dev);
            double task_sp = 0;
            double bug_sp = 0;
            double feature_enhancement_sp = 0;
            int bugCount = 0;
            int taskCount = 0;
            int erCount = 0;
            List<Issue> allissue = new java.util.ArrayList<>();
            for (ProductivityUnit pu : list) {
                allissue.add(pu.getIssue());
                switch (pu.type) {
                    case "Bug":
                        bug_sp += pu.storyPoints;
                        bugCount++;
                        break;
                    case "Enhancement":
                    case "Requirement/User Story":
                        feature_enhancement_sp += pu.storyPoints;
                        erCount++;
                        break;
                    case "Task":
                        task_sp += pu.storyPoints;
                        taskCount++;
                        break;
                    default:
                        break;
                }
            }

            DeveloperProductivity dp = new DeveloperProductivity(dev, task_sp, bug_sp, feature_enhancement_sp);
            dp.setIssues(allissue);
            dp.taskCount = taskCount;
            dp.bugCount = bugCount;
            dp.erCount = erCount;

            developerPro.add(dp);
        }

        developerPro.sort(java.util.Comparator.comparing(h -> h.developer));



        return developerPro;

    }
}
