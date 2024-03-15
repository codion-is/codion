/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import java.awt.Dimension;

public final class InvoiceLineTablePanel extends EntityTablePanel {

  public InvoiceLineTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel, config -> config
            .includeSouthPanel(false)
            .includeConditionPanel(false)
            .editable(attributes -> attributes.remove(InvoiceLine.INVOICE_FK)));
    setEditComponentFactory(InvoiceLine.TRACK_FK, new TrackComponentFactory());
    table().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    table().getModel().columnModel().visible(InvoiceLine.INVOICE_FK).set(false);
    setPreferredSize(new Dimension(360, 40));
  }
}
