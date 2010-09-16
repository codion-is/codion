/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.chinook.domain.Chinook;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class InvoicePanel extends EntityPanel {

  public InvoicePanel(final EntityModel model) {
    super(model, new InvoiceEditPanel(model.getEditModel(), new InvoiceLinePanel(initializeInvoiceLineModel(model.getEditModel()))));
  }

  private static EntityModel initializeInvoiceLineModel(final EntityEditModel invoiceEditModel) {
    final EntityModel lineModel = new DefaultEntityModel(Chinook.T_INVOICELINE, invoiceEditModel.getDbProvider());
    invoiceEditModel.addValueMapSetListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        lineModel.initialize(Chinook.T_INVOICE, Arrays.asList(invoiceEditModel.getEntityCopy()));
      }
    });

    return lineModel;
  }
}
