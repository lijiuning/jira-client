package com.ljn;

import net.rcarz.jiraclient.WorkLog;

import java.util.ArrayList;
import java.util.List;

public class LogHourUnit {
    public LogHourUnit(){
        workLogList = new ArrayList<>();
    }
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLoggedSeconds() {
        return this.workLogList.stream().mapToLong(WorkLog::getTimeSpentSeconds).sum();
    }

    public List<WorkLog> getWorkLogList() {
        return workLogList;
    }

    public void setWorkLogList(List<WorkLog> workLogList) {
        this.workLogList = workLogList;
    }

    private List<WorkLog> workLogList = null;

    @Override
    public String toString() {
        return String.format("[%s] %d hours", name, (getLoggedSeconds()/60/60));
    }
}
