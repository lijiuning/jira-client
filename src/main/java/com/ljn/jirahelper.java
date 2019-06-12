package com.ljn;

import de.neuland.jade4j.Jade4J;
import de.objektkontor.clp.InvalidParameterException;
import de.objektkontor.clp.Parser;
import net.rcarz.jiraclient.*;
import net.rcarz.jiraclient.Issue.SearchResult;
import net.rcarz.jiraclient.agile.Sprint;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;


public class JiraHelper {

    public static String JIRA_URL = null;
    public static String USER = null;
    public static String PASSWORD = null;
    public static int SEARCH_ON_BOARD = 1011;
    public static String JQL_STANDARD_ISSUES_SPRINT = "sprint = %d and issuetype in standardIssueTypes()";
    public static String JQL_IN_SPRINT_DEFECTS = "issuetype= 'Bug Sub-Task' and sprint = %d";

    public static void main(String[] args) {

        initProperties();

        Parser<com.ljn.CommandArgs> parser = new Parser<>("args", CommandArgs.class);
        CommandArgs testParameter = null;
        try {
            testParameter = parser.parse(new CommandArgs(), args);
        } catch (InvalidParameterException e) {
            e.printStackTrace();
        }

        //get sprint number
        int sprint_number = testParameter.getSprint();
        if(sprint_number <= 0) {
            System.out.println("Please specifiy a Sprint number.");
            return;
        }

        int team_number = testParameter.getTeam();
        if(team_number <= 0){
            System.out.println("Please specifiy a team number.");
            return;
        }

        BasicCredentials creds = new BasicCredentials(USER, PASSWORD);
        JiraClient jira = null;
        try {
            jira = new JiraClient(JIRA_URL, creds);
        } catch (JiraException e) {
            e.printStackTrace();
        }

        if(jira == null)
            return;

        Sprint sprint = getSprint(jira, team_number, sprint_number);
        if(sprint == null) {
            System.out.println("Please specifiy a valid Sprint number.");
            return;
        }

        System.out.println();
        System.out.println(sprint);

        // print active sprint data
        if(sprint.getState().equals("active")){
            Calendar cal = Calendar.getInstance();
            if(cal.getTime().after( sprint.getStartDate()) && cal.getTime().before(sprint.getEndDate())){
                int workday = WorkdayUtils.howManyWorkday(sprint.getStartDate(), cal.getTime());
                if(workday>0){
                    System.out.println("It should be " + workday + " workdays from the beginning of current Sprint to now.");
                }
            }
        }

        //verifying or collecting statistics
        SearchResult sr = null;
        try {
            String jql = String.format(JQL_STANDARD_ISSUES_SPRINT, (int)sprint.getId());
            sr = jira.searchIssues(jql, StringUtils.join(Field.includeFields, ','));
        } catch (JiraException e) {
            e.printStackTrace();
        }

        List<Issue> issues = sr.issues;

        if(testParameter.isVerify())
            SprintChecker.checkFields(issues);

        List<com.ljn.DeveloperProductivity> prod_list = new java.util.ArrayList<>();

        Map<String, List<Issue>> isd_map = new HashMap<>();
        List<Issue> defects = null;
        try {
            String isdjql = String.format(JQL_IN_SPRINT_DEFECTS, (int)sprint.getId());
            defects = jira.searchIssues(isdjql).issues;
        } catch (JiraException e) {
            e.printStackTrace();
        }

        if(testParameter.isProd() || testParameter.isExport()) {
            prod_list = ProductivityCalculator.Run(issues);
        }

        if(testParameter.isIsd() || testParameter.isExport())
            isd_map = getISD(defects);

        if(!prod_list.isEmpty() && !isd_map.isEmpty())
            attachISDtoProductivity(prod_list, isd_map);

        checkOverDue(prod_list, isd_map, defects);

        printReport(prod_list, isd_map, defects);


        if(testParameter.isExport())
            exportProdData(sprint, prod_list);


        HourLoggedChecker loggedChecker = new HourLoggedChecker(sprint, jira);
        loggedChecker.Run();

    }

