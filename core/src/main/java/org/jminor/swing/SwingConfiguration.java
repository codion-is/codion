/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing;

import org.jminor.framework.Configuration;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.util.Properties;

/**
 * Swing specific configuration settings.
 */
public final class SwingConfiguration {

  /**
   * Identifies the completion mode MaximumMatch
   * @see #COMBO_BOX_COMPLETION_MODE
   */
  public static final String COMPLETION_MODE_MAXIMUM_MATCH = "max";

  /**
   * Identifies the completion mode AutoCompletion
   * @see #COMBO_BOX_COMPLETION_MODE
   */
  public static final String COMPLETION_MODE_AUTOCOMPLETE = "auto";
  /**
   * Specifies the default table column resize mode for tables in the application<br>
   * Value type: Integer (JTable.AUTO_RESIZE_*)<br>
   * Default value: JTable.AUTO_RESIZE_OFF
   */
  public static final String TABLE_AUTO_RESIZE_MODE = "jminor.client.tableAutoResizeMode";

  /**
   * Specifies the tab placement<br>
   * Value type: Integer (JTabbedPane.TOP, JTabbedPane.BOTTOM, JTabbedPane.LEFT, JTabbedPane.RIGHT)<br>
   * Default value: JTabbedPane.TOP
   */
  public static final String TAB_PLACEMENT = "jminor.client.tabPlacement";

  /**
   * Specifies if EntityPanels opened via the <code>EntityApplicationPanel.showEntityPanelDialog</code> method
   * should be persisted, or kept in memory, when the dialog is closed.<br>
   * Value type: Boolean<br>
   * Default value: false
   * @see org.jminor.swing.framework.ui.EntityApplicationPanel#showEntityPanelDialog(EntityPanelProvider)
   */
  public static final String PERSIST_ENTITY_PANELS = "jminor.client.persistEntityPanels";

  /**
   * Specifies whether maximum match or autocomplete is used for comboboxes,
   * {@link #COMPLETION_MODE_MAXIMUM_MATCH} for {@link org.jminor.swing.common.ui.combobox.MaximumMatch}
   * and {@link #COMPLETION_MODE_AUTOCOMPLETE} for {@link org.jminor.swing.common.ui.combobox.AutoCompletion}.<br>
   * Value type:String<br>
   * Default value: {@link #COMPLETION_MODE_MAXIMUM_MATCH}
   */
  public static final String COMBO_BOX_COMPLETION_MODE = "jminor.client.comboBoxCompletionMode";

  /**
   * Specifies the default horizontal alignment used in labels<br>
   * Value type: Integer (JLabel.LEFT, JLabel.RIGHT, JLabel.CENTER)<br>
   * Default value: JLabel.LEFT
   */
  public static final String LABEL_TEXT_ALIGNMENT = "jminor.client.labelTextAlignment";

  /**
   * Specifies the default size of the divider for detail panel split panes.<br>
   * Value type: Integer<br>
   * Default value: 18<br>
   */
  public static final String SPLIT_PANE_DIVIDER_SIZE = "jminor.client.splitPaneDividerSize";

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

  private static final Properties PROPERTIES = new Properties();

  private static final int DEFAULT_TABLE_AUTO_RESIZE_MODE = JTable.AUTO_RESIZE_OFF;
  private static final int DEFAULT_TAB_PLACEMENT = JTabbedPane.TOP;
  private static final int DEFAULT_LABEL_TEXT_ALIGNMENT = JLabel.LEFT;
  private static final int DEFAULT_SPLIT_PANE_DIVIDER_SIZE = 18;

  static {
    PROPERTIES.put(TABLE_AUTO_RESIZE_MODE, DEFAULT_TABLE_AUTO_RESIZE_MODE);
    PROPERTIES.put(TAB_PLACEMENT, DEFAULT_TAB_PLACEMENT);
    PROPERTIES.put(PERSIST_ENTITY_PANELS, false);
    PROPERTIES.put(LABEL_TEXT_ALIGNMENT, DEFAULT_LABEL_TEXT_ALIGNMENT);
    PROPERTIES.put(SPLIT_PANE_DIVIDER_SIZE, DEFAULT_SPLIT_PANE_DIVIDER_SIZE);
    PROPERTIES.put(COMBO_BOX_COMPLETION_MODE, COMPLETION_MODE_MAXIMUM_MATCH);
    parseSystemSettings();
  }

  private static void parseSystemSettings() {
    Configuration.parseIntegerProperty(LABEL_TEXT_ALIGNMENT, PROPERTIES);
    Configuration.parseBooleanProperty(PERSIST_ENTITY_PANELS, PROPERTIES);
    Configuration.parseIntegerProperty(TAB_PLACEMENT, PROPERTIES);
    Configuration.parseIntegerProperty(TABLE_AUTO_RESIZE_MODE, PROPERTIES);
    Configuration.parseIntegerProperty(SPLIT_PANE_DIVIDER_SIZE, PROPERTIES);
    Configuration.parseStringProperty(COMBO_BOX_COMPLETION_MODE, PROPERTIES);
  }
}
