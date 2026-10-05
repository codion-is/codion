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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.javadoc;

import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Employee;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

/**
 * The {@link EntityEditPanel} javadoc snippets, each the region of the same name.
 */
final class EntityEditPanelSnippets {

  void usage() {
    class CustomerEditPanel extends EntityEditPanel { // @start region=usage

      CustomerEditPanel(SwingEntityEditModel editModel) {
        super(editModel);
      }

      @Override
      protected void initializeUI() {
        create().textField(Customer.FIRSTNAME);
        create().textField(Customer.LASTNAME);

        setLayout(gridLayout(2, 1));

        addInputPanel(Customer.FIRSTNAME);
        addInputPanel(Customer.LASTNAME);
      }
    } // @end
  }

  static final class EmployeeEditPanel extends EntityEditPanel {

    EmployeeEditPanel(SwingEntityEditModel editModel) {
      super(editModel);
      configureControls(layout -> layout // @start region=configureControls
              .separator()
              .control(createCustomControl())); // @end
    }

    @Override // @start region=initializeUI
    protected void initializeUI() {
      create().textField(Employee.FIRSTNAME);
      create().textField(Employee.LASTNAME);

      setLayout(gridLayout(2, 1));

      addInputPanel(Employee.FIRSTNAME);
      addInputPanel(Employee.LASTNAME);
    } // @end

    private Control createCustomControl() {
      return Control.command(() -> {});
    }
  }
}
