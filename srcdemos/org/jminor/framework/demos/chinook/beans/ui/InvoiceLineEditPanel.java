/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.demos.chinook.domain.Chinook;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICELINE_QUANTITY;
import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICELINE_TRACKID_FK;

public class InvoiceLineEditPanel extends EntityEditPanel {

  private JTextField tableSearchField;

  public InvoiceLineEditPanel(final EntityEditModel editModel) {
    super(editModel);
    editModel.setPersistValueOnClear(Chinook.INVOICELINE_TRACKID_FK, false);
  }

  public void setTableSearchFeld(final JTextField tableSearchField) {
    this.tableSearchField = tableSearchField;
  }

  @Override
  protected void initializeUI() {
    setInitialFocusComponentKey(INVOICELINE_TRACKID_FK);
    final JTextField txtTrack = createEntityLookupField(INVOICELINE_TRACKID_FK);
    txtTrack.setColumns(25);
    final JTextField txtQuantity = createTextField(INVOICELINE_QUANTITY);
    txtQuantity.addActionListener(getSaveControl());

    setLayout(new BorderLayout(5, 5));
    add(createPropertyPanel(INVOICELINE_TRACKID_FK), BorderLayout.WEST);
    add(createPropertyPanel(INVOICELINE_QUANTITY), BorderLayout.CENTER);
    add(createPropertyPanel(new JLabel(" "), tableSearchField, true, 5, 5), BorderLayout.EAST);
  }
}