/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework;

import org.jminor.common.Util;
import org.jminor.common.model.Version;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.server.ServerUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * Settings used throughout the framework.
 * These settings are used during initialization and should be set before
 * the application is initialized, before EntityApplicationPanel.startApplication().
 */
public final class Configuration {

  private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

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
   * The version of the server the last RemoteEntityConnectionProvider connected to.
   * Value type: Version<br>
   * Default value: the client version
   */
  public static final String REMOTE_SERVER_VERSION = "jminor.server.version";

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
   * Specifies whether client connections, remote or local, should schedule a periodic validity check of the connection.
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String CONNECTION_SCHEDULE_VALIDATION = "jminor.connection.scheduleValidation";

  /**
   * Specifies the timeout (in seconds) to specify when checking if database connections are valid.
   * Value type: Integer<br>
   * Default value: 0
   */
  public static final String CONNECTION_VALIDITY_CHECK_TIMEOUT = "jminor.connection.validityCheckTimeout";

  /**
   * The report path used for the default report generation,
   * either file or http based
   */
  public static final String REPORT_PATH = "jminor.report.path";

  /**
   * Specifies whether or not reports are cached when loaded from disk/network,<br>
   * this prevents "hot deploy" of reports. This is only applicable if caching makes<br>
   * sense in the reporting plugin context.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String CACHE_REPORTS = "jminor.report.cacheReports";

  /**
   * The host on which to locate the server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final String SERVER_HOST_NAME = "jminor.server.hostname";

  /**
   * The port on which to locate the server registry<br>
   * Value type: Integer<br>
   * Default value: Registry.REGISTRY_PORT (1099)
   */
  public static final String REGISTRY_PORT = "jminor.server.registryPort";

  /**
   * The port on which the server is made available to clients.<br>
   * If specified on the client side, the client will only connect to a server running on this port,
   * use -1 or no value if the client should connect to any available server<br>
   * Value type: Integer<br>
   * Default value: none
   */
  public static final String SERVER_PORT = "jminor.server.port";

  /**
   * The port on which the server should export the remote admin interface<br>
   * Value type: Integer<br>
   * Default value: none
   */
  public static final String SERVER_ADMIN_PORT = "jminor.server.admin.port";

  /**
   * Specifies a username:password combination representing the server admin user<br>
   * Example: scott:tiger
   */
  public static final String SERVER_ADMIN_USER = "jminor.server.admin.user";

  /**
   * The initial connection logging status on the server, either true (on) or false (off)<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String SERVER_CLIENT_LOGGING_ENABLED = "jminor.server.clientLoggingEnabled";

  /**
   * Specifies the size of the (circular) log the server keeps in memory for each connected client<br>
   * Value type: Integer<br>
   * Default value: 40
   */
  public static final String SERVER_CONNECTION_LOG_SIZE = "jminor.server.clientLogSize";

  /**
   * Specifies maximum number of concurrent connections the server accepts<br>
   * -1 indicates no limit and 0 indicates a closed server.
   * Value type: Integer<br>
   * Default value: -1
   */
  public static final String SERVER_CONNECTION_LIMIT = "jminor.server.connectionLimit";

  /**
   * Specifies the default client connection timeout (ms) in a comma separated list.
   * Example: org.jminor.demos.empdept.client.ui.EmpDeptAppPanel:60000,org.jminor.demos.chinook.ui.ChinookAppPanel:120000
   * Value type: String<br>
   * Default value: none
   */
  public static final String SERVER_CLIENT_CONNECTION_TIMEOUT = "jminor.server.clientConnectionTimeout";

  /**
   * Specifies a specific connection timeout for different client types
   * Value type: Integer<br>
   * Default value: 120000ms (2 minutes)
   */
  public static final String SERVER_CONNECTION_TIMEOUT = "jminor.server.connectionTimeout";

  /**
   * Specifies whether the server should establish connections using a secure sockets layer, true (on) or false (off)<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String SERVER_CONNECTION_SSL_ENABLED = "jminor.server.connection.sslEnabled";

  /**
   * Specifies a comma separated list of username:password combinations for which to create connection pools on startup
   * Example: scott:tiger,john:foo,paul:bar
   */
  public static final String SERVER_CONNECTION_POOLING_INITIAL = "jminor.server.pooling.initial";

