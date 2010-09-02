/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework;

import org.jminor.common.model.Util;
import org.jminor.common.model.formats.DateFormats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Properties;

/**
 * Settings used throughout the framework.
 * These settings are used during initialization and should be set before
 * the application is initialized, before EntityApplicationPanel.startApplication is called.
 * EntityApplicationPanel.initializeSettings() is a convenience method for this purpose,
 * override and use it to set configuration properties.
 */
public final class Configuration {

  private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

  private Configuration() {}

  /**
   * Indicates a local database connection
   * @see #CLIENT_CONNECTION_TYPE
   */
  public static final String CONNECTION_TYPE_LOCAL = "local";

  /**
   * Indicates a remote database connection
   * @see #CLIENT_CONNECTION_TYPE
   */
  public static final String CONNECTION_TYPE_REMOTE = "remote";

  /**
   * Specifies whether the client should connect locally or remotely,
   * accepted values: local, remote<br>
   * Value type: String<br>
   * Default value: local
   * @see #CONNECTION_TYPE_LOCAL
   * @see #CONNECTION_TYPE_REMOTE
   */
  public static final String CLIENT_CONNECTION_TYPE = "jminor.client.connectionType";

  /**
   * The report path used for the default report generation,
   * either file or http based
   */
  public static final String REPORT_PATH = "jminor.report.path";

  /**
   * Default username for the login panel
   */
  public static final String DEFAULT_USERNAME = "jminor.client.defaultUser";

  /**
   * The host on which to locate the server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final String SERVER_HOST_NAME = "jminor.server.hostname";

  /**
   * If specified, the client will look for a server running on this port
   */
  public static final String SERVER_PORT = "jminor.server.port";

  /**
   * The port on which the server should export the remote admin interface
   */
  public static final String SERVER_ADMIN_PORT = "jminor.server.admin.port";

  /**
   * The port on which the server should export the remote database connections
   */
  public static final String SERVER_DB_PORT = "jminor.server.db.port";

  /**
   * The initial connection logging status on the server, either true (on) or false (off)<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String SERVER_CLIENT_LOGGING_ENABLED = "jminor.server.clientLoggingEnabled";

  /**
   * Specifies the size of the (circular) log the server keeps in memory for each connected client<br>
   * Value type: Integer<br>
   * Default value: 40
   */
  public static final String SERVER_CONNECTION_LOG_SIZE = "jminor.server.clientLogSize";

  /**
   * Specifies whether the server should establish connections using a secure sockets layer, true (on) or false (off)<br>
   * ValueType: Boolean<br>
   * Default value: true
   */
  public static final String SERVER_CONNECTION_SSL_ENABLED = "jminor.server.connection.sslEnabled";

  /**
   * Specifies a comma separated list of usernames for which to create connection pools on startup
   */
  public static final String SERVER_CONNECTION_POOLING_INITIAL = "jminor.server.pooling.initial";

  /**
   * Specifies a comma separated list of jar files to use when loading domain model classes
   * @see #SERVER_DOMAIN_MODEL_CLASSES
   */
  public static final String SERVER_DOMAIN_MODEL_JARS = "jminor.server.domain.jars";

  /**
   * Specifies a comma separated list of domain model classnames, these classes must be
   * available on the server classpath or the jars containing these classes specified
   * via the SERVER_DOMAIN_MODEL_JARS property
   * @see #SERVER_DOMAIN_MODEL_JARS
   */
  public static final String SERVER_DOMAIN_MODEL_CLASSES = "jminor.server.domain.classes";

  /**
   * Specifies the initial think time setting for the load test client
   * (max think time = thinktime, min think time = max think time / 2)<br>
   * Value type: Integer<br>
   * Default value: 2000
   */
  public static final String LOAD_TEST_THINKTIME = "jminor.loadtest.thinktime";

  /**
   * Specifies the number which the max think time is multiplied with when initializing the clients<br>
   * Value type: Integer<br>
   * Default value: 2
   */
  public static final String LOAD_TEST_LOGIN_DELAY = "jminor.loadtest.logindelay";

