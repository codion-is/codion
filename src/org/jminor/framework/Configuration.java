/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework;

import org.jminor.common.model.Util;
import org.jminor.common.model.formats.DateFormats;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

/**
 * Settings used throughout the framework.
 * These settings are used during initialization and should be set before
 * the application is initialized, before EntityApplicationPanel.startApplication().
 */
public final class Configuration {

  /**
   * A convenience method for loading this class so that it parses configuration files and such
   */
  public static void init() {}

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
   * Specifies whether client connections, remote or local, should check the validity of their connections periodically.
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final String CLIENT_SCHEDULE_CONNECTION_VALIDATION = "jminor.client.scheduleConnectionValidation";

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
   * Default value: 1099
   */
  public static final String REGISTRY_PORT_NUMBER = "jminor.server.registryPort";

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
   * Default value: 3333
   */
  public static final String SERVER_ADMIN_PORT = "jminor.server.admin.port";

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
   * Specifies maximum number of concurrent connections the server accepts<br>
   * -1 indicates no limit and 0 indicates a closed server.
   * Value type: Integer<br>
   * Default value: -1
   */
  public static final String SERVER_CONNECTION_LIMIT = "jminor.server.connectionLimit";

  /**
   * Specifies the default client connection timeout.
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
   * Specifies the default table column resize mode for tables in the application<br>
   * Value type: Integer (JTable.AUTO_RESIZE_*)<br>
   * Default value: JTable.AUTO_RESIZE_OFF
   */
  public static final String TABLE_AUTO_RESIZE_MODE = "jminor.client.tableAutoResizeMode";

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
  public static final String SEARCH_PANEL_STATE = "jminor.client.searchPanelState";

  /**
   * Specifies the prefix used when exporting/looking up the JMinor server<br>
   * Value type: String<br>
   * Default value: JMinor Server
   */
  public static final String SERVER_NAME_PREFIX = "jminor.server.namePrefix";

  /**
   * Specifies the web server class, must implement EntityConnectionServer.AuxiliaryServer<br>
   * and contain a constructor with the following signature: (EntityConnectionServer, String, Integer)<br>
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
   * Value type: String (the name of a class implementing org.jminor.framework.db.provider.EntityConnectionProvider)<br>
   * Default value: org.jminor.framework.server.RemoteEntityConnectionProvider
   */
  public static final String REMOTE_CONNECTION_PROVIDER = "jminor.client.remoteConnectionProvider";

  /**
   * Specifies the class used for serializing and deserializing entity instances.<br>
   * Value type: String, the name of the class implementing org.jminor.common.model.Serializer<Entity><br>
   * Default value: none
   */
  public static final String ENTITY_SERIALIZER_CLASS = "jminor.serialization.entitySerializerClass";

  /**
   * Specifies the class providing local db connections<br>
   * Value type: String (the name of a class implementing org.jminor.framework.db.provider.EntityConnectionProvider)<br>
   * Default value: org.jminor.framework.db.provider.LocalEntityConnectionProvider
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
   * Specifies the default horizontal alignment used in labels<br>
   * Value type: Integer (JLabel.LEFT, JLabel.RIGHT, JLabel.CENTER)<br>
   * Default value: JLabel.LEFT
   */
  public static final String LABEL_TEXT_ALIGNMENT = "jminor.client.labelTextAlignment";

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
   * Specifies the default size of the divider for detail panel split panes.<br>
   * Value type: Integer<br>
   * Default value: 18<br>
   */
  public static final String SPLIT_PANE_DIVIDER_SIZE = "jminor.client.splitPaneDividerSize";

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

  private static final Properties PROPERTIES = new Properties();

  private static final int DEFAULT_LOAD_TEST_THINKTIME = 2000;
  private static final int DEFAULT_LOAD_TEST_BATCH_SIZE = 10;
  private static final int DEFAULT_LOAD_TEST_LOGIN_DELAY = 2;
  private static final int DEFAULT_SERVER_CONNECTION_LIMIT = -1;
  private static final int DEFAULT_SERVER_CONNECTION_TIMEOUT = 120000;
  private static final int DEFAULT_SERVER_CONNECTION_LOG_SIZE = 40;
  private static final int DEFAULT_SERVER_ADMIN_PORT = 3333;
  private static final int DEFAULT_REGISTRY_PORT_NUMBER = 1099;
  private static final int DEFAULT_TABLE_AUTO_RESIZE_MODE = 0;//JTable.AUTO_RESIZE_OFF
  private static final int DEFAULT_TAB_PLACEMENT = 1;//JTabbedPane.TOP
  private static final int DEFAULT_LABEL_TEXT_ALIGNMENT = 2;//JLabel.LEFT
  private static final int DEFAULT_FOREIGN_KEY_FETCH_DEPTH = 1;
  private static final int DEFAULT_WEB_SERVER_PORT = 80;
  private static final int DEFAULT_SPLIT_PANE_DIVIDER_SIZE = 18;
  private static final int DEFAULT_MAXIMUM_FRACTION_DIGITS = 10;

