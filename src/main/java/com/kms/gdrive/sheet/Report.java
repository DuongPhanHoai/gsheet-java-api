package com.kms.gdrive.sheet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

import com.kms.util.ListUtils;
import com.kms.util.StringUtils;

public class Report {
  public static final String CLASSNAME = "com.kms.gdrive.sheet.Report";
  static String TEST_NAME_COLUMN = "C";
  static String TEST_RESULT_COLUMN = "E";
  /**
   * Do not input same column for both Name and Result
   * @param TEST_NAME_COLUMN column character (Ex: "A" or "D") default is "C"
   * @param TEST_RESULT_COLUMN column character (Ex: "A" or "D") default is "E"
   */
  static public void set_TEST_COLUMNS(String TEST_NAME_COLUMN, String TEST_RESULT_COLUMN) {Report.TEST_NAME_COLUMN = TEST_NAME_COLUMN;Report.TEST_RESULT_COLUMN = TEST_RESULT_COLUMN;}
  static int TEST_NAME_START_ROW = 5;
  /**
   * @param TEST_NAME_START_ROW default value is 5
   */
  static public void set_TEST_NAME_START_ROW(int TEST_NAME_START_ROW) {Report.TEST_NAME_START_ROW = TEST_NAME_START_ROW;}
  static final int MAX_BLANK_ROW = 5;
  static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  // STATIC
  /**
   * find the test by name (static)
   * 
   * @param testName            The test name to find
   * @param sheetName           The sheet to find the test
   * @param sheetID             The sheetID which can get from the google sheet
   *                            URL
   * @param allowExistingResult TRUE: find Test which has result or not; FALSE:
   *                            find the test which does not has Result
   * @return The row index of the found test ; -1 if not found
   */
  public static int findTestByName(String testName, String sheetName, String sheetID, boolean allowExistingResult) {
    Report foundReport = getReport(sheetID);
    if (foundReport != null)
      return foundReport.findTestByName(testName, sheetName, allowExistingResult);
    else
      return -1;
  }

  /**
   * update the test result by name (static)
   * 
   * @param testName        The test name to find
   * @param testResult        The test result to update
   * @param sheetName       The sheet to find the test
   * @param sheetID         The sheetID which can get from the google sheet URL
   * @param overWriteResult Is True, overwrite result, else the new row of test
   *                        will be created for the result
   * @return The row index of the found test and update result successful ; -1 if
   *         not found
   */
  public static int updateTestResultByName(String testName, String testResult, String sheetName, String sheetID,
      boolean overWriteResult) {
    Report foundReport = getReport(sheetID);
    if (foundReport != null)
      return foundReport.updateTestResultByName(testName, testResult, sheetName, overWriteResult);
    else
      return -1;
  }

  /**
   * overwrite the new test result colunm at the default location
   * (TEST_RESULT_COLUMN)
   * 
   * @param title the header title 
   * @param sheetName The sheet to find the test
   * @param sheetID   The sheetID which can get from the google sheet URL
   */
  public static void overwriteResultColHeader(String title, String sheetName, String sheetID) {
    Report foundReport = getReport(sheetID);
    if (foundReport != null)
      foundReport.overwriteResultColHeader(title, sheetName);
  }

  /**
   * insert the new test result colunm at the default location
   * (TEST_RESULT_COLUMN) (static)
   * 
   * @param sheetName The sheet to find the test
   * @param sheetID   The sheetID which can get from the google sheet URL
   * @return true if the column is inserted successful
   */
  public static boolean createNewResultCol(String sheetName, String sheetID) {
    Report foundReport = getReport(sheetID);
    if (foundReport != null)
      return foundReport.createNewResultCol(sheetName);
    else
      return false;
  }

  // FACTORY
  /**
   * store the Hash of Report by the SheetID, using for factory buffer
   */
  private static HashMap<String, Report> hashReports = new HashMap<>();

  /**
   * get Report by the sheetID
   * 
   * @param sheetID The sheetID which can get from the google sheet URL
   * @return Report by the input sheetID
   */
  private static Report getReport(String sheetID) {
    if (hashReports.containsKey(sheetID))
      return hashReports.get(sheetID);
    else {
      Report newReport = new Report(sheetID);
      hashReports.put(sheetID, newReport);
      return newReport;
    }
  }

  // ****** REPORT INSTANCE ******
  /**
   * keep the sheetID after constructing
   */
  String sheetID;

  /**
   * Constructor for Report
   * 
   * @param sheetID The sheetID which can get from the sheet URL
   */
  public Report(String sheetID) {
    this.sheetID = sheetID;
  }

  /**
   * the max Row which the Test Name is not empty
   */
  int maxRowIndex = 1;

