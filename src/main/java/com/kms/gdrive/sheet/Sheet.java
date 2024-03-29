package com.kms.gdrive.sheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.kms.util.StringUtils;

public class Sheet {
  public static final String CLASSNAME = "com.kms.gdrive.sheet.Sheet";
  private static final String APPLICATION_NAME = "KMS Google Sheet API";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static String credentialsDirectory = "gconf";
  private static String credentialsFile = "gsheet-auth.json"; // As resource
  private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS, SheetsScopes.DRIVE);

  public static void setCredentialDir (String credentialsDirectory, String credentialsFile) {
    if (!StringUtils.isEmpty(credentialsDirectory))
      Sheet.credentialsDirectory = credentialsDirectory;
    if (!StringUtils.isEmpty(credentialsFile))
      Sheet.credentialsFile = credentialsFile;
  }

  /**
   * Creates an authorized Credential object.
   * 
   * @param httpTransport The network HTTP Transport.
   * @return An authorized Credential object.
   */
  private static Credential getCredentials(final NetHttpTransport httpTransport) {
    // Load google report configuration directory path / env
    File checkExists = new File(credentialsDirectory + File.separator + credentialsFile);

    boolean resourceMode = true;
    if (checkExists.exists() && checkExists.isFile())
      resourceMode = false;
    
    try (InputStreamReader credentialReader = resourceMode?
      new InputStreamReader(Sheet.class.getResourceAsStream(File.separator+credentialsFile)):
      new InputStreamReader((new FileInputStream(credentialsDirectory + File.separator + credentialsFile)));) {
      File tokenDirFileObj = resourceMode?
      new File(Sheet.class.getResource(File.separator).getFile()):
      new File(credentialsDirectory);
      GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, credentialReader);

      // Build flow and trigger user authorization request.
      GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
          clientSecrets, SCOPES).setDataStoreFactory(new FileDataStoreFactory(tokenDirFileObj))
              .setAccessType("offline").build();
      LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
      return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    catch (Exception e) {
      Logger.getLogger(CLASSNAME).log(Level.WARNING, e.getMessage());
      return null;
    }
  }

  /**
   * the the range by sheetID
   * 
   * @param sheetName The sheet to find the test
   * @param startCol  start Column to get range
   * @param startRow  start Row to get range
   * @param endCol    end Column to get range
   * @param endRow    end Row to get range
   * @param sheetID   The sheetID which can get from the google sheet URL
   * @return the range
   */
  public static List<List<Object>> readRange(String sheetName, String startCol, int startRow, String endCol, int endRow,
      String sheetID) {
    Sheet foundSheet = getSheet(sheetID);
    return (foundSheet == null)?Collections.emptyList():foundSheet.readRange(sheetName, startCol, startRow, endCol, endRow);
  }

  /**
   * setValue get the value from range (static)
   * 
   * @param value      To write to as String
   * @param writeRange The range to write
   * @param sheetID    The sheetID which can get from the google sheet URL
   * @return true is successful
   */
  public static boolean setValue(String value, String writeRange, String sheetID) {
    Sheet foundSheet = getSheet(sheetID);
    return (foundSheet != null)&&foundSheet.setValue(value, writeRange);
  }

  /**
   * setValues set the values to range
   * 
   * @param values     To write to as copy from read range
   * @param sheetName The sheet to find the test
   * @param startCol  start Column to get range
   * @param startRow  start Row to get range
   * @param endCol    end Column to get range
   * @param endRow    end Row to get range
   * @param sheetID    The sheetID which can get from the google sheet URL
   * @return true is successful
   */
  public static boolean setValues(List<List<Object>> values, String sheetName, String startCol, int startRow, String endCol, int endRow, String sheetID) {
    Sheet foundSheet = getSheet(sheetID);
    return (foundSheet != null)&&foundSheet.setValues(values, sheetName, startCol, startRow, endCol, endRow);
  }

  /**
   * setValues set the values to range
   * 
   * @param values     To write to as copy from read range
   * @param writeRange The range to write
   * @param sheetID    The sheetID which can get from the google sheet URL
   * @return true is successful
   */
  public static boolean setValues(List<List<Object>> values, String writeRange, String sheetID) {
    Sheet foundSheet = getSheet(sheetID);
    return (foundSheet != null)&&foundSheet.setValues(values, writeRange);
  }

  /**
   * insert a column at the index (static)
   * 
   * @param columnIndex The index of the column to insert
   * @param sheetID     The sheetID which can get from the google sheet URL
   * @param sheetName   The sheet Name to insert result col
   * @return true is successful
   */
  public static boolean insertColumn(int columnIndex, String sheetName, String sheetID) {
    Sheet foundSheet = getSheet(sheetID);
    return (foundSheet != null)&&foundSheet.insertColumn(columnIndex, sheetName);
  }

  // MANAGE Sheet object by Factory
  /**
   * store the Hash of Sheet by the SheetID, using for factory buffer
   */
  private static HashMap<String, Sheet> hashSheets = new HashMap<>();

  /**
   * get Sheet by the sheetID
   * 
   * @param sheetID The sheetID which can get from the google sheet URL
   * @return Sheet by the input sheetID
   */
  private static Sheet getSheet(String sheetID) {
    if (hashSheets.containsKey(sheetID))
      return hashSheets.get(sheetID);
    else {
      Sheet newSheet = new Sheet(sheetID);
      hashSheets.put(sheetID, newSheet);
      return newSheet;
    }
  }

  // OBJECT declaration
  /**
   * has to be constructed by constructor for service as Sheets
   */
  Sheets service = null;

  /**
   * keep the sheetID after constructing
   */
  String sheetID = "";

  /**
   * Constructor for Sheet
   * 
   * @param sheetID The sheetID which can get from the sheet URL
   */
  public Sheet(String sheetID) {
    try {
      this.sheetID = sheetID;
      NetHttpTransport.Builder transportBuilder = new NetHttpTransport.Builder();
      NetHttpTransport httpTransport = transportBuilder.build();
      transportBuilder.doNotValidateCertificate();
      service = new Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
          .setApplicationName(APPLICATION_NAME).build();
    } catch (Exception e) {
      Logger.getLogger(CLASSNAME).log(Level.WARNING, e.getMessage());
    }
  }

  /**
   * read the range from the input
   * 
   * @param sheetName The sheet to find the test
   * @param startCol  start Column to get range
   * @param startRow  start Row to get range
   * @param endCol    end Column to get range
   * @param endRow    end Row to get range
   * @return the range
   */
  public List<List<Object>> readRange(String sheetName, String startCol, int startRow, String endCol, int endRow) {
    if (service != null)
      try {
        final String readRange = sheetName + "!" + startCol + startRow + ":" + endCol + endRow;
        com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Get getRequest = service.spreadsheets().values().get(sheetID, readRange);
        getRequest.setValueRenderOption("FORMULA");
        ValueRange valueRange = getRequest.execute();
        valueRange.set("valueRenderOption", "FORMULA");
        return valueRange.getValues();
      } catch (IOException e) {
        Logger.getLogger(CLASSNAME).log(Level.WARNING, e.getMessage());
      }
    return Collections.emptyList();
  }

  static final String INPUT_OPT_USER_ENTERED = "USER_ENTERED";
  /**
   * setValue set the value to range
   * 
   * @param value      To write to as String
   * @param writeRange The range to write
   * @return true is successful
   */
  public boolean setValue(String value, String writeRange) {
    if (service != null)
      try {
        // Create value list range
        ValueRange updateValues = new ValueRange();
        updateValues.setValues(Arrays.asList(Arrays.asList((Object) value)));
        service.spreadsheets().values().update(sheetID, writeRange, updateValues).setValueInputOption(INPUT_OPT_USER_ENTERED)
            .execute();
        return true;
      } catch (IOException e) {
        Logger.getLogger(CLASSNAME).log(Level.WARNING, e.getMessage());
      }
    return false;
  }

  /**
   * setValue set the values to range
   * 
   * @param values     To write to as copy from read range
   * @param sheetName The sheet to find the test
   * @param startCol  start Column to get range
   * @param startRow  start Row to get range
   * @param endCol    end Column to get range
   * @param endRow    end Row to get range
   * @return true is successful
   */
  public boolean setValues(List<List<Object>> values, String sheetName, String startCol, int startRow, String endCol, int endRow) {
    if (service != null)
      try {
        final String writeRange = sheetName + "!" + startCol + startRow + ":" + endCol + endRow;
        // Create value list range
        ValueRange updateValues = new ValueRange();
        updateValues.setValues(values);
        service.spreadsheets().values().update(sheetID, writeRange, updateValues).setValueInputOption(INPUT_OPT_USER_ENTERED)
            .execute();
        return true;
      } catch (IOException e) {
        Logger.getLogger(CLASSNAME).log(Level.WARNING, e.getMessage());
      }
    return false;
  }

  /**
   * setValue set the values to range
   * 
   * @param values     To write to as copy from read range
   * @param writeRange The range to write
   * @return true is successful
   */
  public boolean setValues(List<List<Object>> values, String writeRange) {
    if (service != null)
      try {
        // Create value list range
        ValueRange updateValues = new ValueRange();
        updateValues.setValues(values);
        service.spreadsheets().values().update(sheetID, writeRange, updateValues).setValueInputOption(INPUT_OPT_USER_ENTERED)
            .execute();
        return true;
      } catch (IOException e) {
        Logger.getLogger(CLASSNAME).log(Level.WARNING, e.getMessage());
      }
    return false;
  }

  /**
   * insert a column at the index
   * 
   * @param columnIndex The index of the column to insert
   * @param sheetName   The sheet Name to insert result col
   * @return true is successful
   */
  public boolean insertColumn(int columnIndex, String sheetName) {
    Spreadsheet spreadsheet = null;
    if (service != null)
      // Get sheet id
      try {
        spreadsheet = service.spreadsheets().get(sheetID).execute();

        Integer isheetID = -1;
        
        for (int iSheetIndex = 0 ; iSheetIndex < spreadsheet.getSheets().size() ; iSheetIndex ++)
          if (spreadsheet.getSheets().get(iSheetIndex).getProperties().getTitle().equalsIgnoreCase(sheetName)) {
            isheetID = spreadsheet.getSheets().get(iSheetIndex).getProperties().getSheetId();
            break;
          }

        if (isheetID >= 0) {
          // Set column insert
          DimensionRange dimentionRange = new DimensionRange();
          dimentionRange.setStartIndex(columnIndex);
          dimentionRange.setEndIndex(columnIndex + 1);
          dimentionRange.setSheetId(isheetID);
          dimentionRange.setDimension("COLUMNS");

          InsertDimensionRequest insertCol = new InsertDimensionRequest();
          insertCol.setRange(dimentionRange);

          // Execute to insert column
          BatchUpdateSpreadsheetRequest r = new BatchUpdateSpreadsheetRequest()
              .setRequests(Arrays.asList(new Request().setInsertDimension(insertCol)));
          service.spreadsheets().batchUpdate(sheetID, r).execute();
          return true;
        }
      } catch (IOException e) {
        Logger.getLogger(CLASSNAME).log(Level.WARNING, e.getMessage());
      }
    return false;
  }
}