  /**
   * Specifies the class name of the connection pool provider to user, if none is specified
   * the internal connection pool is used if necessary<br>
   * Value type: String<br>
   * Default value: none
   * @see org.jminor.common.db.pool.ConnectionPoolProvider
   */
  public static final String SERVER_CONNECTION_POOL_PROVIDER_CLASS = "jminor.server.pooling.poolProviderClass";

  /**
   * Specifies a comma separated list of domain model class names, these classes must be
   * available on the server classpath
   */
  public static final String SERVER_DOMAIN_MODEL_CLASSES = "jminor.server.domain.classes";

  /**
   * Specifies a comma separated list of LoginProxy class names, which should be initialized on server startup,
   * these classes must be available on the server classpath and contain a parameterless constructor
   * @see org.jminor.common.server.LoginProxy
   */
  public static final String SERVER_LOGIN_PROXY_CLASSES = "jminor.server.loginProxyClasses";

  /**
   * Specifies a comma separated list of ConnectionValidator class names, which should be initialized on server startup,
   * these classes must be available on the server classpath and contain a parameterless constructor
   * @see org.jminor.common.server.ConnectionValidator
   */
  public static final String SERVER_CONNECTION_VALIDATOR_CLASSES = "jminor.server.connectionValidatorClasses";

  /**
   * Specifies the statistics polling rate for the server monitor, in seconds.
   * Value type: Integer<br>
   * Default value: 5
   */
  public static final String SERVER_MONITOR_UPDATE_RATE = "jminor.server.monitor.updateRate";

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
   * Specifies the hostname of the remote load test server<br>
   * Value type: String<br>
   * Default value: localhost
   */
  public static final String LOAD_TEST_REMOTE_HOSTNAME = "jminor.loadtest.remote.hostname";

  /**
   * The date format pattern to use when showing time values in tables and when
   * creating default time input fields<br>
   * Value type: String<br>
   * Default value: HH:mm
   */
  public static final String TIME_FORMAT = "jminor.client.timeFormat";

  /**
   * The date format pattern to use when showing timestamp values in tables and when
   * creating default timestamp input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy HH:mm
   */
  public static final String TIMESTAMP_FORMAT = "jminor.client.timestampFormat";

  /**
   * The date format pattern to use when showing date values in tables and when
   * creating default date input fields<br>
   * Value type: String<br>
   * Default value: dd-MM-yyyy
   */
  public static final String DATE_FORMAT = "jminor.client.dateFormat";

  /**
   * Specifies whether or not a table model should be automatically filtered when an insert is performed
   * in a master model, using the inserted entity.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String FILTER_ON_MASTER_INSERT = "jminor.client.filterOnMasterInsert";

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
   * Default value: true
   */
  public static final String COMPACT_ENTITY_PANEL_LAYOUT = "jminor.client.compactEntityPanelLayout";

  /**
   * Indicates whether entity edit panel dialogs should be closed on escape<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String DISPOSE_EDIT_DIALOG_ON_ESCAPE = "jminor.client.disposeEditDialogOnEscape";

  /**
   * Indicates whether dialogs opened by child panels in the application should be centered
   * on their respective parent panel or the application frame/dialog.
   * This applies to edit panels.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String CENTER_APPLICATION_DIALOGS = "jminor.client.centerApplicationDialogs";

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
   * Indicates whether the application should ask for confirmation when exiting<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String CONFIRM_EXIT = "jminor.client.confirmExit";

  /**
   * Indicates whether the application should ask for confirmation when exiting if some data is unsaved<br>
   * and whether it should warn when unsaved data is about to be lost due to selection changes f.ex.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String WARN_ABOUT_UNSAVED_DATA = "jminor.client.warnAboutUnsavedData";

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
   * Specifies whether or not the client saves the last successful login username,<br>
   * which is then displayed as the default username the next time the application is started<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String SAVE_DEFAULT_USERNAME = "jminor.client.saveDefaultUsername";

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
   * Default value: true
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
   * Specifies whether the table criteria panels should be visible or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String TABLE_CRITERIA_PANEL_VISIBLE = "jminor.client.tableCriteriaPanelVisible";

  /**
   * Specifies the prefix used when exporting/looking up the JMinor server<br>
   * Value type: String<br>
   * Default value: JMinor Server
   */
  public static final String SERVER_NAME_PREFIX = "jminor.server.namePrefix";