  /**
   * find the test by name
   * 
   * @param testName            The test name to find
   * @param sheetName           The sheet to find the test
   * @param allowExistingResult TRUE: find Test which has result or not; FALSE:
   *                            find the test which does not has Result
   * @return The row index of the found test ; -1 if not found
   */
  public int findTestByName(String testName, String sheetName, boolean allowExistingResult) {
    if (StringUtils.isAnyEmpty(new String[] { testName, sheetName }))
      return -1;
    int blankCount = 0;
    for (int row10x = 0; row10x < 1000 && blankCount <= MAX_BLANK_ROW; row10x++) {
      // Get the Name Range
      List<List<Object>> values = Sheet.readRange(sheetName, TEST_NAME_COLUMN, TEST_NAME_START_ROW + row10x * 10,
          TEST_NAME_COLUMN, TEST_NAME_START_ROW + row10x * 10 + 10, sheetID);
      if (ListUtils.isEmpty(values))
        break; // break in blank sheet
      else {
        for (int rowIndex = 0; rowIndex < 10 && blankCount <= MAX_BLANK_ROW; rowIndex++) {
          String scanName = ListUtils.getValue(values, rowIndex, 0);
          if (!StringUtils.isEmpty(scanName)) {
            blankCount = 0;
            maxRowIndex = (TEST_NAME_START_ROW + row10x * 10 + rowIndex); // now it is current index
            if (testName.equalsIgnoreCase(scanName) && (allowExistingResult || StringUtils.isEmpty(ListUtils.getValue(
                Sheet.readRange(sheetName, TEST_RESULT_COLUMN, maxRowIndex, TEST_RESULT_COLUMN, maxRowIndex, sheetID),
                0, 0))))
              return maxRowIndex;
          } else {
            blankCount++;
          }
        }
      }
    }
    return -1;

  }

  /**
   * update the test result by name
   * 
   * @param testName        The test name to find
   * @param testResult        The test result to update
   * @param sheetName       The sheet to find the test
   * @param overWriteResult Is True, overwrite result, else the new row of test
   *                        will be created for the result
   * @return The row index of the found test and update result successful ; -1 if
   *         not found
   */
  public int updateTestResultByName(String testName, String testResult, String sheetName, boolean overWriteResult) {
    int foundTestRow = findTestByName(testName, sheetName, overWriteResult);
    if (foundTestRow >= 0)
      Sheet.setValue(testResult,
          sheetName + "!" + TEST_RESULT_COLUMN + foundTestRow + ":" + TEST_RESULT_COLUMN + foundTestRow, sheetID);
    else {
      if (maxRowIndex < TEST_NAME_START_ROW)
        maxRowIndex = TEST_NAME_START_ROW;
      foundTestRow = maxRowIndex + 1;

      Sheet.setValue(testName,
          sheetName + "!" + TEST_NAME_COLUMN + foundTestRow + ":" + TEST_NAME_COLUMN + foundTestRow, sheetID);
      Sheet.setValue(testResult,
          sheetName + "!" + TEST_RESULT_COLUMN + foundTestRow + ":" + TEST_RESULT_COLUMN + foundTestRow, sheetID);
    }
    return foundTestRow;
  }

  /**
   * insert the new test result colunm at the default location
   * (TEST_RESULT_COLUMN)
   * 
   * @param sheetName The sheet to find the test
   * @return true if the column is inserted successful
   */
  public boolean createNewResultCol(String sheetName) {
    // Insert a column
    Sheet.insertColumn(letterToColumn(TEST_RESULT_COLUMN), sheetID);

    // Add the column label
    LocalDateTime now = LocalDateTime.now();
    String colHeader = now.format(DATETIME_FORMATTER);
    Sheet.setValue(colHeader, sheetName + "!" + TEST_RESULT_COLUMN + (TEST_NAME_START_ROW - 1) + ":"
        + TEST_RESULT_COLUMN + (TEST_NAME_START_ROW - 1), sheetID);

    return false;
  }
  
  /**
   * overwrite the new test result colunm at the default location
   * (TEST_RESULT_COLUMN)
   * 
   * @param title the header title 
   * @param sheetName The sheet to find the test
   */
  public void overwriteResultColHeader(String title, String sheetName) {
    // Add the column label
    LocalDateTime now = LocalDateTime.now();
    String colHeader = title + "-" + now.format(DATETIME_FORMATTER);
    Sheet.setValue(colHeader, sheetName + "!" + TEST_RESULT_COLUMN + (TEST_NAME_START_ROW - 1) + ":"
        + TEST_RESULT_COLUMN + (TEST_NAME_START_ROW - 1), sheetID);
  }



  // Follow help from: 
  static public String columnToLetter(int column)
  {
    int temp;
    String letter = "";
    while (column > 0)
    {
      temp = (column) % 26;
      letter = (char)(temp + 65) + letter;
      column = (column - temp - 1) / 26;
    }
    return letter;
  }

  static public int letterToColumn(String letter)
  {
    int column = 0;
    int length = letter.length();
    for (int i = 0; i < length; i++)
    {
      column += ((int)letter.charAt(i) - 64) * Math.pow(26, length - i - 1);
    }
    return column - 1;
  }
}
