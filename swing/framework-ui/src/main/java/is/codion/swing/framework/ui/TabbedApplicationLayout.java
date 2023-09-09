/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/**
 * Application layout based on a JTabbedPane.
 */
public interface TabbedApplicationLayout extends EntityApplicationPanel.ApplicationLayout {

  /**
   * Specifies the tab placement<br>
   * Value type: Integer (SwingConstants.TOP, SwingConstants.BOTTOM, SwingConstants.LEFT, SwingConstants.RIGHT)<br>
   * Default value: {@link SwingConstants#TOP}
   */
  PropertyValue<Integer> TAB_PLACEMENT = Configuration.integerValue("is.codion.swing.framework.ui.TabbedApplicationLayout.tabPlacement", SwingConstants.TOP);

  /**
   * @return the application tabbed pane
   */
  JTabbedPane applicationTabPane();
}
