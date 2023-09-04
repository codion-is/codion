/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import javax.swing.JTabbedPane;

/**
 * Application layout based on a JTabbedPane.
 */
public interface TabbedApplicationLayout extends EntityApplicationPanel.ApplicationLayout {

  /**
   * @return the application tabbed pane
   */
  JTabbedPane applicationTabPane();
}