  /**
   * Specifies the initial client batch size<br>
   * Value type: Integer<br>
   * Default value: 10
   */
  public static final String LOAD_TEST_BATCH_SIZE = "jminor.loadtest.batchsize";

  /**
   * Indicates whether a load test runner should use a remote load test server<br>
   * Value type: Boolean<br>
   * Default value: false
   * @see #USE_FOCUS_ACTIVATION
   */
  public static final String LOAD_TEST_REMOTE = "jminor.loadtest.remote";

  /**
   * The date format pattern to use when showing timestamp values in tables and when
   * creating default timestamp input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy HH:mm
   */
  public static final String DEFAULT_TIMESTAMP_FORMAT = "jminor.client.defaultTimestampFormat";

  /**
   * The date format pattern to use when showing date values in tables and when
   * creating default date input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy
   */
  public static final String DEFAULT_DATE_FORMAT = "jminor.client.defaultDateFormat";

  /**
   * Indicates whether all entity panels should be enabled and receiving input by default<br>
   * Value type: Boolean<br>
   * Default value: false
   * @see #USE_FOCUS_ACTIVATION
   */
  public static final String ALL_PANELS_ACTIVE = "jminor.client.allPanelsActive";

  /**
   * Indicates whether entity panels containing detail panels should by default be laid out in a compact manner<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String COMPACT_ENTITY_PANEL_LAYOUT = "jminor.client.compactEntityPanelLayout";

  /**
   * Indicates whether keyboard navigation will be enabled<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String USE_KEYBOARD_NAVIGATION = "jminor.client.useKeyboardNavigation";

  /**
   * Indicates whether entity panels should be activated when the panel receives focus<br>
   * Value type: Boolean<br>
   * Default value: true
   * @see #ALL_PANELS_ACTIVE
   */
  public static final String USE_FOCUS_ACTIVATION = "jminor.client.useFocusActivation";

  /**
   * Specifies the default table column resize mode for tables in the application<br>
   * Value type: Integer (JTable.AUTO_RESIZE_*)<br>
   * Default value: JTable.AUTO_RESIZE_OFF
   */
  public static final String TABLE_AUTO_RESIZE_MODE = "jminor.client.tableautoResizeMode";

  /**
   * Indicates whether the application should ask for confirmation when exiting<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String CONFIRM_EXIT = "jminor.client.confirmExit";

  /**
   * Specifies whether the framework should output verbose debug output regarding property changes in entities<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String PROPERTY_DEBUG_OUTPUT = "jminor.client.propertyDebugOutput";

  /**
   * Specifies the tab placement<br>
   * Value type: Integer (JTabbedPane.TOP, JTabbedPane.BOTTOM, JTabbedPane.LEFT, JTabbedPane.RIGHT)<br>
   * Default value: JTabbedPane.TOP
   */
  public static final String TAB_PLACEMENT = "jminor.client.tabPlacement";

  /**
   * Specifies whether or not columns can be rearranged in tables<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String ALLOW_COLUMN_REORDERING = "jminor.client.allowColumnReordering";

  /**
   * Specifies whether the action buttons (Save, update, delete, clear, refresh) should be on a toolbar<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String TOOLBAR_BUTTONS = "jminor.client.toolbarButtons";

  /**
   * Specifies whether foreign key values should persist when the UI is cleared or be reset to null<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String PERSIST_FOREIGN_KEY_VALUES = "jminor.client.persistForeignKeyValues";

  /**
   * Specifies a string to prepend to the username field in the login dialog<br>
   * Value type: String<br>
   * Default value: [empty string]
   */
  public static final String USERNAME_PREFIX = "jminor.client.usernamePrefix";

