package org.jminor.common.ui;

import org.junit.Test;

import javax.swing.JPanel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AbstractTableColumnSyncPanelTest {

  private final TableColumnSyncPanelImpl panel;
  private final TableColumnModel columnModel;

  public AbstractTableColumnSyncPanelTest() {
    columnModel = new DefaultTableColumnModel();
    columnModel.addColumn(new TableColumn(0, 20));
    columnModel.addColumn(new TableColumn(1, 20));
    panel = new TableColumnSyncPanelImpl(columnModel);
  }

  @Test
  public void addColumn() {
    assertNotNull(panel.getColumnModel());
    panel.setVerticalFillerWidth(20);
    final TableColumn col = new TableColumn(3, 20);
    columnModel.addColumn(col);
    assertTrue(panel.getColumnPanels().containsKey(col));
  }

  private static class TableColumnSyncPanelImpl extends AbstractTableColumnSyncPanel {

    private TableColumnSyncPanelImpl(final TableColumnModel columnModel) {
      super(columnModel);
    }

    @Override
    protected JPanel initializeColumnPanel(final TableColumn column) {
      return new JPanel();
    }
  }
}
