/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework;

import org.jminor.common.model.Util;

import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.util.HashMap;
import java.util.Map;

/**
 * Settings used throughout the framework.
 * These settings are used during initialization and should be set before
 * the application is initialized, before EntityApplicationPanel.startApplication is called.
 * EntityApplicationPanel.initializeSettings is a convenience method for this purpose,
 * override and use it to set configuration properties.
 */
public class Configuration {

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
   * accepted values: local, remote
   * @see #CONNECTION_TYPE_LOCAL
   * @see #CONNECTION_TYPE_REMOTE
   */
  public static final String CLIENT_CONNECTION_TYPE = "jminor.client.connection.type";

  /**
   * The report path used for the default report generation,
   * either file or http based
   */
  public static final String REPORT_PATH = "jminor.report.path";

  /**
   * Default username for the login panel
   */
  public static final String DEFAULT_USERNAME = "jminor.client.defaultuser";

  /**
   * The host on which to locate the server
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
   * The initial logging status on the server, either 1 (on) or (0) off
   */
  public static final String SERVER_LOGGING_ON = "jminor.server.logging.status";

  /**
   * Specifies the size of the (circular) log the server keeps in memory for each connected client
   */
  public static final String SERVER_CONNECTION_LOG_SIZE = "jminor.server.logging.clientlogsize";

  /**
   * Specifies whether the server should establish connections using a secure sockets layer, 1 (on) or 0 (off)
   */
  public static final String SERVER_SECURE_CONNECTION = "jminor.server.connection.secure";

  /**
   * Specifies a comma seperated list of usernames for which to create connection pools on startup
   */
  public static final String SERVER_POOLING_INITIAL = "jminor.server.pooling.initial";

  /**
   * Specifies the initial think time setting for the profiling client
   * (max think time = thinktime, min think time = max think time / 2)
   */
  public static final String PROFILING_THINKTIME = "jminor.profiling.thinktime";

  /**
   * Specifies the number time which the max think time is multiplied with when initializing the clients
   */
  public static final String PROFILING_LOGIN_WAIT = "jminor.profiling.loginwait";

  /**
   * Specifies the initial client batch size
   */
  public static final String PROFILING_BATCH_SIZE = "jminor.profiling.batchsize";

  /**
   * The date format pattern to use when showing timestamp values in tables and when
   * creating default timestamp input fields
   * Value type: String
   * Default value: dd-MM-yyyy HH:mm
   */
  public static final String DEFAULT_TIMESTAMP_FORMAT = "default_timestamp_format";

  /**
   * The date format pattern to use when showing date values in tables and when
   * creating default date input fields
   * Value type: String
   * Default value: dd-MM-yyyy
   */
  public static final String DEFAULT_DATE_FORMAT = "default_date_format";

  /**
   * Indicates whether all entity panels should be enabled and receiving input by default
   * Value type: Boolean
   * Default value: false
   * @see #USE_FOCUS_ACTIVATION
   */
  public static final String ALL_PANELS_ACTIVE = "all_panels_active";

  /**
   * Indicates whether keyboard navigation will be enabled
   * Value type: Boolean
   * Default value: true
   */
  public static final String USE_KEYBOARD_NAVIGATION = "use_keyboard_navigation";

  /**
   * Indicates whether entity panels should be activated when the panel recieves focus
   * Value type: Boolean
   * Default value: true
   * @see #ALL_PANELS_ACTIVE
   */
  public static final String USE_FOCUS_ACTIVATION = "use_focus_activation";

  /**
   * Specifies the default table column resize mode for tables in the application
   * Value type: Integer (JTable.AUTO_RESIZE_*)
   * Default value: JTable.AUTO_RESIZE_OFF
   */
  public static final String TABLE_AUTO_RESIZE_MODE = "table_auto_resize_mode";

  /**
   * Indicates whether the application should ask for confirmation when exiting
   * Value type: Boolean
   * Default value: false
   */
  public static final String CONFIRM_EXIT = "confirm_exit";