  static {
    Util.parseConfigurationFile();
    //default settings
    PROPERTIES.put(LOAD_TEST_THINKTIME, DEFAULT_LOAD_TEST_THINKTIME);
    PROPERTIES.put(LOAD_TEST_BATCH_SIZE, DEFAULT_LOAD_TEST_BATCH_SIZE);
    PROPERTIES.put(LOAD_TEST_LOGIN_DELAY, DEFAULT_LOAD_TEST_LOGIN_DELAY);
    PROPERTIES.put(LOAD_TEST_REMOTE_HOSTNAME, "localhost");
    PROPERTIES.put(CLIENT_CONNECTION_TYPE, CONNECTION_TYPE_LOCAL);
    PROPERTIES.put(CLIENT_SCHEDULE_CONNECTION_VALIDATION, true);
    PROPERTIES.put(SERVER_CLIENT_LOGGING_ENABLED, true);
    PROPERTIES.put(SERVER_CONNECTION_LIMIT, DEFAULT_SERVER_CONNECTION_LIMIT);
    PROPERTIES.put(SERVER_CONNECTION_TIMEOUT, DEFAULT_SERVER_CONNECTION_TIMEOUT);
    PROPERTIES.put(SERVER_CONNECTION_LOG_SIZE, DEFAULT_SERVER_CONNECTION_LOG_SIZE);
    PROPERTIES.put(SERVER_CONNECTION_SSL_ENABLED, true);
    PROPERTIES.put(SERVER_ADMIN_PORT, DEFAULT_SERVER_ADMIN_PORT);
    PROPERTIES.put(SERVER_HOST_NAME, "localhost");
    PROPERTIES.put(REGISTRY_PORT_NUMBER, DEFAULT_REGISTRY_PORT_NUMBER);
    PROPERTIES.put(TIMESTAMP_FORMAT, "dd-MM-yyyy HH:mm");
    PROPERTIES.put(DATE_FORMAT, "dd-MM-yyyy");
    PROPERTIES.put(TIME_FORMAT, "HH:mm");
    PROPERTIES.put(ALL_PANELS_ACTIVE, false);
    PROPERTIES.put(COMPACT_ENTITY_PANEL_LAYOUT, true);
    PROPERTIES.put(USE_KEYBOARD_NAVIGATION, true);
    PROPERTIES.put(USE_FOCUS_ACTIVATION, true);
    PROPERTIES.put(TABLE_AUTO_RESIZE_MODE, DEFAULT_TABLE_AUTO_RESIZE_MODE);
    PROPERTIES.put(CONFIRM_EXIT, false);
    PROPERTIES.put(PROPERTY_DEBUG_OUTPUT, false);
    PROPERTIES.put(TAB_PLACEMENT, DEFAULT_TAB_PLACEMENT);
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
    PROPERTIES.put(SEARCH_PANEL_STATE, false);
    PROPERTIES.put(SERVER_NAME_PREFIX, "JMinor Server");
    PROPERTIES.put(WILDCARD_CHARACTER, "%");
    PROPERTIES.put(REMOTE_CONNECTION_PROVIDER, "org.jminor.framework.server.provider.RemoteEntityConnectionProvider");
    PROPERTIES.put(LOCAL_CONNECTION_PROVIDER, "org.jminor.framework.db.provider.LocalEntityConnectionProvider");
    PROPERTIES.put(COMBO_BOX_NULL_VALUE_ITEM, "-");
    PROPERTIES.put(PERFORM_NULL_VALIDATION, true);
    PROPERTIES.put(LABEL_TEXT_ALIGNMENT, DEFAULT_LABEL_TEXT_ALIGNMENT);
    PROPERTIES.put(ALLOW_COLUMN_REORDERING, true);
    PROPERTIES.put(FOREIGN_KEY_FETCH_DEPTH, DEFAULT_FOREIGN_KEY_FETCH_DEPTH);
    PROPERTIES.put(LIMIT_FOREIGN_KEY_FETCH_DEPTH, true);
    PROPERTIES.put(WEB_SERVER_PORT, DEFAULT_WEB_SERVER_PORT);
    PROPERTIES.put(WEB_SERVER_IMPLEMENTATION_CLASS, "org.jminor.framework.plugins.rest.EntityRESTServer");
    PROPERTIES.put(CACHE_REPORTS, true);
    PROPERTIES.put(SPLIT_PANE_DIVIDER_SIZE, DEFAULT_SPLIT_PANE_DIVIDER_SIZE);
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
    parseSystemSettings();
  }

