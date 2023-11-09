/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.TabbedPanelLayout;

public final class InvoicePanel extends EntityPanel {

  public InvoicePanel(SwingEntityModel invoiceModel, EntityPanel invoiceLinePanel) {
    super(invoiceModel, new InvoiceEditPanel(invoiceModel.editModel(), invoiceLinePanel),
            TabbedPanelLayout.builder()
                    .includeDetailTabPane(false)
                    .includeDetailControls(false)
                    .build());
    tablePanel().editableAttributes().remove(Invoice.TOTAL);
    addDetailPanel(invoiceLinePanel);
  }
}