  /**
   * Specifies whether user authentication is required<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String AUTHENTICATION_REQUIRED = "jminor.client.authenticationRequired";

  /**
   * Specifies whether a startup dialog should be shown<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String SHOW_STARTUP_DIALOG = "jminor.client.showStartupDialog";

  /**
   * Specifies whether focus should be transferred from components on enter,
   * this does not work for editable combo boxes, combo boxes with the
   * maximum match functionality enabled or text areas<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String TRANSFER_FOCUS_ON_ENTER = "jminor.client.transferFocusOnEnter";

  /**
   * Specifies whether optimistic locking should be performed, that is, if entities should
   * be selected for update and checked for modification before being updated<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String USE_OPTIMISTIC_LOCKING = "jminor.db.useOptimisticLocking";

  /**
   * Specifies the value used to denote a boolean false in the database<br>
   * Value type: Any Object<br>
   * Default value: 0
   */
  public static final String SQL_BOOLEAN_VALUE_FALSE = "jminor.client.sqlBooleanValueFalse";

  /**
   * Specifies the value used to denote a boolean true in the database<br>
   * Value type: Any Object<br>
   * Default value: 1
   */
  public static final String SQL_BOOLEAN_VALUE_TRUE = "jminor.client.sqlBooleanValueTrue";

  /**
   * Specifies if EntityPanels opened via the <code>EntityApplicationPanel.showEntityPanelDialog</code> method
   * should be persisted, or kept in memory, when the dialog is closed.<br>
   * Value type: Boolean<br>
   * Default value: false
   * @see org.jminor.framework.client.ui.EntityApplicationPanel#showEntityPanelDialog(org.jminor.framework.client.ui.EntityPanelProvider)
   */
  public static final String PERSIST_ENTITY_PANELS = "jminor.client.persistEntityPanels";

  /**
   * Specifies the default search panel state, whether it should be visible or not<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String DEFAULT_SEARCH_PANEL_STATE = "jminor.client.defaultSearchPanelState";

  /**
   * Specifies the prefix used when exporting/looking up the JMinor server<br>
   * Value type: String<br>
   * Default value: JMinor Server
   */
  public static final String SERVER_NAME_PREFIX = "jminor.server.namePrefix";

  /**
   * Specifies the wildcard character used by the framework<br>
   * Value type: String<br>
   * Default value: %
   */
  public static final String WILDCARD_CHARACTER = "jminor.wildcardCharacter";

  /**
   * Specifies whether or not to use number format grouping in table views,
   * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale.
   * This can be overridden on Property basis via Property.setUseNumberFormatGrouping()<br>
   * Value type: Boolean<br>
   * Default value: true
   * @see org.jminor.framework.domain.Property#setUseNumberFormatGrouping(boolean)
   */
  public static final String USE_NUMBER_FORMAT_GROUPING = "jminor.client.useNumberFormatGrouping";

  /**
   * Specifies the class providing remote db connections<br>
   * Value type: String (the name of a class implementing org.jminor.framework.db.provider.EntityDbProvider)<br>
   * Default value: org.jminor.framework.server.EntityDbRemoteProvider
   */
  public static final String REMOTE_CONNECTION_PROVIDER = "jminor.client.remoteConnectionProvider";

  /**
   * Specifies the class used for serializing entity instances.<br>
   * Value type: String, the name of the class implementing org.jminor.common.model.Serializer<Entity><br>
   * Default value: none
   */
  public static final String ENTITY_SERIALIZER_CLASS = "jminor.serialization.entitySerializerClass";

  /**
   * Specifies the class used for deserializing entity instances.<br>
   * Value type: String, the name of the class implementing org.jminor.common.model.Deserializer<Entity><br>
   * Default value: none
   */
  public static final String ENTITY_DESERIALIZER_CLASS = "jminor.serialization.entityDeserializerClass";

  /**
   * Specifies the class providing local db connections<br>
   * Value type: String (the name of a class implementing org.jminor.framework.db.provider.EntityDbProvider)<br>
   * Default value: org.jminor.framework.db.provider.EntityDbLocalProvider
   */
  public static final String LOCAL_CONNECTION_PROVIDER = "jminor.client.localConnectionProvider";

