/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.chinook.domain.Chinook;

import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class InvoiceLinePanel extends EntityPanel {

  public InvoiceLinePanel(final EntityModel model) {
    super(model, "Invoice line");
  }

  @Override
  protected EntityEditPanel initializeEditPanel(EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      protected void initializeUI() {
        setLayout(new GridLayout(3, 1, 5, 5));
        final JTextField txtTrack = createEntityLookupField(Chinook.INVOICELINE_TRACKID_FK);
        txtTrack.setColumns(25);
        setDefaultFocusComponent(txtTrack);
        final JTextField txtQuantity = createTextField(Chinook.INVOICELINE_QUANTITY);
        final JTextField txtUnitPrice = createTextField(Chinook.INVOICELINE_UNITPRICE, LinkType.READ_ONLY);
        add(createPropertyPanel(Chinook.INVOICELINE_TRACKID_FK, txtTrack));
        add(createPropertyPanel(Chinook.INVOICELINE_QUANTITY, txtQuantity));
        add(createPropertyPanel(Chinook.INVOICELINE_UNITPRICE, txtUnitPrice));
      }
    };
  }
}