  /**
   * Specifies whether the framework should output verbose debug output regarding property changes in entities
   * Value type: Boolean
   * Default value: false
   */
  public static final String PROPERTY_DEBUG_OUTPUT = "property_debug_output";

  /**
   * Specifies the tab placement
   * Value type: Integer (JTabbedPane.TOP, JTabbedPane.BOTTOM, JTabbedPane.LEFT, JTabbedPane.RIGHT)
   * Default value: JTabbedPane.TOP
   */
  public static final String TAB_PLACEMENT = "tab_placement";

  /**
   * Specifies whether the action buttons (Save, update, delete, clear, refresh) should be on a toolbar
   * Value type: Boolean
   * Default value: false
   */
  public static final String TOOLBAR_BUTTONS = "toolbar_buttons";

  /**
   * Specifies whether entity field values should persist when the UI is cleared or be reset to null
   * Value type: Boolean
   * Default value: true
   */
  public static final String PERSIST_ENTITY_REFERENCE_VALUES = "persist_entity_reference_values";

  /**
   * Specifies a string prepended to the username field in the login dialog
   * Value type: String
   * Default value: [empty string]
   */
  public static final String DEFAULT_USERNAME_PREFIX = "default_username_prefix";

  /**
   * Specifies whether user authentication is required
   * Value type: Boolean
   * Default value: true
   */
  public static final String AUTHENTICATION_REQUIRED = "authentication_required";

  /**
   * Specifies whether focus should be transfered from components on enter,
   * this does not work for editable combo boxes, combo boxes with the
   * maximum match functionality enabled or text areas
   * Value type: Boolean
   * Default value: true
   */
  public static final String TRANSFER_FOCUS_ON_ENTER = "transfer_focus_on_enter";

  /**
   * Specifies whether optimistic locking should be performed, that is, if entities should
   * be checked for modification before being updated
   * Value type: Boolean
   * Default value: false
   */
  public static final String USE_OPTIMISTIC_LOCKING = "use_optimistic_locking";

  /**
   * Specifies the global tooltip delay in milliseconds
   * Value type: Integer
   * Default value: 500
   */
  public static final String TOOLTIP_DELAY = "tooltip_delay";

  /**
   * Specifies the value used to denote a boolean false in the database
   * Value type: Any Object
   * Default value: 0
   */
  public static final String SQL_BOOLEAN_VALUE_FALSE = "sql_boolean_value_false";

  /**
   * Specifies the value used to denote a boolean true in the database
   * Value type: Any Object
   * Default value: 1
   */
  public static final String SQL_BOOLEAN_VALUE_TRUE = "sql_boolean_value_true";

  /**
   * Specifies the value used to denote a null boolean value in the database
   * Value type: Any Object
   * Default value: null
   */
  public static final String SQL_BOOLEAN_VALUE_NULL = "sql_boolean_value_null";

  /**
   * Specifies if the default filtering behaviour should be to filter the underlying query
   * Value type: Boolean
   * Default value: true
   */
  public static final String FILTER_QUERY_BY_MASTER = "filter_query_by_master";

  /**
   * Specifies if EntityPanels opened via the <code>EntityApplicationPanel.showEntityPanel</code> method
   * should be persisted, or kept in memory, when the dialog is closed.
   * Value type: Boolean
   * Default value: false
   * @see org.jminor.framework.client.ui.EntityApplicationPanel#showEntityPanel(org.jminor.framework.client.ui.EntityPanelProvider)
   */
  public static final String PERSIST_ENTITY_PANELS = "persist_entity_panels";

  /**
   * Specifies the initial search panel state, whether it should be visible or not by default
   * Value type: Boolean
   * Default value: false
   */
  public static final String INITIAL_SEARCH_PANEL_STATE = "inital_search_panel_state";

  /**
   * Specifies the prefix used when exporting/looking up the JMinor server
   * Value type: String
   * Default value: JMinor EntityDb Server
   */
  public static final String SERVER_NAME_PREFIX = "server_name_prefix";

  /**
   * Specifies the wildcard character used by the framework
   * Value type: String
   * Default value: %
   */
  public static final String WILDCARD_CHARACTER = "wildcard_character";

