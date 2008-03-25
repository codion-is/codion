/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework;

import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.io.Serializable;

/**
 * Settings used throughout the framework
 */
public class FrameworkSettings implements Serializable {

  public boolean useQueryRange = true;
  public boolean allModelsEnabled = false;
  public boolean useKeyboardNavigation = true;
  public boolean useFocusActivation = true;
  public int tableAutoResizeMode = JTable.AUTO_RESIZE_OFF;
  public boolean useSmartRefresh = false;
  public boolean confirmExit = false;
  public boolean propertyDebug = false;
  public int tabPlacement = JTabbedPane.TOP;
  public boolean toolbarActions = false;
  public boolean resetComboBoxModelsOnClear = false;
  public Object sqlBooleanValueFalse = 0;
  public Object sqlBooleanValueTrue = 1;
  public Object sqlBooleanValueNull = null;
  public long printMemoryUsageInterval = 0;
  public String usernamePrefix = "ops$";
  public boolean transferTextFieldFocusOnEnter = true;
  public boolean strictEditing = false;
  public int tooltipDelay = 500;

  private final static FrameworkSettings instance = new FrameworkSettings();

  public static FrameworkSettings get() {
    return instance;
  }

  /**
   * @return Value for property 'defaultUsername'.
   */
  public static String getDefaultUsername() {
    return System.getProperty(FrameworkConstants.DEFAULT_USERNAME_PROPERTY,
            FrameworkSettings.get().usernamePrefix + System.getProperty("user.name"));
  }
}