  /**
   * Specifies the web server class, must implement Server.AuxiliaryServer<br>
   * and contain a constructor with the following signature: (Server, String, Integer)<br>
   * for the server, file document root and port respectively<br>
   * Value type: String<br>
   * Default value: org.jminor.framework.plugins.rest.EntityRESTServer
   */
  public static final String WEB_SERVER_IMPLEMENTATION_CLASS = "jminor.server.web.webServerClass";

  /**
   * Specifies the document root for the WebStartServer, if no specified the web server will not be started<br>
   * Value type: String<br>
   * Default value: null
   */
  public static final String WEB_SERVER_DOCUMENT_ROOT = "jminor.server.web.documentRoot";

  /**
   * Specifies the port number for the WebStartServer<br>
   * Value type: Integer<br>
   * Default value: 80
   */
  public static final String WEB_SERVER_PORT = "jminor.server.web.port";

  /**
   * Specifies the wildcard character used by the framework<br>
   * Value type: String<br>
   * Default value: %
   */
  public static final String WILDCARD_CHARACTER = "jminor.wildcardCharacter";

  /**
   * Specifies the default maximum number of fraction digits for double property values<br>
   * Note that values are rounded when set.<br>
   * Value type: Integer<br>
   * Default value: 10
   */
  public static final String MAXIMUM_FRACTION_DIGITS = "jminor.maximumFractionDigits";

  /**
   * Specifies the class providing remote db connections<br>
   * Value type: String (the name of a class implementing org.jminor.framework.db.EntityConnectionProvider)<br>
   * Default value: org.jminor.framework.db.remote.RemoteEntityConnectionProvider
   */
  public static final String REMOTE_CONNECTION_PROVIDER = "jminor.client.remoteConnectionProvider";

  /**
   * Specifies the class used for serializing and deserializing entity instances.<br>
   * Value type: String, the name of the class implementing org.jminor.common.model.Serializer&#60;Entity&#62;<br>
   * Default value: none
   */
  public static final String ENTITY_SERIALIZER_CLASS = "jminor.serialization.entitySerializerClass";

  /**
   * Specifies the class providing local db connections<br>
   * Value type: String (the name of a class implementing org.jminor.framework.db.EntityConnectionProvider)<br>
   * Default value: org.jminor.framework.db.local.LocalEntityConnectionProvider
   */
  public static final String LOCAL_CONNECTION_PROVIDER = "jminor.client.localConnectionProvider";

  /**
   * Specifies the value used by default to represent a null value in combo box models.
   * Using the value null indicates that no null value item should be used.<br>
   * Value type: String<br>
   * Default value: -
   */
  public static final String COMBO_BOX_NULL_VALUE_ITEM = "jminor.client.comboBoxNullValueItem";

  /**
   * Specifies whether the client layer should perform null validation on entities
   * before update/insert actions are performed<br>
   * Value type: Boolean<br>
   * Default value: true
   * @see org.jminor.framework.domain.Property#setNullable(boolean)
   */
  public static final String PERFORM_NULL_VALIDATION = "jminor.client.performNullValidation";

  /**
   * Specifies that it should not be possible to define foreign keys referencing entities that have
   * not been defined, this can be disabled in case of entities with circular references<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String STRICT_FOREIGN_KEYS = "jminor.strictForeignKeys";

  /**
   * Specifies whether or not to allow entities to be re-defined, that is,
   * allow a new definition to replace an old one.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String ALLOW_REDEFINE_ENTITY = "jminor.allowRedefineEntity";

  /**
   * Specifies the default foreign key fetch depth<br>
   * Value type: Integer<br>
   * Default value: 1
   */
  public static final String FOREIGN_KEY_FETCH_DEPTH = "jminor.db.foreignKeyFetchDepth";

  /**
   * Specifies whether the foreign key value graph should be fully populated instead of
   * being limited by the foreign key fetch depth setting.<br>
   * Value type: Boolean<br>
   * Default value: true<br>
   */
  public static final String LIMIT_FOREIGN_KEY_FETCH_DEPTH = "jminor.db.limitForeignKeyFetchDepth";

