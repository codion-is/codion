/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.table;

import org.jminor.common.model.table.DefaultFilteredTableColumnModel;
import org.jminor.common.model.table.FilteredTableColumnModel;

import org.junit.Test;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class AbstractTableColumnSyncPanelTest {

  private final TableColumnSyncPanelImpl panel;
  private final FilteredTableColumnModel columnModel;

  public AbstractTableColumnSyncPanelTest() {
    columnModel = new DefaultFilteredTableColumnModel(Arrays.asList(new TableColumn(0, 20), new TableColumn(2, 20)), null);
    columnModel.addColumn(new TableColumn(0, 20));
    columnModel.addColumn(new TableColumn(1, 20));
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

    private TableColumnSyncPanelImpl(final FilteredTableColumnModel columnModel) {
      super(columnModel);
    }

    @Override
    protected JPanel initializeColumnPanel(final TableColumn column) {
      return new JPanel();
    }
  }
}
