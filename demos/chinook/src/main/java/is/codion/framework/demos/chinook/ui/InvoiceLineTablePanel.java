/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import java.awt.Dimension;

public final class InvoiceLineTablePanel extends EntityTablePanel {

  public InvoiceLineTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    setUpdateSelectedComponentFactory(InvoiceLine.TRACK_FK, new TrackComponentFactory());
    setIncludeSouthPanel(false);
    setIncludeConditionPanel(false);
    getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    setPreferredSize(new Dimension(360, 40));
    getTable().getModel().columnModel().setColumnVisible(InvoiceLine.INVOICE_FK, false);
  }
}