  /**
   * Specifies whether or not actions to hide detail panels or show them in a dialog are available to the user<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String SHOW_DETAIL_PANEL_CONTROLS = "jminor.client.showDetailPanelControls";

  /**
   * Specifies whether or not a control for toggling the edit panel is available to the user<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String SHOW_TOGGLE_EDIT_PANEL_CONTROL = "jminor.client.showToggleEditPanelControl";

  /**
   * Specifies whether or not an embedded database is shut down when disconnected from<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final String SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT = "jminor.db.shutdownEmbeddedOnDisconnect";

  /**
   * Specifies whether or not the client should save and apply user preferences<br>
   * Value type: Boolean<br>
   * Default value: true if required JSON library is found on classpath, false otherwise
   */
  public static final String USE_CLIENT_PREFERENCES = "jminor.client.useClientPreferences";
  /**
   * Specifies the main configuration file.<br>
   * Value type: String<br>
   * Default value: null
   * @see #parseConfigurationFile()
   */
  public static final String CONFIGURATION_FILE = "jminor.configurationFile";
  /**
   * Add a property with this name in the main configuration file and specify a comma separated list
   * of additional configuration files that should be parsed along with the main configuration file.<br>
   * Value type: String<br>
   * Default value: null
   * @see #parseConfigurationFile()
   */
  public static final String ADDITIONAL_CONFIGURATION_FILES = "jminor.additionalConfigurationFiles";

  private static final int DEFAULT_LOAD_TEST_THINKTIME = 2000;
  private static final int DEFAULT_LOAD_TEST_BATCH_SIZE = 10;
  private static final int DEFAULT_LOAD_TEST_LOGIN_DELAY = 2;
  private static final int DEFAULT_SERVER_CONNECTION_LIMIT = -1;
  private static final int DEFAULT_SERVER_CONNECTION_TIMEOUT = 120000;
  private static final int DEFAULT_SERVER_CONNECTION_LOG_SIZE = 40;
  private static final int DEFAULT_FOREIGN_KEY_FETCH_DEPTH = 1;
  private static final int DEFAULT_WEB_SERVER_PORT = 80;
  private static final int DEFAULT_MAXIMUM_FRACTION_DIGITS = 10;
  private static final int DEFAULT_SERVER_MONITOR_UPDATE_RATE = 5;

  private static final Properties PROPERTIES = new Properties();

  private Configuration() {}

  /**
   * A convenience method for loading this class so that it parses configuration files and such
   */
  public static void init() {/*Simply a convenience method for triggering the loading of this class*/}

  /**
   * Sets the given configuration value
   * @param key the property key
   * @param value the value
   */
  public static void setValue(final String key, final Object value) {
    PROPERTIES.put(key, value);
    System.setProperty(key, value.toString());
  }

  /**
   * Clears the given configuration value
   * @param key the property key
   */
  public static void clearValue(final String key) {
    PROPERTIES.remove(key);
    System.clearProperty(key);
  }

  /**
   * Retrieves the configuration value associated with the given key
   * @param key the property key
   * @return the value
   */
  public static Object getValue(final String key) {
    return PROPERTIES.get(key);
  }

  /**
   * Retrieves the configuration value associated with the given key, assuming it is an Integer
   * @param key the property key
   * @return the value
   */
  public static Integer getIntValue(final String key) {
    return (Integer) getValue(key);
  }

  /**
   * Retrieves the configuration value associated with the given key, assuming it is a Boolean
   * @param key the property key
   * @return the value
   */
  public static Boolean getBooleanValue(final String key) {
    return (Boolean) getValue(key);
  }

  /**
   * Retrieves the configuration value associated with the given key, assuming it is a String
   * @param key the property key
   * @return the value
   */
  public static String getStringValue(final String key) {
    return (String) getValue(key);
  }

  /**
   * @return A non-lenient SimpleDateFormat based on Configuration.DATE_FORMAT
   * @see Configuration#DATE_FORMAT
   */
  public static SimpleDateFormat getDefaultDateFormat() {
    return DateFormats.getDateFormat((String) getValue(DATE_FORMAT));
  }

  /**
   * @return A non-lenient SimpleDateFormat based on Configuration.TIMESTAMP_FORMAT
   * @see org.jminor.framework.Configuration#TIMESTAMP_FORMAT
   */
  public static SimpleDateFormat getDefaultTimestampFormat() {
    return DateFormats.getDateFormat((String) getValue(TIMESTAMP_FORMAT));
  }