  /**
   * Specifies the value used by default to represent a null value in combo box models.
   * Using the value null indicates that no null value item should be used.<br>
   * Value type: String<br>
   * Default value: -
   */
  public static final String DEFAULT_COMBO_BOX_NULL_VALUE_ITEM = "jminor.client.defaultComboBoxNullValueItem";

  /**
   * Specifies the color to use as background in input fields containing invalid values<br>
   * Value type: Color<br>
   * Default value: Color.LIGHT_GRAY
   */
  public static final String INVALID_VALUE_BACKGROUND_COLOR = "jminor.client.invalidValueBackgroundColor";

  /**
   * Specifies whether the client layer should perform null validation on entities
   * before update/insert actions are performed<br>
   * Value type: Boolean<br>
   * Default value: true
   * @see org.jminor.framework.domain.Property#setNullable(boolean)
   */
  public static final String PERFORM_NULL_VALIDATION = "jminor.client.performNullValidation";

  /**
   * Specifies the default horizontal alignment used in labels<br>
   * Value type: Integer (JLabel.LEFT, JLabel.RIGHT, JLabel.CENTER)<br>
   * Default value: JLabel.LEFT
   * @see JLabel#LEFT
   * @see JLabel#RIGHT
   * @see JLabel#CENTER
   */
  public static final String DEFAULT_LABEL_TEXT_ALIGNMENT = "jminor.client.defaultLabelTextAlignment";

  /**
   * Specifies the default foreign key fetch depth<br>
   * Value type: Integer<br>
   * Default value: 1
   */
  public static final String DEFAULT_FOREIGN_KEY_FETCH_DEPTH = "jminor.db.foreignKeyFetchDepth";

  /**
   * Specifies whether the foreign key value graph should be fully populated instead of
   * being limited by the foreign key fetch depth setting.
   * Value type: Boolean<br>
   * Default value: true<br>
   */
  public static final String LIMIT_FOREIGN_KEY_FETCH_DEPTH = "jminor.db.limitForeignKeyFetchDepth";

  /**
   * Specifies the default look and feel classname
   * Value type: String<br>
   * Default value: UIManager.getSystemLookAndFeelClassName()
   */
  public static final String DEFAULT_LOOK_AND_FEEL_CLASSNAME = "jminor.client.defaultLookAndFeelClassName";

  /**
   * Specifies whether entity models should be automatically created if not specified.
   * Value type: Boolean<br>
   * Default value: true<br>
   */
  public static final String AUTO_CREATE_ENTITY_MODELS = "jminor.client.autoCreateEntityModels";

  /**
   * Specifies whether search panels should be visible by default in table panels.
   * Value type: Boolean<br>
   * Default value: false<br>
   */
  public static final String SEARCH_PANELS_VISIBLE = "jminor.client.searchPanelsVisible";

  public static final String JAVAX_NET_NET_TRUSTSTORE = "javax.net.ssl.trustStore";
  private static final Properties PROPERTIES = new Properties();
  private static final int INPUT_BUFFER_SIZE = 8192;

