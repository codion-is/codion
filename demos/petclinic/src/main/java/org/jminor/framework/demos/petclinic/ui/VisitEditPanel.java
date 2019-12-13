/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.ui;

import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import static org.jminor.framework.demos.petclinic.domain.Clinic.*;

public final class VisitEditPanel extends EntityEditPanel {

  public VisitEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(VISIT_PET_FK);

    createForeignKeyComboBox(VISIT_PET_FK);
    createTextField(VISIT_DATE);
    createTextArea(VISIT_DESCRIPTION, 4, 20);

    JPanel northPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    northPanel.add(createPropertyPanel(VISIT_PET_FK));
    northPanel.add(createPropertyPanel(VISIT_DATE));

    setLayout(new BorderLayout(5, 5));
    add(northPanel, BorderLayout.NORTH);
    add(createPropertyPanel(VISIT_DESCRIPTION,
            new JScrollPane(getComponent(VISIT_DESCRIPTION))), BorderLayout.CENTER);
  }
}
