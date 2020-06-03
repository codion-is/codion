/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import static is.codion.framework.demos.petclinic.domain.Clinic.Visit;

public final class VisitEditPanel extends EntityEditPanel {

  public VisitEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Visit.PET_FK);

    createForeignKeyComboBox(Visit.PET_FK);
    createTextField(Visit.DATE);
    createTextArea(Visit.DESCRIPTION, 4, 20);

    JPanel northPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    northPanel.add(createPropertyPanel(Visit.PET_FK));
    northPanel.add(createPropertyPanel(Visit.DATE));

    setLayout(new BorderLayout(5, 5));
    add(northPanel, BorderLayout.NORTH);
    add(createPropertyPanel(Visit.DESCRIPTION,
            new JScrollPane(getComponent(Visit.DESCRIPTION))), BorderLayout.CENTER);
  }
}
