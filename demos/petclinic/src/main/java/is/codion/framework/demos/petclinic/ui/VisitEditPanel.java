/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class VisitEditPanel extends EntityEditPanel {

  public VisitEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Visit.PET_FK);

    createForeignKeyComboBox(Visit.PET_FK);
    createTextField(Visit.VISIT_DATE);
    createTextArea(Visit.DESCRIPTION)
            .rowsColumns(4, 20);

    JPanel northPanel = Components.panel(gridLayout(1, 2))
            .add(createInputPanel(Visit.PET_FK))
            .add(createInputPanel(Visit.VISIT_DATE))
            .build();

    setLayout(borderLayout());
    add(northPanel, BorderLayout.NORTH);
    addInputPanel(Visit.DESCRIPTION, new JScrollPane(getComponent(Visit.DESCRIPTION)), BorderLayout.CENTER);
  }
}
