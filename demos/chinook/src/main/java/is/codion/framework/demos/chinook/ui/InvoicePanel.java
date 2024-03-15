/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.TabbedPanelLayout;

public final class InvoicePanel extends EntityPanel {

  public InvoicePanel(SwingEntityModel invoiceModel, EntityPanel invoiceLinePanel) {
    super(invoiceModel, new InvoiceEditPanel(invoiceModel.editModel(), invoiceLinePanel),
            new EntityTablePanel(invoiceModel.tableModel(), settings -> settings
                    .editable(attributes -> attributes.remove(Invoice.TOTAL))),
            settings -> settings.panelLayout(TabbedPanelLayout.builder()
                    .includeDetailTabbedPane(false)
                    .includeDetailControls(false)
                    .build()));
    addDetailPanel(invoiceLinePanel);
  }
}
