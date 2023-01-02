/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.model.EmployeeTableModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityTree.entityTree;

public final class EmployeeTablePanel extends EntityTablePanel {

  public EmployeeTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected void layoutPanel(JPanel tablePanel, JPanel southPanel) {
    setLayout(borderLayout());
    JSplitPane splitPane = Components.splitPane()
            .orientation(JSplitPane.HORIZONTAL_SPLIT)
            .dividerSize(EntityPanel.SPLIT_PANE_DIVIDER_SIZE.get())
            .continuousLayout(true)
            .oneTouchExpandable(true)
            .resizeWeight(0.65)
            .leftComponent(tablePanel)
            .rightComponent(new JScrollPane(entityTree(((EmployeeTableModel) tableModel()).treeModel())))
            .build();
    add(splitPane, BorderLayout.CENTER);
    add(southPanel, BorderLayout.SOUTH);
  }
}