  /**
   * Specifies whether or not to use number format grouping in table views,
   * i.e. 1234567 shown as 1.234.567 or 1,234,567 depending on locale
   * Value type: Boolean
   * Default value: true
   */
  public static final String USE_NUMBER_FORMAT_GROUPING = "use_number_format_grouping";

  /**
   * Specifies the class providing remote db connections
   * Value type: String (the name of a class implementing org.jminor.framework.db.provider.EntityDbProvider)
   * Default value: org.jminor.framework.server.EntityDbRemoteProvider
   */
  public static final String REMOTE_CONNECTION_PROVIDER = "remote_connection_provider";

  /**
   * Specifies the class providing local db connections
   * Value type: String (the name of a class implementing org.jminor.framework.db.provider.EntityDbProvider)
   * Default value: org.jminor.framework.db.provider.EntityDbLocalProvider
   */
  public static final String LOCAL_CONNECTION_PROVIDER = "local_connection_provider";

  /**
   * Specifies the value used by default to represent a null value in combo box models.
   * Using the value null indicates that no null value item should be used (the default).
   * Value type: String
   * Default value: null
   */
  public static final String DEFAULT_COMBO_BOX_NULL_VALUE_ITEM = "default_combo_box_null_value_item";

  private static Map<String, Object> settings = new HashMap<String, Object>();

  static {
    //default settings
    setValue(DEFAULT_TIMESTAMP_FORMAT, "dd-MM-yyyy HH:mm");
    setValue(DEFAULT_DATE_FORMAT, "dd-MM-yyyy");
    setValue(ALL_PANELS_ACTIVE, false);
    setValue(USE_KEYBOARD_NAVIGATION, true);
    setValue(USE_FOCUS_ACTIVATION, true);
    setValue(TABLE_AUTO_RESIZE_MODE, JTable.AUTO_RESIZE_OFF);
    setValue(CONFIRM_EXIT, false);
    setValue(PROPERTY_DEBUG_OUTPUT, false);
    setValue(TAB_PLACEMENT, JTabbedPane.TOP);
    setValue(TOOLBAR_BUTTONS, false);
    setValue(PERSIST_ENTITY_REFERENCE_VALUES, true);
    setValue(DEFAULT_USERNAME_PREFIX, "");
    setValue(AUTHENTICATION_REQUIRED, true);
    setValue(TRANSFER_FOCUS_ON_ENTER, true);
    setValue(USE_OPTIMISTIC_LOCKING, false);
    setValue(TOOLTIP_DELAY, 500);
    setValue(SQL_BOOLEAN_VALUE_FALSE, 0);
    setValue(SQL_BOOLEAN_VALUE_TRUE, 1);
    setValue(SQL_BOOLEAN_VALUE_NULL, null);
    setValue(FILTER_QUERY_BY_MASTER, true);
    setValue(PERSIST_ENTITY_PANELS, false);
    setValue(INITIAL_SEARCH_PANEL_STATE, false);
    setValue(SERVER_NAME_PREFIX, "JMinor Server");
    setValue(WILDCARD_CHARACTER, "%");
    setValue(USE_NUMBER_FORMAT_GROUPING, true);
    setValue(REMOTE_CONNECTION_PROVIDER, "org.jminor.framework.server.provider.EntityDbRemoteProvider");
    setValue(LOCAL_CONNECTION_PROVIDER, "org.jminor.framework.db.provider.EntityDbLocalProvider");
    setValue(DEFAULT_COMBO_BOX_NULL_VALUE_ITEM, null);
  }

  public static void setValue(final String key, final Object value) {
    settings.put(key, value);
  }

  public static Object getValue(final String key) {
    return settings.get(key);
  }

  /**
   * @param applicationIdentifier the application identifier name
   * @return the default username
   */
  public static String getDefaultUsername(final String applicationIdentifier) {
    final String preferredUserName = Util.getDefaultUserName(applicationIdentifier,
            getValue(DEFAULT_USERNAME_PREFIX) + System.getProperty("user.name"));
    return System.getProperty(DEFAULT_USERNAME, preferredUserName);
  }
}