  static {
    Util.parseConfigurationFile();
    //default settings
    PROPERTIES.put(LOAD_TEST_THINKTIME, 2000);
    PROPERTIES.put(LOAD_TEST_BATCH_SIZE, 10);
    PROPERTIES.put(LOAD_TEST_LOGIN_DELAY, 2);
    PROPERTIES.put(LOAD_TEST_REMOTE, false);
    PROPERTIES.put(CLIENT_CONNECTION_TYPE, CONNECTION_TYPE_LOCAL);
    PROPERTIES.put(SERVER_CLIENT_LOGGING_ENABLED, true);
    PROPERTIES.put(SERVER_CONNECTION_LOG_SIZE, 40);
    PROPERTIES.put(SERVER_CONNECTION_SSL_ENABLED, true);
    PROPERTIES.put(SERVER_HOST_NAME, "localhost");
    PROPERTIES.put(DEFAULT_TIMESTAMP_FORMAT, "dd-MM-yyyy HH:mm");
    PROPERTIES.put(DEFAULT_DATE_FORMAT, "dd-MM-yyyy");
    PROPERTIES.put(ALL_PANELS_ACTIVE, false);
    PROPERTIES.put(COMPACT_ENTITY_PANEL_LAYOUT, false);
    PROPERTIES.put(USE_KEYBOARD_NAVIGATION, true);
    PROPERTIES.put(USE_FOCUS_ACTIVATION, true);
    PROPERTIES.put(TABLE_AUTO_RESIZE_MODE, JTable.AUTO_RESIZE_OFF);
    PROPERTIES.put(CONFIRM_EXIT, false);
    PROPERTIES.put(PROPERTY_DEBUG_OUTPUT, false);
    PROPERTIES.put(TAB_PLACEMENT, JTabbedPane.TOP);
    PROPERTIES.put(TOOLBAR_BUTTONS, false);
    PROPERTIES.put(PERSIST_FOREIGN_KEY_VALUES, true);
    PROPERTIES.put(USERNAME_PREFIX, "");
    PROPERTIES.put(AUTHENTICATION_REQUIRED, true);
    PROPERTIES.put(SHOW_STARTUP_DIALOG, true);
    PROPERTIES.put(TRANSFER_FOCUS_ON_ENTER, true);
    PROPERTIES.put(USE_OPTIMISTIC_LOCKING, false);
    PROPERTIES.put(SQL_BOOLEAN_VALUE_FALSE, 0);
    PROPERTIES.put(SQL_BOOLEAN_VALUE_TRUE, 1);
    PROPERTIES.put(PERSIST_ENTITY_PANELS, false);
    PROPERTIES.put(DEFAULT_SEARCH_PANEL_STATE, false);
    PROPERTIES.put(SERVER_NAME_PREFIX, "JMinor Server");
    PROPERTIES.put(WILDCARD_CHARACTER, "%");
    PROPERTIES.put(USE_NUMBER_FORMAT_GROUPING, true);
    PROPERTIES.put(REMOTE_CONNECTION_PROVIDER, "org.jminor.framework.server.provider.EntityDbRemoteProvider");
    PROPERTIES.put(LOCAL_CONNECTION_PROVIDER, "org.jminor.framework.db.provider.EntityDbLocalProvider");
    PROPERTIES.put(DEFAULT_COMBO_BOX_NULL_VALUE_ITEM, "-");
    PROPERTIES.put(INVALID_VALUE_BACKGROUND_COLOR, Color.LIGHT_GRAY);
    PROPERTIES.put(PERFORM_NULL_VALIDATION, true);
    PROPERTIES.put(DEFAULT_LABEL_TEXT_ALIGNMENT, JLabel.LEFT);
    PROPERTIES.put(ALLOW_COLUMN_REORDERING, true);
    PROPERTIES.put(DEFAULT_FOREIGN_KEY_FETCH_DEPTH, 1);
    PROPERTIES.put(LIMIT_FOREIGN_KEY_FETCH_DEPTH, true);
    PROPERTIES.put(DEFAULT_LOOK_AND_FEEL_CLASSNAME, UIManager.getSystemLookAndFeelClassName());
    PROPERTIES.put(AUTO_CREATE_ENTITY_MODELS, true);
    parseSystemSettings();
  }

