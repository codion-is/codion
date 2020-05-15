/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityLookupField;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static is.codion.framework.demos.chinook.domain.Chinook.INVOICELINE_QUANTITY;
import static is.codion.framework.demos.chinook.domain.Chinook.INVOICELINE_TRACK_FK;
import static is.codion.swing.common.ui.KeyEvents.removeTransferFocusOnEnter;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.textfield.TextFields.selectAllOnFocusGained;

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
    selectAllOnFocusGained(quantityField);
    removeTransferFocusOnEnter(quantityField);//otherwise the action set below wont work
    quantityField.setAction(getSaveControl());

    setLayout(borderLayout());
    add(createPropertyPanel(INVOICELINE_TRACK_FK), BorderLayout.WEST);
    add(createPropertyPanel(INVOICELINE_QUANTITY), BorderLayout.CENTER);
    add(createPropertyPanel(new JLabel(" "), tableSearchField), BorderLayout.EAST);
  }
}