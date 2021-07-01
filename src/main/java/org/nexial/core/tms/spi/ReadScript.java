package org.nexial.core.tms.spi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.jetbrains.annotations.NotNull;
import org.nexial.commons.utils.TextUtils;
import org.nexial.core.excel.Excel;
import org.nexial.core.excel.Excel.Worksheet;
import org.nexial.core.excel.ExcelAddress;
import org.nexial.core.excel.ExcelArea;
import org.nexial.core.model.TestProject;
import org.nexial.core.tms.model.TmsCustomStep;
import org.nexial.core.tms.model.TmsTestCase;
import org.nexial.core.tms.model.TmsTestStep;
import org.nexial.core.utils.InputFileUtils;

import static org.nexial.core.NexialConst.MSG_PROBLMATIC_NAME;
import static org.nexial.core.excel.ExcelConfig.*;

/**
 * Parse a script file and return the List of Test Cases by reading the scenarios.
 */
public class ReadScript {

    /**
     * Parse the script file specified by the provided path and return the {@link List} of Test Cases for it
     *
     * @param filepath the path of the script file
     * @return List of test cases
     */
    public static List<TmsTestCase> loadScript(String filepath) {
        List<TmsTestCase> testCases = new ArrayList<>();
        try {
            Excel script = getScript(filepath);
            List<String> scenarioNames = InputFileUtils.retrieveValidTestScenarioNames(script);
            // parse the script file for every scenario and create TmsTestCase objects
            for (String scenarioName : scenarioNames) {
                Worksheet worksheet = script.worksheet(scenarioName);
                TmsTestCase testCase = new TmsTestCase(worksheet);
                if (StringUtils.startsWith(testCase.getName().toLowerCase(), "(nat)")) { continue; }
                List<TmsTestStep> activities = parseScenario(testCase, worksheet);
                testCase.setTestSteps(activities);
                testCases.add(testCase);
            }
        } catch (Exception e) {
            System.err.println("Error occurred while parsing the excel file: " + filepath + " : " + e.getMessage());
            System.exit(-1);
        }
        return testCases;
    }

    /**
     * Perform validations on the path provided and return and {@link Excel} instance of the Script file
     *
     * @param testScriptPath path of the script file
     * @return Excel instance of script file
     */
    @NotNull
    protected static Excel getScript(String testScriptPath) {
        File docLocation = new File(testScriptPath);
        if (!docLocation.exists()) {
            System.err.println("The path specified does not exist");
            System.exit(-1);
        }
        Excel script = InputFileUtils.resolveValidScript(testScriptPath);
        if (script == null) {
            System.err.println("Invalid test script - " + testScriptPath);
            System.exit(0);
        }

        File testScriptFile = new File(testScriptPath);

        TestProject project = TestProject.newInstance(testScriptFile);
        if (!project.isStandardStructure()) {
            System.err.println("specified test script (" + testScriptFile + ") not following standard project " +
                               "structure, related directories would not be resolved from commandline arguments.");
            System.exit(-1);
        }
        return script;
    }

    /**
     * Parse each scenario present in the script and return a list if Test Steps inside the scenario
     *
     * @param scenario A scenario in the Nexial script, mapped to a single Test Case
     * @param worksheet the current {@link Worksheet}
     * @return List of Test Steps belonging to the scenario
     */
    private static List<TmsTestStep> parseScenario(TmsTestCase scenario, Worksheet worksheet) {
        int lastCommandRow = worksheet.findLastDataRow(ADDR_COMMAND_START);
        ExcelArea area = new ExcelArea(worksheet,
                                       new ExcelAddress(FIRST_STEP_ROW + ":" + COL_REASON + lastCommandRow),
                                       false);
        List<TmsTestStep> testSteps = new ArrayList<>();
        List<String> testCases = new ArrayList<>();

        String scenarioRef = "Error found in [" + worksheet.getFile().getName() + "][" + worksheet.getName() + "]";

        // 3. parse for test steps->test case grouping
        TmsTestStep currentActivity = null;
        for (int i = 0; i < area.getWholeArea().size(); i++) {
            List<XSSFCell> row = area.getWholeArea().get(i);

            XSSFCell cellActivity = row.get(COL_IDX_TESTCASE);
            String errorPrefix = scenarioRef + "[" + cellActivity.getReference() + "]: ";
            String activity = Excel.getCellValue(cellActivity);

            // detect space only activity name
            if (StringUtils.isNotEmpty(activity) && StringUtils.isAllBlank(activity)) {
                System.err.println(errorPrefix + "Found invalid, space-only activity name");
                System.exit(-1);
            }

            // detect leading/trailing non-printable characters
            if (!StringUtils.equals(activity, StringUtils.trim(activity))) {
                System.err.printf(errorPrefix + MSG_PROBLMATIC_NAME + "%n", "activity", activity);
                System.exit(-1);
            }

            boolean hasActivity = StringUtils.isNotBlank(activity);
            if (currentActivity == null && !hasActivity) {
                // first row must define test case (hence at least 1 test case is required)
                System.err.println(errorPrefix + "Invalid format; First row must contain valid activity name");
                System.exit(-1);
            }
            if (hasActivity) {
                if (testCases.contains(activity)) {
                    // found duplicate activity name!
                    System.err.println(errorPrefix + "Found duplicate activity name '" + activity + "'");
                    System.exit(-1);
                }
                testCases.add(activity);
                currentActivity = new TmsTestStep(scenario);
                currentActivity.setName(TextUtils.toOneLine(activity, true));
                testSteps.add(currentActivity);
            }
            TmsCustomStep testStep = new TmsCustomStep(worksheet, row, currentActivity);
            currentActivity.addTmsCustomTestStep(testStep);
        }
        return testSteps;
    }
}
