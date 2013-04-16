/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.table;

import org.junit.Test;

import javax.swing.JPanel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class AbstractTableColumnSyncPanelTest {

  private final TableColumnSyncPanelImpl panel;
  private final TableColumnModel columnModel;

  public AbstractTableColumnSyncPanelTest() {
    columnModel = new DefaultTableColumnModel();
    columnModel.addColumn(new TableColumn(0, 20));
    columnModel.addColumn(new TableColumn(1, 20));
    final List<TableColumn> columns = new ArrayList<TableColumn>();
    columns.add(columnModel.getColumn(0));
    columns.add(columnModel.getColumn(1));
    panel = new TableColumnSyncPanelImpl(columnModel, columns);
  }

  @Test
  public void addColumn() {
    panel.setVerticalFillerWidth(20);
    final TableColumn col = new TableColumn(3, 20);
    columnModel.addColumn(col);
    assertTrue(panel.getColumnPanels().containsKey(col));
  }

  private static class TableColumnSyncPanelImpl extends AbstractTableColumnSyncPanel {

    private TableColumnSyncPanelImpl(final TableColumnModel columnModel, final List<TableColumn> columns) {
      super(columnModel, columns);
    }

    @Override
    protected JPanel initializeColumnPanel(final TableColumn column) {
      return new JPanel();
    }
  }
}
