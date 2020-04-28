package com.ljn;

import com.ljn.utils.CommandArgs;
import com.ljn.utils.Emoji;
import com.ljn.utils.SysPath;
import com.ljn.utils.WorkdayUtils;
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
import java.util.stream.Collectors;


public class JiraHelper {

    public static String JIRA_URL = null;
    public static String USER = null;
    public static String PASSWORD = null;
    public static int SEARCH_ON_BOARD = 1380;
    public static String JQL_STANDARD_ISSUES_SPRINT = "sprint = %d and issuetype in standardIssueTypes()";
    public static String JQL_IN_SPRINT_DEFECTS = "issuetype= 'Bug Sub-Task' and sprint = %d";

    public static void main(String[] args) {

        initProperties();

        Parser<CommandArgs> parser = new Parser<>("args", CommandArgs.class);
        CommandArgs testParameter = null;
        try {
            testParameter = parser.parse(new CommandArgs(), args);
        } catch (InvalidParameterException e) {
            e.printStackTrace();
            return;
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

        Emoji.iconMode = testParameter.isIconMode();

        BasicCredentials creds = new BasicCredentials(USER, PASSWORD);
        JiraClient jira = null;
        try {
            jira = new JiraClient(JIRA_URL, creds);
        } catch (JiraException e) {
            e.printStackTrace();
        }

        if(jira == null)
            return;

        printSprintReport(testParameter, sprint_number, team_number, jira);


        /*
        int days = 180;
        String developer = "ryuan";
        printDeveloperStatistics(jira, days, developer);

        developer = "fgao";
        printDeveloperStatistics(jira, days, developer);
        */
    }

    private static void printDeveloperStatistics(JiraClient jira, int days, String developer) {
        String issue_jql = "Developer = '"+ developer + "' AND resolved > -" + days + "d AND issuetype in standardIssueTypes()";
        String defects_jql = "'Defect Introduced By' = '"+ developer + "' AND created > -" + days + "d";

        Map<String, DeveloperView> dv = getDeveloperView(jira, issue_jql, defects_jql);
        DeveloperView.printStatistics(dv, days);
        dv.values().stream().forEach(p -> { System.out.println(p.toString());});
    }

    private static void printSprintReport(CommandArgs testParameter, int sprint_number, int team_number, JiraClient jira) {
        Sprint sprint = getSprint(jira, team_number, sprint_number);
        if(sprint == null) {
            System.out.println("Please specify a valid Sprint number.");
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


        if(!testParameter.isPlanMode()) {
            List<Issue> defects = null;
            try {
                String isdjql = String.format(JQL_IN_SPRINT_DEFECTS, (int) sprint.getId());
                defects = jira.searchIssues(isdjql).issues;
            } catch (JiraException e) {
                e.printStackTrace();
            }

            Map<String, DeveloperView> dvs = DeveloperView.getDeveloperView(issues, defects);
            System.out.println();
            System.out.println("========================:     DEV VIEW    :====================================");
            dvs.values().stream().forEach(p -> p.printReport());
            printDefectReport(defects);


            Map<String, TesterView> tvs = TesterView.getTesterView(issues, defects);
            System.out.println();
            System.out.println("========================:     QA VIEW    :====================================");
            tvs.values().stream().forEach(p -> p.printReport());

            System.out.println("========================:     STATISTICS VIEW    :====================================");
            DeveloperView.printStatistics(dvs);

            if (testParameter.isExport())
                exportProdData(sprint, dvs);

            HourLoggedChecker loggedChecker = new HourLoggedChecker(sprint, jira);
            System.out.println("========================:   LOGGED HOURS  :====================================");
            loggedChecker.Run();


            System.out.println("========================:   REPORT END  :====================================");
        }else{
            //plan mode

            printCurrentCommitment(issues);
        }
    }

    public static Map<String, DeveloperView> getDeveloperView(JiraClient jira, String issue_jql, String defects_jql){
        try {
            return DeveloperView.getDeveloperView(jira.searchIssues(issue_jql).issues, jira.searchIssues(defects_jql).issues);
        } catch (JiraException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void printCurrentCommitment(List<Issue> issues) {
        System.out.println();
        System.out.println("=====================: CURRENT COMMITMENT (workload balance) :==============================");
        Map<String, DeveloperView> dvs = DeveloperView.getDeveloperViewByAssignee(issues);
        dvs.values().stream().forEach(p -> System.out.println(p));
    }


    public static void printDefectReport(List<Issue> defects){
        List<Issue> invalidIssue = defects.stream().filter(p->!p.isValidBug()).collect(Collectors.toList());
        System.out.println("==== Invalid/total In-sprint defects: "+ invalidIssue.size() + " / " + defects.size());
        if(!defects.isEmpty()) {
            System.out.println("---- Invalid: ");
            invalidIssue.stream().forEach(n -> System.out.println(n));
            System.out.println("---- Valid: ");
            defects.stream().filter(p -> p.isValidBug()).forEach(n -> System.out.println(n));
        }
    }


    private static void renderingJade(Sprint sprint, List<DeveloperProductivity> prod_list, Map<String, List<Issue>> isd_map) throws java.io.IOException {
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

    private static void exportProdData(Sprint sprint, Map<String, DeveloperView> dvs) {
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

            ArrayList<DeveloperView> l = new ArrayList<>();
            l.addAll(dvs.values());
            l.sort(Comparator.comparing(p->p.getDeveloper()));

            colnumber = 0; //reset
            for(DeveloperView dv : l) {
                colnumber = 0;
                Row row = sheet.createRow(rownumber);
                row.createCell(colnumber).setCellValue(dv.getDeveloper());
                colnumber++;

                //SP
                row.createCell(colnumber).setCellValue(dv.storyPoints());
                colnumber++;

                //ISD
                row.createCell(colnumber).setCellValue(dv.defectCount());
                colnumber++;

                //ISD/SP
                row.createCell(colnumber).setCellValue(String.format("%.2f", dv.qualityFactor()));
                colnumber++;

                //ER
                row.createCell(colnumber).setCellValue(dv.featureStoryPoints());
                colnumber++;

                //BUG
                row.createCell(colnumber).setCellValue(dv.bugStoryPoints());
                colnumber++;

                //BUG count
                row.createCell(colnumber).setCellValue(dv.bugCount());
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
            java.util.List<Sprint> listSprints = dl.getSprints(480);
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







}