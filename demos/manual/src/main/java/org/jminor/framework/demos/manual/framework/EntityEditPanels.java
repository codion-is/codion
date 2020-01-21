/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.manual.framework;

import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

public final class EntityEditPanels {

  private static final class Domain {
    static final String BOOLEAN_PROPERTY = "boolean";
  }

  private static final class EditPanelDemo extends EntityEditPanel {

    public EditPanelDemo(final SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      // tag::checkBox[]
      createCheckBox(Domain.BOOLEAN_PROPERTY, null, false);
      // end::checkBox[]
      // tag::booleanComboBox[]
      createBooleanComboBox(Domain.BOOLEAN_PROPERTY);
      // end::booleanComboBox[]
    }
  }
}
