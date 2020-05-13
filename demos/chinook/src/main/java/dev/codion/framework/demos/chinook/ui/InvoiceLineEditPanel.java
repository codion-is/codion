/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.chinook.ui;

import dev.codion.swing.common.ui.KeyEvents;
import dev.codion.swing.framework.model.SwingEntityEditModel;
import dev.codion.swing.framework.ui.EntityEditPanel;
import dev.codion.swing.framework.ui.EntityLookupField;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static dev.codion.framework.demos.chinook.domain.Chinook.INVOICELINE_QUANTITY;
import static dev.codion.framework.demos.chinook.domain.Chinook.INVOICELINE_TRACK_FK;
import static dev.codion.swing.common.ui.layout.Layouts.borderLayout;

public class InvoiceLineEditPanel extends EntityEditPanel {

  private JTextField tableSearchField;

  public InvoiceLineEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
    editModel.setPersistValue(INVOICELINE_TRACK_FK, false);
  }

  public void setTableSearchFeld(final JTextField tableSearchField) {
    this.tableSearchField = tableSearchField;
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(INVOICELINE_TRACK_FK);

    final EntityLookupField trackLookupField = createForeignKeyLookupField(INVOICELINE_TRACK_FK);
    trackLookupField.setSelectionProvider(new TrackSelectionProvider(trackLookupField.getModel()));
    trackLookupField.setColumns(15);
    final JTextField quantityField = createTextField(INVOICELINE_QUANTITY);
    KeyEvents.removeTransferFocusOnEnter(quantityField);//otherwise the action added below wont work
    quantityField.addActionListener(getSaveControl());

    setLayout(borderLayout());
    add(createPropertyPanel(INVOICELINE_TRACK_FK), BorderLayout.WEST);
    add(createPropertyPanel(INVOICELINE_QUANTITY), BorderLayout.CENTER);
    add(createPropertyPanel(new JLabel(" "), tableSearchField), BorderLayout.EAST);
  }
}