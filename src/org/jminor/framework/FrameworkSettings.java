/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework;

import org.jminor.common.model.Util;

import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Settings used throughout the framework.
 * These settings are used during initialization and should be set before
 * the application is initialized, before EntityApplicationPanel.startApplication is called.
 * EntityApplicationPanel.initializeSettings is a convenience method for this purpose,
 * override and use it to set settings properties.
 */
public class FrameworkSettings implements Serializable {

  /**
   * Indicates whether query range is used
   * Value type: Boolean
   * Default value: false
   */
  public static final String USE_QUERY_RANGE = "use_query_range";

  /**
   * Indicates whether all entity panels should be enabled and receiving input by default
   * Value type: Boolean
   * Default value: false
   * @see #USE_FOCUS_ACTIVATION
   */
  public static final String ALL_PANELS_ENABLED = "all_panels_enabled";

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
   * @see #ALL_PANELS_ENABLED
   */
  public static final String USE_FOCUS_ACTIVATION = "use_focus_activation";

  /**
   * Specifies the default table column resize mode for tables in the application
   * Value type: Integer (JTable.AUTO_RESIZE_*)
   * Default value: JTable.AUTO_RESIZE_OFF
   */
  public static final String TABLE_AUTO_RESIZE_MODE = "table_auto_resize_mode";

  /**
   * Specifies whether smart refresh is used, which prevents a data refresh
   * if the underlying table data has not changed since the last refresh
   * Value type: Boolean
   * Default value: false
   */
  public static final String USE_SMART_REFRESH = "use_smart_refresh";

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
   * Specifies whether strict editing should be enabled, this involves selecting a record for update
   * when it is modified, thereby locking it for modification
   * Value type: Boolean
   * Default value: false
   */
  public static final String USE_STRICT_EDIT_MODE = "use_strict_edit_mode";

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
   * Value type: String (the name of a class implementing org.jminor.framework.db.IEntityDbProvider)
   * Default value: org.jminor.framework.server.EntityDbRemoteProvider
   */
  public static final String REMOTE_CONNECTION_PROVIDER = "remote_connection_provider";

  /**
   * Specifies the class providing local db connections
   * Value type: String (the name of a class implementing org.jminor.framework.db.IEntityDbProvider)
   * Default value: org.jminor.framework.db.EntityDbLocalProvider
   */
  public static final String LOCAL_CONNECTION_PROVIDER = "local_connection_provider";

  private Map<String, Object> settings = new HashMap<String, Object>();

  private final static FrameworkSettings instance = new FrameworkSettings();

  private FrameworkSettings() {
    //default settings
    setProperty(USE_QUERY_RANGE, false);
    setProperty(ALL_PANELS_ENABLED, false);
    setProperty(USE_KEYBOARD_NAVIGATION, true);
    setProperty(USE_FOCUS_ACTIVATION, true);
    setProperty(TABLE_AUTO_RESIZE_MODE, JTable.AUTO_RESIZE_OFF);
    setProperty(USE_SMART_REFRESH, false);
    setProperty(CONFIRM_EXIT, false);
    setProperty(PROPERTY_DEBUG_OUTPUT, false);
    setProperty(TAB_PLACEMENT, JTabbedPane.TOP);
    setProperty(TOOLBAR_BUTTONS, false);
    setProperty(PERSIST_ENTITY_REFERENCE_VALUES, true);
    setProperty(DEFAULT_USERNAME_PREFIX, "");
    setProperty(AUTHENTICATION_REQUIRED, true);
    setProperty(TRANSFER_FOCUS_ON_ENTER, true);
    setProperty(USE_STRICT_EDIT_MODE, false);
    setProperty(TOOLTIP_DELAY, 500);
    setProperty(SQL_BOOLEAN_VALUE_FALSE, 0);
    setProperty(SQL_BOOLEAN_VALUE_TRUE, 1);
    setProperty(SQL_BOOLEAN_VALUE_NULL, null);
    setProperty(FILTER_QUERY_BY_MASTER, true);
    setProperty(PERSIST_ENTITY_PANELS, false);
    setProperty(INITIAL_SEARCH_PANEL_STATE, false);
    setProperty(SERVER_NAME_PREFIX, "JMinor EntityDb Server");
    setProperty(WILDCARD_CHARACTER, "%");
    setProperty(USE_NUMBER_FORMAT_GROUPING, true);
    setProperty(REMOTE_CONNECTION_PROVIDER, "org.jminor.framework.server.EntityDbRemoteProvider");
    setProperty(LOCAL_CONNECTION_PROVIDER, "org.jminor.framework.db.EntityDbLocalProvider");
  }

  public static FrameworkSettings get() {
    return instance;
  }

  public void setProperty(final String key, final Object value) {
    settings.put(key, value);
  }

  public Object getProperty(final String key) {
    return settings.get(key);
  }

  /**
   * @param applicationIdentifier the application identifier name
   * @return the default username
   */
  public static String getDefaultUsername(final String applicationIdentifier) {
    final String preferredUserName = Util.getDefaultUserName(applicationIdentifier,
            instance.getProperty(DEFAULT_USERNAME_PREFIX) + System.getProperty("user.name"));
    return System.getProperty(FrameworkConstants.DEFAULT_USERNAME_PROPERTY, preferredUserName);
  }
}
