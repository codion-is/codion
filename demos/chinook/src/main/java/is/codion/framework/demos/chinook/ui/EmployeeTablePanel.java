/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.model.EmployeeTableModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import static is.codion.swing.common.ui.component.Components.splitPane;
import static is.codion.swing.framework.ui.EntityTree.entityTree;

public final class EmployeeTablePanel extends EntityTablePanel {

  public EmployeeTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected void layoutPanel(JComponent tableComponent, JPanel southPanel) {
    super.layoutPanel(splitPane()
            .orientation(JSplitPane.HORIZONTAL_SPLIT)
            .continuousLayout(true)
            .oneTouchExpandable(true)
            .resizeWeight(0.65)
            .leftComponent(tableComponent)
            .rightComponent(new JScrollPane(entityTree(((EmployeeTableModel) tableModel()).treeModel())))
            .build(), southPanel);
  }
}
