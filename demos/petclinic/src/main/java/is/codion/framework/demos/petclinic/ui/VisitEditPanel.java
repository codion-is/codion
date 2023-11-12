/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.petclinic.ui;

import is.codion.framework.demos.petclinic.domain.api.Visit;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class VisitEditPanel extends EntityEditPanel {

  public VisitEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Visit.PET_FK);

    createForeignKeyComboBox(Visit.PET_FK);
    createTemporalFieldPanel(Visit.VISIT_DATE);
    createTextArea(Visit.DESCRIPTION)
            .rowsColumns(4, 20);

    JPanel northPanel = gridLayoutPanel(1, 2)
            .add(createInputPanel(Visit.PET_FK))
            .add(createInputPanel(Visit.VISIT_DATE))
            .build();

    setLayout(borderLayout());
    add(northPanel, BorderLayout.NORTH);
    addInputPanel(Visit.DESCRIPTION, new JScrollPane(component(Visit.DESCRIPTION)), BorderLayout.CENTER);
  }
}