  private static void parseSystemSettings() {
    parseBooleanSetting(ALL_PANELS_ACTIVE);
    parseBooleanSetting(ALLOW_COLUMN_REORDERING);
    parseBooleanSetting(AUTHENTICATION_REQUIRED);
    parseStringSetting(CLIENT_CONNECTION_TYPE);
    parseBooleanSetting(CLIENT_SCHEDULE_CONNECTION_VALIDATION);
    parseBooleanSetting(COMPACT_ENTITY_PANEL_LAYOUT);
    parseBooleanSetting(CONFIRM_EXIT);
    parseStringSetting(COMBO_BOX_NULL_VALUE_ITEM);
    parseStringSetting(DATE_FORMAT);
    parseStringSetting(TIMESTAMP_FORMAT);
    parseStringSetting(TIME_FORMAT);
    parseIntegerSetting(FOREIGN_KEY_FETCH_DEPTH);
    parseIntegerSetting(LABEL_TEXT_ALIGNMENT);
    parseBooleanSetting(SEARCH_PANEL_STATE);
    parseBooleanSetting(LIMIT_FOREIGN_KEY_FETCH_DEPTH);
    parseIntegerSetting(LOAD_TEST_THINKTIME);
    parseIntegerSetting(LOAD_TEST_BATCH_SIZE);
    parseIntegerSetting(LOAD_TEST_LOGIN_DELAY);
    parseBooleanSetting(LOAD_TEST_REMOTE_HOSTNAME);
    parseStringSetting(LOCAL_CONNECTION_PROVIDER);
    parseBooleanSetting(PERFORM_NULL_VALIDATION);
    parseBooleanSetting(PERSIST_ENTITY_PANELS);
    parseBooleanSetting(PERSIST_FOREIGN_KEY_VALUES);
    parseBooleanSetting(PROPERTY_DEBUG_OUTPUT);
    parseStringSetting(REMOTE_CONNECTION_PROVIDER);
    parseIntegerSetting(SERVER_ADMIN_PORT);
    parseIntegerSetting(SERVER_PORT);
    parseStringSetting(SERVER_HOST_NAME);
    parseStringSetting(REPORT_PATH);
    parseIntegerSetting(REGISTRY_PORT_NUMBER);
    parseBooleanSetting(SERVER_CLIENT_LOGGING_ENABLED);
    parseIntegerSetting(SERVER_CONNECTION_LIMIT);
    parseIntegerSetting(SERVER_CONNECTION_TIMEOUT);
    parseIntegerSetting(SERVER_CONNECTION_LOG_SIZE);
    parseStringSetting(SERVER_CONNECTION_POOLING_INITIAL);
    parseStringSetting(SERVER_DOMAIN_MODEL_CLASSES);
    parseStringSetting(SERVER_LOGIN_PROXY_CLASSES);
    parseStringSetting(SERVER_NAME_PREFIX);
    parseBooleanSetting(SERVER_CONNECTION_SSL_ENABLED);
    parseBooleanSetting(SHOW_STARTUP_DIALOG);
    parseIntegerSetting(TAB_PLACEMENT);
    parseIntegerSetting(TABLE_AUTO_RESIZE_MODE);
    parseBooleanSetting(TOOLBAR_BUTTONS);
    parseBooleanSetting(TRANSFER_FOCUS_ON_ENTER);
    parseBooleanSetting(USE_FOCUS_ACTIVATION);
    parseBooleanSetting(USE_KEYBOARD_NAVIGATION);
    parseBooleanSetting(USE_OPTIMISTIC_LOCKING);
    parseStringSetting(USERNAME_PREFIX);
    parseStringSetting(WILDCARD_CHARACTER);
    parseStringSetting(WEB_SERVER_DOCUMENT_ROOT);
    parseIntegerSetting(WEB_SERVER_PORT);
    parseStringSetting(WEB_SERVER_IMPLEMENTATION_CLASS);
    parseStringSetting(Util.JAVAX_NET_NET_TRUSTSTORE);
    parseBooleanSetting(CACHE_REPORTS);
    parseIntegerSetting(SPLIT_PANE_DIVIDER_SIZE);
    parseBooleanSetting(STRICT_FOREIGN_KEYS);
    parseBooleanSetting(SHOW_DETAIL_PANEL_CONTROLS);
    parseBooleanSetting(SHOW_TOGGLE_EDIT_PANEL_CONTROL);
    parseIntegerSetting(MAXIMUM_FRACTION_DIGITS);
    parseBooleanSetting(SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT);
    parseBooleanSetting(USE_CLIENT_PREFERENCES);
    parseStringSetting(SERVER_CONNECTION_POOL_PROVIDER_CLASS);
    parseBooleanSetting(SAVE_DEFAULT_USERNAME);
    parseBooleanSetting(DISPOSE_EDIT_DIALOG_ON_ESCAPE);
    parseBooleanSetting(CENTER_APPLICATION_DIALOGS);
    parseBooleanSetting(ALLOW_REDEFINE_ENTITY);
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
      PROPERTIES.put(setting, value.equalsIgnoreCase(Boolean.TRUE.toString()));
    }
  }

  private static void parseStringSetting(final String setting) {
    final String value = System.getProperty(setting);
    if (value != null) {
      PROPERTIES.put(setting, value);
    }
  }
}
