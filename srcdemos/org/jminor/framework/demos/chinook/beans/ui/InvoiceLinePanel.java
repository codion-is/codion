/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class InvoiceLinePanel extends EntityPanel {

  public InvoiceLinePanel(final EntityModel model) {
    super(model);
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
      return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new GridLayout(4, 1, 5, 5));
        final JTextField txtInvoice = createEntityLookupField(INVOICELINE_INVOICEID_FK);
        setInitialFocusComponent(txtInvoice);
        final JTextField txtTrack = createEntityLookupField(INVOICELINE_TRACKID_FK);
        txtTrack.setColumns(25);
        final JTextField txtQuantity = createTextField(INVOICELINE_QUANTITY);
        final JTextField txtUnitPrice = createTextField(INVOICELINE_UNITPRICE, LinkType.READ_ONLY);
        add(createPropertyPanel(INVOICELINE_INVOICEID_FK, txtInvoice));
        add(createPropertyPanel(INVOICELINE_TRACKID_FK, txtTrack));
        add(createPropertyPanel(INVOICELINE_QUANTITY, txtQuantity));
        add(createPropertyPanel(INVOICELINE_UNITPRICE, txtUnitPrice));
      }
    };
  }
}