package org.jminor.common.ui;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import javax.swing.JPanel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

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
  public void test() {
    assertNotNull(panel.getColumnModel());
    panel.setVerticalFillerWidth(20);
    final TableColumn col = columnModel.getColumn(0);
    col.setWidth(30);
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