    private static void overdue(Issue issue){
        Date now = new Date();

        if(issue.getStatus().getName().toLowerCase().equals("closed") || issue.getStatus().getName().toLowerCase().equals("test")){
            return;
        }
        if(issue.getDueDate() == null)
        {
            SprintChecker.printLog(issue, " due date is EMPTY!");
        }else if(issue.getDueDate().before(now)) {
            SprintChecker.printLog(issue, " is OVER due!"  + issue.getDueDate());

        }
    }
    private static void checkOverDue(List<DeveloperProductivity> prod_list, Map<String, List<Issue>> isd_map, List<Issue> defects) {
        System.out.println("Checking due-dates...");
        System.out.println("-------------- Sprint commitment -----------------");
        for(DeveloperProductivity dp : prod_list) {
            for(Issue issue : dp.getIssues()){
                overdue(issue);
            }
        }

        System.out.println("-------------- In-sprint defects -----------------");
        for(String dcb : isd_map.keySet()){
            List<Issue> isds = isd_map.get(dcb);
            for(Issue i : isds){
                overdue(i);
            }
        }
    }

    private static void printReport(List<DeveloperProductivity> prod_list, Map<String, List<Issue>> isd_map, List<Issue> defects) {
        System.out.println("=======================:  Productivity  :======================================");
        for(DeveloperProductivity dp : prod_list) {
            System.out.println(dp.toString());
            for(Issue issue : dp.getIssues()){
                SprintChecker.printLog(issue, "");
            }
            System.out.println();
        }

        System.out.println("=====================:  Quality  :==========================================");
        System.out.println("-------------------- Total in-sprint defects: (" + defects.size() +") ---------------------- ");

        int total_valid = 0;

        for(String dcb : isd_map.keySet()){
            List<Issue> isds = isd_map.get(dcb);
            defects.removeIf(n->isds.stream().anyMatch(p->p.getKey() == n.getKey()));
            total_valid += isd_map.get(dcb).size();
            System.out.println(dcb + ", ISD count: (" + isd_map.get(dcb).size() +")");
            isd_map.get(dcb).stream().forEach(n-> SprintChecker.printLog(n, "["+ (n.getResolution() == null ? "Unresolved":n.getResolution().getName()) + "]"));
        }
        System.out.println("-------------------- Total valid in-sprint defects: (" + total_valid + ") -------------------- ");

        defects.stream().forEach(n-> SprintChecker.printLog(n, "["+ (n.getAssignee() == null ? "Unassigned" : n.getAssignee().getDisplayName()) + "] ["+ (n.getResolution() == null ? "Unresolved":n.getResolution().getName()) + "]"));
        System.out.println("-------------------- Total invalid / unresolved ISD: (" + defects.size() +") ----------------- ");
    }


    private static void renderingJade(Sprint sprint, List<com.ljn.DeveloperProductivity> prod_list, Map<String, List<Issue>> isd_map) throws java.io.IOException {
        java.util.Map<String, Object> model = new java.util.HashMap<String, Object>();
        model.put("pageName", sprint.getName());
        model.put("products", prod_list);
        model.put("defects", isd_map);
        String html = renderJade(model);
        String saveFilePath = "./";
        java.io.FileWriter fw = new java.io.FileWriter(saveFilePath + "report.html");
        fw.write(html);
        fw.close();
    }

