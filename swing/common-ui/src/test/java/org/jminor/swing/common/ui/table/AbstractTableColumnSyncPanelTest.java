/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.swing.common.model.table.SwingFilteredTableColumnModel;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractTableColumnSyncPanelTest {

  private final TableColumnSyncPanelImpl panel;
  private final SwingFilteredTableColumnModel columnModel;

  public AbstractTableColumnSyncPanelTest() {
    columnModel = new SwingFilteredTableColumnModel(Arrays.asList(new TableColumn(0, 20), new TableColumn(2, 20)), null);
    panel = new TableColumnSyncPanelImpl(columnModel);
  }

  @Test
  public void addColumn() {
    panel.setVerticalFillerWidth(20);
    final TableColumn col = new TableColumn(3, 20);
    columnModel.addColumn(col);
    assertTrue(panel.getColumnPanels().containsKey(col));
  }

  private static class TableColumnSyncPanelImpl extends AbstractTableColumnSyncPanel {

    private TableColumnSyncPanelImpl(final SwingFilteredTableColumnModel columnModel) {
      super(columnModel);
    }

    @Override
    protected JPanel initializeColumnPanel(final TableColumn column) {
      return new JPanel();
    }
  }
}
