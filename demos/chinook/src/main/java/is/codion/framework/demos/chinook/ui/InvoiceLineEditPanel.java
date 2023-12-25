/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.toolBar;
import static is.codion.swing.common.ui.component.text.TextComponents.preferredTextFieldHeight;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class InvoiceLineEditPanel extends EntityEditPanel {

  private final JTextField tableSearchField;

  public InvoiceLineEditPanel(SwingEntityEditModel editModel, JTextField tableSearchField) {
    super(editModel);
    this.tableSearchField = tableSearchField;
    editModel.persist(InvoiceLine.TRACK_FK).set(false);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(InvoiceLine.TRACK_FK);

    createForeignKeySearchField(InvoiceLine.TRACK_FK)
            .selectorFactory(new TrackSelectorFactory())
            .columns(15);
    createTextField(InvoiceLine.QUANTITY)
            .selectAllOnFocusGained(true)
            .columns(2)
            .action(control(EditControl.INSERT).get());

    JToolBar updateToolBar = toolBar()
            .floatable(false)
            .action(control(EditControl.UPDATE).get())
            .preferredHeight(preferredTextFieldHeight())
            .build();

    JPanel centerPanel = flexibleGridLayoutPanel(1, 0)
            .add(createInputPanel(InvoiceLine.TRACK_FK))
            .add(createInputPanel(InvoiceLine.QUANTITY))
            .add(createInputPanel(new JLabel(" "), updateToolBar))
            .add(createInputPanel(new JLabel(" "), tableSearchField))
            .build();

    setLayout(borderLayout());
    add(centerPanel, BorderLayout.CENTER);
  }
}