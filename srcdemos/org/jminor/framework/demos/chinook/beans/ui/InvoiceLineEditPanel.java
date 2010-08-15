/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

import javax.swing.JTextField;
import java.awt.GridLayout;

public class InvoiceLineEditPanel extends EntityEditPanel {

  public InvoiceLineEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusComponentKey(INVOICELINE_INVOICEID_FK);
    createEntityLookupField(INVOICELINE_INVOICEID_FK);
    final JTextField txtTrack = createEntityLookupField(INVOICELINE_TRACKID_FK);
    txtTrack.setColumns(25);
    createTextField(INVOICELINE_QUANTITY);
    createTextField(INVOICELINE_UNITPRICE, LinkType.READ_ONLY);

    setLayout(new GridLayout(4, 1, 5, 5));
    addPropertyPanel(INVOICELINE_INVOICEID_FK);
    addPropertyPanel(INVOICELINE_TRACKID_FK);
    addPropertyPanel(INVOICELINE_QUANTITY);
    addPropertyPanel(INVOICELINE_UNITPRICE);
  }
}