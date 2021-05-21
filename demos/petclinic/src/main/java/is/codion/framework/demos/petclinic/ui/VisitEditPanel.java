/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

public final class VisitEditPanel extends EntityEditPanel {

  public VisitEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Visit.PET_FK);

    foreignKeyComboBox(Visit.PET_FK).build();
    textField(Visit.DATE).build();
    textArea(Visit.DESCRIPTION).rows(4).columns(20).build();

    JPanel northPanel = new JPanel(Layouts.gridLayout(1, 2));
    northPanel.add(createInputPanel(Visit.PET_FK));
    northPanel.add(createInputPanel(Visit.DATE));

    setLayout(new BorderLayout(5, 5));
    add(northPanel, BorderLayout.NORTH);
    addInputPanel(Visit.DESCRIPTION,
            new JScrollPane(getComponent(Visit.DESCRIPTION)), BorderLayout.CENTER);
  }
}
