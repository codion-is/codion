/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICELINE_QUANTITY;
import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICELINE_TRACKID_FK;

public class InvoiceLineEditPanel extends EntityEditPanel {

  private JTextField tableSearchField;

  public InvoiceLineEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
    editModel.setValuePersistent(INVOICELINE_TRACKID_FK, false);
  }

  public void setTableSearchFeld(final JTextField tableSearchField) {
    this.tableSearchField = tableSearchField;
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(INVOICELINE_TRACKID_FK);
    final JTextField txtTrack = createForeignKeyLookupField(INVOICELINE_TRACKID_FK);
    txtTrack.setColumns(25);
    final JTextField txtQuantity = createTextField(INVOICELINE_QUANTITY);
    UiUtil.removeTransferFocusOnEnter(txtQuantity);//otherwise the action added below wont work
    txtQuantity.addActionListener(getSaveControl());

    setLayout(new BorderLayout(5, 5));
    add(createPropertyPanel(INVOICELINE_TRACKID_FK), BorderLayout.WEST);
    add(createPropertyPanel(INVOICELINE_QUANTITY), BorderLayout.CENTER);
    add(createPropertyPanel(new JLabel(" "), tableSearchField, true), BorderLayout.EAST);
  }
}