  /**
   * @return A non-lenient SimpleDateFormat based on Configuration.TIME_FORMAT
   * @see org.jminor.framework.Configuration#TIME_FORMAT
   */
  public static SimpleDateFormat getDefaultTimeFormat() {
    return DateFormats.getDateFormat((String) getValue(TIME_FORMAT));
  }

  /**
   * @return the value associated with {@link #REPORT_PATH}
   */
  public static String getReportPath() {
    final String path = getStringValue(REPORT_PATH);
    if (Util.nullOrEmpty(path)) {
      throw new IllegalArgumentException(REPORT_PATH + " property is not specified");
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
   * Parses the value associated with the given property, splitting it by comma,
   * returning the trimmed String values.
   * Returns an empty Collection in case the given property has no value.
   * @param propertyName the name of the property
   * @return trimmed String values
   */
  public static Collection<String> parseCommaSeparatedValues(final String propertyName) {
    final Collection<String> values = new ArrayList<>();
    final String commaSeparatedValues = Configuration.getStringValue(propertyName);
    if (!Util.nullOrEmpty(commaSeparatedValues)) {
      final String[] classNames = commaSeparatedValues.split(",");
      for (final String className : classNames) {
        values.add(className.trim());
      }
    }

    return values;
  }

  static {
    parseConfigurationFile();
    //default settings
    PROPERTIES.put(LOAD_TEST_THINKTIME, DEFAULT_LOAD_TEST_THINKTIME);
    PROPERTIES.put(LOAD_TEST_BATCH_SIZE, DEFAULT_LOAD_TEST_BATCH_SIZE);
    PROPERTIES.put(LOAD_TEST_LOGIN_DELAY, DEFAULT_LOAD_TEST_LOGIN_DELAY);
    PROPERTIES.put(LOAD_TEST_REMOTE_HOSTNAME, "localhost");
    PROPERTIES.put(CLIENT_CONNECTION_TYPE, CONNECTION_TYPE_LOCAL);
    PROPERTIES.put(REMOTE_SERVER_VERSION, Version.getVersion());
    PROPERTIES.put(CONNECTION_SCHEDULE_VALIDATION, true);
    PROPERTIES.put(CONNECTION_VALIDITY_CHECK_TIMEOUT, 0);
    PROPERTIES.put(SERVER_CLIENT_LOGGING_ENABLED, false);
    PROPERTIES.put(SERVER_CONNECTION_LIMIT, DEFAULT_SERVER_CONNECTION_LIMIT);
    PROPERTIES.put(SERVER_CONNECTION_TIMEOUT, DEFAULT_SERVER_CONNECTION_TIMEOUT);
    PROPERTIES.put(SERVER_CONNECTION_LOG_SIZE, DEFAULT_SERVER_CONNECTION_LOG_SIZE);
    PROPERTIES.put(SERVER_CONNECTION_SSL_ENABLED, true);
    PROPERTIES.put(SERVER_HOST_NAME, "localhost");
    PROPERTIES.put(REGISTRY_PORT, Registry.REGISTRY_PORT);
    PROPERTIES.put(TIMESTAMP_FORMAT, "dd-MM-yyyy HH:mm");
    PROPERTIES.put(DATE_FORMAT, "dd-MM-yyyy");
    PROPERTIES.put(TIME_FORMAT, "HH:mm");
    PROPERTIES.put(ALL_PANELS_ACTIVE, false);
    PROPERTIES.put(COMPACT_ENTITY_PANEL_LAYOUT, true);
    PROPERTIES.put(USE_KEYBOARD_NAVIGATION, true);
    PROPERTIES.put(USE_FOCUS_ACTIVATION, true);
    PROPERTIES.put(CONFIRM_EXIT, false);
    PROPERTIES.put(WARN_ABOUT_UNSAVED_DATA, false);
    PROPERTIES.put(TOOLBAR_BUTTONS, false);
    PROPERTIES.put(PERSIST_FOREIGN_KEY_VALUES, true);
    PROPERTIES.put(USERNAME_PREFIX, "");
    PROPERTIES.put(AUTHENTICATION_REQUIRED, true);
    PROPERTIES.put(SHOW_STARTUP_DIALOG, true);
    PROPERTIES.put(TRANSFER_FOCUS_ON_ENTER, true);
    PROPERTIES.put(USE_OPTIMISTIC_LOCKING, true);
    PROPERTIES.put(SQL_BOOLEAN_VALUE_FALSE, 0);
    PROPERTIES.put(SQL_BOOLEAN_VALUE_TRUE, 1);
    PROPERTIES.put(TABLE_CRITERIA_PANEL_VISIBLE, false);
    PROPERTIES.put(SERVER_NAME_PREFIX, "JMinor Server");
    PROPERTIES.put(WILDCARD_CHARACTER, "%");
    PROPERTIES.put(REMOTE_CONNECTION_PROVIDER, "org.jminor.framework.db.remote.RemoteEntityConnectionProvider");
    PROPERTIES.put(LOCAL_CONNECTION_PROVIDER, "org.jminor.framework.db.local.LocalEntityConnectionProvider");
    PROPERTIES.put(COMBO_BOX_NULL_VALUE_ITEM, "-");
    PROPERTIES.put(PERFORM_NULL_VALIDATION, true);
    PROPERTIES.put(ALLOW_COLUMN_REORDERING, true);
    PROPERTIES.put(FOREIGN_KEY_FETCH_DEPTH, DEFAULT_FOREIGN_KEY_FETCH_DEPTH);
    PROPERTIES.put(LIMIT_FOREIGN_KEY_FETCH_DEPTH, true);
    PROPERTIES.put(WEB_SERVER_PORT, DEFAULT_WEB_SERVER_PORT);
    PROPERTIES.put(WEB_SERVER_IMPLEMENTATION_CLASS, "org.jminor.framework.plugins.rest.EntityRESTServer");
    PROPERTIES.put(CACHE_REPORTS, true);
    PROPERTIES.put(STRICT_FOREIGN_KEYS, true);
    PROPERTIES.put(SHOW_DETAIL_PANEL_CONTROLS, true);
    PROPERTIES.put(SHOW_TOGGLE_EDIT_PANEL_CONTROL, true);
    PROPERTIES.put(MAXIMUM_FRACTION_DIGITS, DEFAULT_MAXIMUM_FRACTION_DIGITS);
    PROPERTIES.put(SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT, false);
    PROPERTIES.put(USE_CLIENT_PREFERENCES, Util.onClasspath("org.json.JSONObject"));
    PROPERTIES.put(SAVE_DEFAULT_USERNAME, true);
    PROPERTIES.put(DISPOSE_EDIT_DIALOG_ON_ESCAPE, true);
    PROPERTIES.put(CENTER_APPLICATION_DIALOGS, false);
    PROPERTIES.put(ALLOW_REDEFINE_ENTITY, false);
    PROPERTIES.put(SERVER_MONITOR_UPDATE_RATE, DEFAULT_SERVER_MONITOR_UPDATE_RATE);
    PROPERTIES.put(FILTER_ON_MASTER_INSERT, false);
    parseSystemSettings();
  }

  private static void parseSystemSettings() {
    parseBooleanProperty(ALL_PANELS_ACTIVE, PROPERTIES);
    parseBooleanProperty(ALLOW_COLUMN_REORDERING, PROPERTIES);
    parseBooleanProperty(AUTHENTICATION_REQUIRED, PROPERTIES);
    parseStringProperty(CLIENT_CONNECTION_TYPE, PROPERTIES);
    parseBooleanProperty(CONNECTION_SCHEDULE_VALIDATION, PROPERTIES);
    parseIntegerProperty(CONNECTION_VALIDITY_CHECK_TIMEOUT, PROPERTIES);
    parseBooleanProperty(COMPACT_ENTITY_PANEL_LAYOUT, PROPERTIES);
    parseBooleanProperty(CONFIRM_EXIT, PROPERTIES);
    parseBooleanProperty(WARN_ABOUT_UNSAVED_DATA, PROPERTIES);
    parseStringProperty(COMBO_BOX_NULL_VALUE_ITEM, PROPERTIES);
    parseStringProperty(DATE_FORMAT, PROPERTIES);
    parseStringProperty(TIMESTAMP_FORMAT, PROPERTIES);
    parseStringProperty(TIME_FORMAT, PROPERTIES);
    parseIntegerProperty(FOREIGN_KEY_FETCH_DEPTH, PROPERTIES);
    parseBooleanProperty(TABLE_CRITERIA_PANEL_VISIBLE, PROPERTIES);
    parseBooleanProperty(LIMIT_FOREIGN_KEY_FETCH_DEPTH, PROPERTIES);
    parseIntegerProperty(LOAD_TEST_THINKTIME, PROPERTIES);
    parseIntegerProperty(LOAD_TEST_BATCH_SIZE, PROPERTIES);
    parseIntegerProperty(LOAD_TEST_LOGIN_DELAY, PROPERTIES);
    parseBooleanProperty(LOAD_TEST_REMOTE_HOSTNAME, PROPERTIES);
    parseStringProperty(LOCAL_CONNECTION_PROVIDER, PROPERTIES);
    parseBooleanProperty(PERFORM_NULL_VALIDATION, PROPERTIES);
    parseBooleanProperty(PERSIST_FOREIGN_KEY_VALUES, PROPERTIES);
    parseStringProperty(REMOTE_CONNECTION_PROVIDER, PROPERTIES);
    parseStringProperty(SERVER_ADMIN_USER, PROPERTIES);
    parseIntegerProperty(SERVER_PORT, PROPERTIES);
    parseIntegerProperty(SERVER_ADMIN_PORT, PROPERTIES);
    parseStringProperty(SERVER_HOST_NAME, PROPERTIES);
    parseStringProperty(REPORT_PATH, PROPERTIES);
    parseIntegerProperty(REGISTRY_PORT, PROPERTIES);
    parseBooleanProperty(SERVER_CLIENT_LOGGING_ENABLED, PROPERTIES);
    parseIntegerProperty(SERVER_CONNECTION_LIMIT, PROPERTIES);
    parseIntegerProperty(SERVER_CONNECTION_TIMEOUT, PROPERTIES);
    parseStringProperty(SERVER_CLIENT_CONNECTION_TIMEOUT, PROPERTIES);
    parseIntegerProperty(SERVER_CONNECTION_LOG_SIZE, PROPERTIES);
    parseStringProperty(SERVER_CONNECTION_POOLING_INITIAL, PROPERTIES);
    parseStringProperty(SERVER_DOMAIN_MODEL_CLASSES, PROPERTIES);
    parseStringProperty(SERVER_LOGIN_PROXY_CLASSES, PROPERTIES);
    parseStringProperty(SERVER_NAME_PREFIX, PROPERTIES);
    parseBooleanProperty(SERVER_CONNECTION_SSL_ENABLED, PROPERTIES);
    parseBooleanProperty(SHOW_STARTUP_DIALOG, PROPERTIES);
    parseBooleanProperty(TOOLBAR_BUTTONS, PROPERTIES);
    parseBooleanProperty(TRANSFER_FOCUS_ON_ENTER, PROPERTIES);
    parseBooleanProperty(USE_FOCUS_ACTIVATION, PROPERTIES);
    parseBooleanProperty(USE_KEYBOARD_NAVIGATION, PROPERTIES);
    parseBooleanProperty(USE_OPTIMISTIC_LOCKING, PROPERTIES);
    parseStringProperty(USERNAME_PREFIX, PROPERTIES);
    parseStringProperty(WILDCARD_CHARACTER, PROPERTIES);
    parseStringProperty(WEB_SERVER_DOCUMENT_ROOT, PROPERTIES);
    parseIntegerProperty(WEB_SERVER_PORT, PROPERTIES);
    parseStringProperty(WEB_SERVER_IMPLEMENTATION_CLASS, PROPERTIES);
    parseStringProperty(ServerUtil.JAVAX_NET_NET_TRUSTSTORE, PROPERTIES);
    parseBooleanProperty(CACHE_REPORTS, PROPERTIES);
    parseBooleanProperty(STRICT_FOREIGN_KEYS, PROPERTIES);
    parseBooleanProperty(SHOW_DETAIL_PANEL_CONTROLS, PROPERTIES);
    parseBooleanProperty(SHOW_TOGGLE_EDIT_PANEL_CONTROL, PROPERTIES);
    parseIntegerProperty(MAXIMUM_FRACTION_DIGITS, PROPERTIES);
    parseBooleanProperty(SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT, PROPERTIES);
    parseBooleanProperty(USE_CLIENT_PREFERENCES, PROPERTIES);
    parseStringProperty(SERVER_CONNECTION_POOL_PROVIDER_CLASS, PROPERTIES);
    parseBooleanProperty(SAVE_DEFAULT_USERNAME, PROPERTIES);
    parseBooleanProperty(DISPOSE_EDIT_DIALOG_ON_ESCAPE, PROPERTIES);
    parseBooleanProperty(CENTER_APPLICATION_DIALOGS, PROPERTIES);
    parseBooleanProperty(ALLOW_REDEFINE_ENTITY, PROPERTIES);
    parseIntegerProperty(SERVER_MONITOR_UPDATE_RATE, PROPERTIES);
    parseBooleanProperty(FILTER_ON_MASTER_INSERT, PROPERTIES);
  }

  /**
   * Parses the value associated with the given system property and adds it to {@code Properties}.
   * The value is assumed to be an Integer.
   * @param property the name of the property to parse
   * @param properties the Properties to add the value to
   */
  public static void parseIntegerProperty(final String property, final Properties properties) {
    final String value = System.getProperty(property);
    if (value != null) {
      properties.put(property, Integer.parseInt(value));
    }
  }

  /**
   * Parses the value associated with the given system property and adds it to {@code Properties}
   * The value is assumed to be either 'true' or 'false', case insensitive.
   * @param property the name of the property to parse
   * @param properties the Properties to add the value to
   */
  public static void parseBooleanProperty(final String property, final Properties properties) {
    final String value = System.getProperty(property);
    if (value != null) {
      properties.put(property, value.equalsIgnoreCase(Boolean.TRUE.toString()));
    }
  }

  /**
   * Parses the value associated with the given system property and adds it to {@code Properties}
   * @param property the name of the property to parse
   * @param properties the Properties to add the value to
   */
  public static void parseStringProperty(final String property, final Properties properties) {
    final String value = System.getProperty(property);
    if (value != null) {
      properties.put(property, value);
    }
  }

  /**
   * Parses the configuration file specified by the {@link #CONFIGURATION_FILE} property,
   * adding the resulting properties via System.setProperty(key, value).
   * Also parses any configuration files specified by {@link #ADDITIONAL_CONFIGURATION_FILES}.
   * @see #CONFIGURATION_FILE
   */
  public static void parseConfigurationFile() {
    parseConfigurationFile(System.getProperty(CONFIGURATION_FILE));
  }

  /**
   * Parses the given configuration file adding the resulting properties via System.setProperty(key, value).
   * If a file with the given name is not found on the classpath we try to locate it on the filesystem,
   * relative to user.dir, if the file is not found a RuntimeException is thrown.
   * If the {@link #ADDITIONAL_CONFIGURATION_FILES} property is found, the files specified are parsed as well,
   * note that the actual property value is not added to the system properties.
   * @param filename the configuration filename
   * @throws IllegalArgumentException in case the configuration file is not found
   * @see #CONFIGURATION_FILE
   */
  public static void parseConfigurationFile(final String filename) {
    if (filename != null) {
      InputStream inputStream = null;
      String additionalConfigurationFiles = null;
      try {
        inputStream = ClassLoader.getSystemResourceAsStream(filename);
        if (inputStream == null) {//not on classpath
          final File configurationFile = new File(System.getProperty("user.dir") + File.separator + filename);
          if (!configurationFile.exists()) {
            throw new IllegalArgumentException("Configuration file not found on classpath (" + filename + ") or as a file (" + configurationFile.getPath() + ")");
          }
          inputStream = new FileInputStream(configurationFile);
          LOG.debug("Reading configuration file from filesystem: {}", filename);
        }
        else {
          LOG.debug("Reading configuration file from classpath: {}", filename);
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        for (final Map.Entry entry : properties.entrySet()) {
          final Object key = entry.getKey();
          final String value = (String) properties.get(key);
          LOG.debug("{} -> {}", key, value);
          if (ADDITIONAL_CONFIGURATION_FILES.equals(key)) {
            additionalConfigurationFiles = value;
          }
          else {
            System.setProperty((String) key, value);
          }
        }
      }
      catch (final IOException e) {
        throw new RuntimeException(e);
      }
      finally {
        Util.closeSilently(inputStream);
      }
      if (additionalConfigurationFiles != null) {
        final String[] configurationFiles = additionalConfigurationFiles.split(",");
        for (final String configurationFile : configurationFiles) {
          parseConfigurationFile(configurationFile.trim());
        }
      }
    }
  }
}