  private static void parseSystemSettings() {
    parseBooleanSetting(ALL_PANELS_ACTIVE);
    parseBooleanSetting(ALLOW_COLUMN_REORDERING);
    parseBooleanSetting(AUTHENTICATION_REQUIRED);
    parseStringSetting(CLIENT_CONNECTION_TYPE);
    parseBooleanSetting(COMPACT_ENTITY_PANEL_LAYOUT);
    parseBooleanSetting(CONFIRM_EXIT);
    parseStringSetting(DEFAULT_COMBO_BOX_NULL_VALUE_ITEM);
    parseStringSetting(DEFAULT_DATE_FORMAT);
    parseIntegerSetting(DEFAULT_FOREIGN_KEY_FETCH_DEPTH);
    parseIntegerSetting(DEFAULT_LABEL_TEXT_ALIGNMENT);
    parseBooleanSetting(DEFAULT_SEARCH_PANEL_STATE);
    parseStringSetting(DEFAULT_USERNAME);
    parseStringSetting(DEFAULT_TIMESTAMP_FORMAT);
    parseBooleanSetting(LIMIT_FOREIGN_KEY_FETCH_DEPTH);
    parseIntegerSetting(LOAD_TEST_THINKTIME);
    parseIntegerSetting(LOAD_TEST_BATCH_SIZE);
    parseIntegerSetting(LOAD_TEST_LOGIN_DELAY);
    parseBooleanSetting(LOAD_TEST_REMOTE);
    parseStringSetting(LOCAL_CONNECTION_PROVIDER);
    parseBooleanSetting(PERFORM_NULL_VALIDATION);
    parseBooleanSetting(PERSIST_ENTITY_PANELS);
    parseBooleanSetting(PERSIST_FOREIGN_KEY_VALUES);
    parseBooleanSetting(PROPERTY_DEBUG_OUTPUT);
    parseStringSetting(REMOTE_CONNECTION_PROVIDER);
    parseStringSetting(SERVER_ADMIN_PORT);
    parseStringSetting(SERVER_DB_PORT);
    parseStringSetting(SERVER_HOST_NAME);
    parseStringSetting(REPORT_PATH);
    parseStringSetting(SERVER_PORT);
    parseBooleanSetting(SERVER_CLIENT_LOGGING_ENABLED);
    parseIntegerSetting(SERVER_CONNECTION_LOG_SIZE);
    parseStringSetting(SERVER_CONNECTION_POOLING_INITIAL);
    parseStringSetting(SERVER_DOMAIN_MODEL_CLASSES);
    parseStringSetting(SERVER_DOMAIN_MODEL_JARS);
    parseStringSetting(SERVER_NAME_PREFIX);
    parseBooleanSetting(SERVER_CONNECTION_SSL_ENABLED);
    parseBooleanSetting(SHOW_STARTUP_DIALOG);
    parseIntegerSetting(TAB_PLACEMENT);
    parseIntegerSetting(TABLE_AUTO_RESIZE_MODE);
    parseBooleanSetting(TOOLBAR_BUTTONS);
    parseBooleanSetting(TRANSFER_FOCUS_ON_ENTER);
    parseBooleanSetting(USE_FOCUS_ACTIVATION);
    parseBooleanSetting(USE_KEYBOARD_NAVIGATION);
    parseBooleanSetting(USE_NUMBER_FORMAT_GROUPING);
    parseBooleanSetting(USE_OPTIMISTIC_LOCKING);
    parseStringSetting(USERNAME_PREFIX);
    parseStringSetting(WILDCARD_CHARACTER);
    parseStringSetting(DEFAULT_LOOK_AND_FEEL_CLASSNAME);
    parseBooleanSetting(AUTO_CREATE_ENTITY_MODELS);

    parseStringSetting(JAVAX_NET_NET_TRUSTSTORE);
  }

  private static void parseIntegerSetting(final String setting) {
    final String value = System.getProperty(setting);
    if (value != null) {
      PROPERTIES.put(setting, Integer.parseInt(value));
    }
  }

  private static void parseBooleanSetting(final String setting) {
    final String value = System.getProperty(setting);
    if (value != null) {
      PROPERTIES.put(setting, value.equalsIgnoreCase("true"));
    }
  }

  private static void parseStringSetting(final String setting) {
    final String value = System.getProperty(setting);
    if (value != null) {
      PROPERTIES.put(setting, value);
    }
  }

