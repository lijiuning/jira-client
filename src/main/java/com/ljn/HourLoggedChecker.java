package com.ljn;

import net.rcarz.jiraclient.*;
import net.rcarz.jiraclient.agile.Sprint;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class HourLoggedChecker {
    private JiraClient client;
    private Sprint sprint;
    public HourLoggedChecker(Sprint sprint, JiraClient jira){
        this.sprint = sprint;
        this.client = jira;
    }

    public void Run(){
        Issue.SearchResult sr = null;
        if(sprint == null)
            return;
        try {
            sr = client.searchIssues("sprint = " + sprint.getId(), StringUtils.join(Field.includeFields, ','), 1000);
            List<Issue> issues = sr.issues;
            List<net.rcarz.jiraclient.WorkLog> works = new java.util.ArrayList<>();
            issues.stream().forEach(n->{works.addAll(n.getWorkLogs());});

            //group
            HashMap<String, LogHourUnit> worklogs = new HashMap<>();
            HashMap<String, LogHourUnit> outOfSprintWorklogs = new HashMap<>();
            for (WorkLog log: works) {

                if(!worklogs.containsKey(log.getAuthor().getDisplayName())){
                    LogHourUnit lhu = new LogHourUnit();
                    lhu.setName(log.getAuthor().getDisplayName());
                    worklogs.put(log.getAuthor().getDisplayName(), lhu);
                }
                if(!outOfSprintWorklogs.containsKey(log.getAuthor().getDisplayName())){
                    LogHourUnit lhu = new LogHourUnit();
                    lhu.setName(log.getAuthor().getDisplayName());
                    outOfSprintWorklogs.put(log.getAuthor().getDisplayName(), lhu);
                }

                if(log.getStarted().after(sprint.getStartDate()) && log.getStarted().before(sprint.getEndDate())) {
                    worklogs.get(log.getAuthor().getDisplayName()).getWorkLogList().add(log);
                }else{
                    outOfSprintWorklogs.get(log.getAuthor().getDisplayName()).getWorkLogList().add(log);
                }

            }

            List<Map.Entry<String, LogHourUnit>> worklist = new ArrayList<>(worklogs.entrySet());
            Collections.sort(worklist, (o1, o2) -> Long.compare(o2.getValue().getLoggedSeconds(), o1.getValue().getLoggedSeconds()));

            worklist.forEach(p->{
                System.out.println(p.getValue() + "  Out of sprint: (" + outOfSprintWorklogs.get(p.getKey()).getLoggedSeconds()/60/60 + " hrs)");
            });




        } catch (JiraException e) {
            e.printStackTrace();
        }
    }
}
