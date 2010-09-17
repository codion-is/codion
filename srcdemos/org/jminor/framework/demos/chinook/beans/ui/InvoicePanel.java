/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entities;

import javax.swing.JTable;
import java.awt.Dimension;

public class InvoicePanel extends EntityPanel {

  public InvoicePanel(final EntityModel model) {
    super(model, new InvoiceEditPanel(model.getEditModel(),
            initializeInvoiceLinePanel(model.getDetailModel(Chinook.T_INVOICELINE))));
  }

  private static EntityPanel initializeInvoiceLinePanel(final EntityModel invoiceLineModel) {
    final EntityTablePanel invoiceLineTablePanel = new EntityTablePanel(invoiceLineModel.getTableModel());
    invoiceLineTablePanel.setIncludeSouthPanel(false);
    invoiceLineTablePanel.getJTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    final EntityEditPanel invoiceLineEditPanel = new InvoiceLineEditPanel(invoiceLineModel.getEditModel(),
            invoiceLineTablePanel.getSearchField());
    final EntityPanel invoiceLinePanel = new EntityPanel(invoiceLineModel,
            invoiceLineEditPanel, invoiceLineTablePanel);
    invoiceLinePanel.setIncludeControlPanel(false);
    invoiceLineModel.getTableModel().setColumnVisible(Entities.getProperty(Chinook.T_INVOICELINE, Chinook.INVOICELINE_INVOICEID_FK), false);
    invoiceLinePanel.getTablePanel().setPreferredSize(new Dimension(360, 40));
    invoiceLinePanel.initializePanel();

    return invoiceLinePanel;
  }
}
