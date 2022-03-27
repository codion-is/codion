/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class InvoiceLineEditPanel extends EntityEditPanel {

  private final JTextField tableSearchField;

  public InvoiceLineEditPanel(SwingEntityEditModel editModel, JTextField tableSearchField) {
    super(editModel);
    this.tableSearchField = tableSearchField;
    editModel.setPersistValue(InvoiceLine.TRACK_FK, false);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(InvoiceLine.TRACK_FK);

    createForeignKeySearchField(InvoiceLine.TRACK_FK)
            .selectionProviderFactory(TrackSelectionProvider::new)
            .columns(15);
    createTextField(InvoiceLine.QUANTITY)
            .selectAllOnFocusGained(true)
            .columns(2)
            .action(getControl(ControlCode.SAVE));

    setLayout(borderLayout());
    addInputPanel(InvoiceLine.TRACK_FK, BorderLayout.WEST);
    addInputPanel(InvoiceLine.QUANTITY, BorderLayout.CENTER);
    add(createInputPanel(new JLabel(" "), tableSearchField), BorderLayout.EAST);
  }
}