    public static String renderJade(Map<String, Object> model){
        de.neuland.jade4j.template.JadeTemplate template = null;
        try {
            template = Jade4J.getTemplate("./index.jade");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        String html = Jade4J.render(template, model);
        return html;
    }

    private static void attachISDtoProductivity(List<DeveloperProductivity> list, Map<String, List<Issue>> map){
        //TODO: we need to consider that the issue might be led by someone who did not output anything
        for (DeveloperProductivity dp : list) {
            if(map.containsKey(dp.developer)){
                dp.setIsdCount(map.get(dp.developer).size());
                dp.setDefects(map.get(dp.developer));
            }
        }
    }

    private static void exportProdData(Sprint sprint, List<DeveloperProductivity> list) {
        try (Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) { //or new HSSFWorkbook();
            Sheet sheet = wb.createSheet(sprint.getName() + " - Productivity");

            short rownumber = 0;
            short colnumber = 0;
            Row row_header = sheet.createRow(rownumber);
            row_header.createCell(colnumber).setCellValue("Developer");
            colnumber++;

            row_header.createCell(colnumber).setCellValue("SP");
            colnumber++;

            row_header.createCell((short) colnumber).setCellValue("ISD");
            colnumber++;

            row_header.createCell((short) colnumber).setCellValue("ISD/SP");
            colnumber++;

            row_header.createCell((short) colnumber).setCellValue("ER");
            colnumber++;

            row_header.createCell((short) colnumber).setCellValue("BUG");
            colnumber++;

            row_header.createCell((short) colnumber).setCellValue("BUG(count)");
            colnumber++;

            rownumber = 1;

            colnumber = 0; //reset
            for(DeveloperProductivity dp : list) {
                colnumber = 0;
                Row row = sheet.createRow(rownumber);
                row.createCell(colnumber).setCellValue(dp.developer);
                colnumber++;

                //SP
                row.createCell(colnumber).setCellValue(dp.getTotalSP());
                colnumber++;

                //ISD
                row.createCell(colnumber).setCellValue(dp.getIsdCount());
                colnumber++;

                //ISD/SP
                row.createCell(colnumber).setCellValue(String.format("%.2f", dp.getISDPerSP()));
                colnumber++;

                //ER
                row.createCell(colnumber).setCellValue(dp.getErStoryPoints());
                colnumber++;

                //BUG
                row.createCell(colnumber).setCellValue(dp.getBugStoryPoints());
                colnumber++;

                //BUG count
                row.createCell(colnumber).setCellValue(dp.getBugCount());
                colnumber++;

                rownumber++;
            }
            // Write the output to a file
            try (java.io.FileOutputStream fileOut = new java.io.FileOutputStream(sprint.getName() + ".xlsx")) {
                wb.write(fileOut);
            } catch (java.io.FileNotFoundException e) {
                e.printStackTrace();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private static void initProperties() {
        Properties pps = new Properties();
        try {
            pps.load(new FileInputStream(new SysPath().jarPath() + "/helper.properties"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Enumeration<?> enum_prop = pps.propertyNames();
        while(enum_prop.hasMoreElements()) {
            String strKey = (String) enum_prop.nextElement();
            String strValue = pps.getProperty(strKey);
            switch (strKey){
                case "JIRA_URL":
                    JIRA_URL = strValue;
                    break;
                case "USER":
                    USER = strValue;
                    break;
                case "PASSWORD":
                    PASSWORD = strValue;
                    break;
                case "":
                    SEARCH_ON_BOARD = Integer.valueOf(strValue);
                    break;
            }
        }
    }


    public static Sprint getSprint(JiraClient jira, int team, int sprint){

        String sprintNamePattern = "ANE Sprint %d - Roadmap %d";
        String sprintName = String.format(sprintNamePattern, sprint, team);

        try {
            net.rcarz.jiraclient.agile.AgileClient ac = new net.rcarz.jiraclient.agile.AgileClient(jira);
            net.rcarz.jiraclient.agile.Board dl =  ac.getBoard(SEARCH_ON_BOARD);
            java.util.List<Sprint> listSprints = dl.getSprints();
            listSprints.sort((o1, o2) -> Long.compare(o2.getId(), o1.getId()));
            for(Sprint b : listSprints){
                if(b.getName().equals(sprintName))
                    return b;
            }
        } catch (JiraException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, List<Issue>> getISD(List<Issue> issues){
        if(issues.isEmpty())
            return new HashMap<>();

        List<Issue> invalidList = new java.util.ArrayList<>();
        Map<String, List<Issue>> validList = new TreeMap<>(String::compareTo);

        for(Issue issue : issues){
            net.rcarz.jiraclient.Resolution ro = issue.getResolution();
            if(ro == null)
                continue;
            if(!ro.getName().equals("Fixed"))
            {
                invalidList.add(issue);
                if(issue.getDefectIntroducedBy() != null && !issue.getDefectIntroducedBy().getDisplayName().equals("Not Applicable")){
                    SprintChecker.printLog(issue, " defect created by error: " + issue.getDefectIntroducedBy().getDisplayName());
                }
                continue;
            }else{
                if(issue.getDefectIntroducedBy()==null)
                {
                    SprintChecker.printLog(issue, "defect introduced by is empty!");
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

        //System.out.println("Invalid ISD: " + invalidList.size());
        //invalidList.stream().forEach(n-> SprintChecker.printLog(n, ""));

        return validList;
    }





}