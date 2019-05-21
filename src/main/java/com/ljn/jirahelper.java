package com.ljn;

import de.objektkontor.clp.InvalidParameterException;
import de.objektkontor.clp.Parser;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Issue.SearchResult;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.agile.Sprint;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class JiraHelper {

    public static String JIRA_URL = null;
    public static String USER = null;
    public static String PASSWORD = null;
    public static int SEARCHONBOARD = 1115;
    public static String JQL_STANDARD_ISSUES_SPRINT = "sprint = %d and issuetype in standardIssueTypes()";
    public static String JQL_IN_SPRINT_DEFECTS = "issuetype= 'Bug Sub-Task' and sprint = %d";

    public static void main(String[] args) {

        initProperties();

        Parser<CommandArgs> parser = new Parser<>("args", CommandArgs.class);

        java.util.HashMap<String, Double> productivity = new HashMap<>();

        try {

            BasicCredentials creds = new BasicCredentials(USER, PASSWORD);
            JiraClient jira = new JiraClient(JIRA_URL, creds);

            CommandArgs testParameter = parser.parse(new CommandArgs(), args);

            //get sprint number
            int sprint_number = testParameter.getSprint();
            int team_number = 4;

            Sprint sprint = null;

            if(sprint_number <= 0) {
                System.out.println("Please specifiy a Sprint number.");
                return;
            }else {
                sprint = getSprint(jira, team_number, sprint_number);
                if(sprint != null) {
                    System.out.println(sprint);
                }
            }

            //verifying or collecting statistics
            String jql = String.format(JQL_STANDARD_ISSUES_SPRINT, (int)sprint.getId());
            String isdjql = String.format(JQL_IN_SPRINT_DEFECTS, (int)sprint.getId());

            SearchResult sr = jira.searchIssues(jql);
            List<Issue> issues = sr.issues;

            if(testParameter.isVerify())
                SprintChecker.checkFields(issues);

            List<DeveloperProductivity> prod_list = new java.util.ArrayList<>();
            Map<String, List<Issue>> isd_map = new HashMap<>();

            if(testParameter.isProd() || testParameter.isExport()) {
                prod_list = ProductivityCalculator.Run(issues, true);
            }

            if(testParameter.isIsd()||testParameter.isExport())
                isd_map = getISD(jira.searchIssues(isdjql).issues);

            if(!isd_map.isEmpty() && !prod_list.isEmpty()){
                attachISDtoProductivity(prod_list, isd_map);
                System.out.println("==============  Defects  ===========");
                for(DeveloperProductivity dp : prod_list){
                    if(!dp.defects.isEmpty()) {
                        System.out.println(dp.developer);
                        for (Issue issue : dp.defects) {
                            SprintChecker.printLog(issue, "");
                        }
                    }
                }
            }
            if(testParameter.isExport())
            {
                exportProdData(sprint, prod_list);
            }

        } catch (InvalidParameterException | JiraException e) {
            e.printStackTrace();
        }
    }

    private static void attachISDtoProductivity(List<DeveloperProductivity> list, Map<String, List<Issue>> map){
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
        java.util.Properties pps = new java.util.Properties();
        try {
            pps.load(new java.io.FileInputStream("helper.properties"));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        java.util.Enumeration enum_prop = pps.propertyNames();
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
                    SEARCHONBOARD = Integer.valueOf(strValue);
                    break;

            }
        }
    }

    private static void writeSprintToProperties(){

    }

    public static Sprint getSprint(JiraClient jira, int team, int sprint){

        String sprintNamePattern = "ANE Sprint %d - Roadmap %d";
        String sprintName = String.format(sprintNamePattern, sprint, team);

        try {
            net.rcarz.jiraclient.agile.AgileClient ac = new net.rcarz.jiraclient.agile.AgileClient(jira);
            net.rcarz.jiraclient.agile.Board dl =  ac.getBoard(SEARCHONBOARD);
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

        System.out.println("==================  Quality  =====================");
        System.out.println("Total ISD: " + issues.size());
        issues.stream().forEach(n->SprintChecker.printLog(n, ""));
        List<Issue> invalidList = new java.util.ArrayList<>();
        Map<String, List<Issue>> validList = new java.util.TreeMap<>(String::compareTo);

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

        System.out.println("Invalid ISD: " + invalidList.size());
        invalidList.stream().forEach(n-> SprintChecker.printLog(n, ""));


        int total_valid = 0;
        for(String dcb : validList.keySet()){
            total_valid += validList.get(dcb).size();
            System.out.println(dcb + ", ISD count: " + validList.get(dcb).size());
            validList.get(dcb).stream().forEach(n->SprintChecker.printLog(n, ""));
        }
        System.out.println("Invalid ISD: " + total_valid);


        return validList;
    }





}