  public static void setValue(final String key, final Object value) {
    PROPERTIES.put(key, value);
    System.setProperty(key, value.toString());
  }

  public static void clearValue(final String key) {
    PROPERTIES.remove(key);
    System.clearProperty(key);
  }

  public static Object getValue(final String key) {
    return PROPERTIES.get(key);
  }

  public static Integer getIntValue(final String key) {
    return (Integer) getValue(key);
  }

  public static Boolean getBooleanValue(final String key) {
    return (Boolean) getValue(key);
  }

  public static String getStringValue(final String key) {
    return (String) getValue(key);
  }

  /**
   * @param applicationIdentifier the application identifier name
   * @return the default username
   */
  public static String getDefaultUsername(final String applicationIdentifier) {
    final String preferredUserName = Util.getDefaultUserName(applicationIdentifier,
            getValue(USERNAME_PREFIX) + System.getProperty("user.name"));
    return System.getProperty(DEFAULT_USERNAME, preferredUserName);
  }

  /**
   * @return A non-lenient SimpleDateFormat based on Configuration.DEFAULT_DATE_FORMAT
   * @see Configuration#DEFAULT_DATE_FORMAT
   */
  public static SimpleDateFormat getDefaultDateFormat() {
    return DateFormats.getDateFormat((String) getValue(DEFAULT_DATE_FORMAT));
  }

  /**
   * @return A non-lenient SimpleDateFormat based on Configuration.DEFAULT_TIMESTAMP_FORMAT
   * @see org.jminor.framework.Configuration#DEFAULT_TIMESTAMP_FORMAT
   */
  public static SimpleDateFormat getDefaultTimestampFormat() {
    return DateFormats.getDateFormat((String) getValue(DEFAULT_TIMESTAMP_FORMAT));
  }

  public static String getReportPath() {
    final String path = getStringValue(REPORT_PATH);
    if (Util.nullOrEmpty(path)) {
      throw new IllegalArgumentException("jminor.report.path property is not speficied");
    }

    return path;
  }

  /**
   * @return true if a entity serializer is specified and available on the classpath
   */
  public static boolean entitySerializerAvailable() {
    final String serializerClass = getStringValue(ENTITY_SERIALIZER_CLASS);
    return serializerClass != null && Util.onClasspath(serializerClass);
  }

  /**
   * @return true if a entity deserializer is specified and available on the classpath
   */
  public static boolean entityDeserializerAvailable() {
    final String deserializerClass = getStringValue(ENTITY_DESERIALIZER_CLASS);
    return deserializerClass != null && Util.onClasspath(deserializerClass);
  }

  /**
   * Resolves the "javax.net.ssl.trustStore" to a temporary file, assigning it to the property
   * @param temporaryFileName the temp filename
   */
  public static void resolveTruststoreProperty(final String temporaryFileName) {
    final String value = getStringValue(JAVAX_NET_NET_TRUSTSTORE);
    if (value == null || value.isEmpty()) {
      LOG.debug("resolveFileProperty: " + JAVAX_NET_NET_TRUSTSTORE + " is empty");
      return;
    }
    FileOutputStream out = null;
    InputStream in = null;
    try {
      final ClassLoader loader = Util.class.getClassLoader();
      in = loader.getResourceAsStream(value);
      if (in == null) {
        LOG.debug("resolveFileProperty: " + value + " not found on classpath");
        return;
      }
      final File file = File.createTempFile(temporaryFileName, "tmp");
      file.deleteOnExit();
      out = new FileOutputStream(file);
      final byte[] buf = new byte[INPUT_BUFFER_SIZE];
      int br = in.read(buf);
      while (br > 0) {
        out.write(buf, 0, br);
        br = in.read(buf);
      }
      LOG.debug("resolveFileProperty: " + JAVAX_NET_NET_TRUSTSTORE + " -> " + file.toString());
      setValue(JAVAX_NET_NET_TRUSTSTORE, file.toString());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      Util.closeSilently(out, in);
    }
  }
}
