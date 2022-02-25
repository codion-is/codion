/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.model.EmployeeTableModel;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.EntityTree;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;

public final class EmployeeTablePanel extends EntityTablePanel {

  public EmployeeTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected void layoutPanel(final JPanel tablePanel, final JPanel southPanel) {
    setLayout(Layouts.borderLayout());
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setDividerSize(EntityPanel.SPLIT_PANE_DIVIDER_SIZE.get());
    splitPane.setContinuousLayout(true);
    splitPane.setOneTouchExpandable(true);
    splitPane.setTopComponent(new JScrollPane(new EntityTree(((EmployeeTableModel) getTableModel()).getTreeModel())));
    splitPane.setBottomComponent(tablePanel);
    add(splitPane, BorderLayout.CENTER);
    add(southPanel, BorderLayout.SOUTH);
  }
}
