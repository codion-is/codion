/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public class InvoiceLineEditPanel extends EntityEditPanel {

  private final JTextField tableSearchField;

  public InvoiceLineEditPanel(final SwingEntityEditModel editModel, final JTextField tableSearchField) {
    super(editModel);
    this.tableSearchField = tableSearchField;
    editModel.setPersistValue(InvoiceLine.TRACK_FK, false);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(InvoiceLine.TRACK_FK);

    foreignKeySearchField(InvoiceLine.TRACK_FK)
            .selectionProviderFactory(TrackSelectionProvider::new)
            .columns(15)
            .build();
    textField(InvoiceLine.QUANTITY)
            .selectAllOnFocusGained()
            .action(Control.control(this::save))
            .build();

    setLayout(borderLayout());
    addInputPanel(InvoiceLine.TRACK_FK, BorderLayout.WEST);
    addInputPanel(InvoiceLine.QUANTITY, BorderLayout.CENTER);
    add(createInputPanel(new JLabel(" "), tableSearchField), BorderLayout.EAST);
  }
}