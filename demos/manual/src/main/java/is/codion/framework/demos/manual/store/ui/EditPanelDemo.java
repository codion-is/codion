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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.manual.store.ui;

import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.swing.common.ui.component.text.AbstractTextComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

final class EditPanelDemo extends EntityEditPanel {

  private EditPanelDemo() {
    super(null);//can't be instantiated
  }

  @Override
  protected void initializeUI() {
    initializeUIBasic();
    initializeUIExpanded();
  }

  private void initializeUIBasic() {
    // tag::basic[]
    createTextField(Customer.FIRST_NAME)
            .columns(12);

    setLayout(gridLayout(1, 1));
    addInputPanel(Customer.FIRST_NAME);
    // end::basic[]
  }

  private void initializeUIExpanded() {
    // tag::expanded[]
    ColumnDefinition<String> firstNameDefinition =
            editModel().entityDefinition().columnDefinition(Customer.FIRST_NAME);

    //create the text field
    JTextField firstNameField = new JTextField();
    firstNameField.setColumns(12);
    firstNameField.setToolTipText(firstNameDefinition.description());
    //associate the text field with the first name attribute
    setComponent(Customer.FIRST_NAME, firstNameField);

    //wrap the text field in a ComponentValue
    ComponentValue<String, JTextField> firstNameFieldValue =
            new AbstractTextComponentValue<String, JTextField>(firstNameField) {
              @Override
              protected String getComponentValue() {
                return component().getText();
              }
              @Override
              protected void setComponentValue(String text) {
                component().setText(text);
              }
            };

    //link the component value to the attribute value in the edit model
    firstNameFieldValue.link(editModel().value(Customer.FIRST_NAME));

    //create the first name label
    JLabel firstNameLabel = new JLabel(firstNameDefinition.caption());
    //associate the label with the text field
    firstNameLabel.setLabelFor(firstNameField);

    //create an input panel, with the label and text field
    JPanel firstNamePanel = new JPanel(borderLayout());
    firstNamePanel.add(firstNameLabel, BorderLayout.NORTH);
    firstNamePanel.add(firstNameField, BorderLayout.CENTER);

    setLayout(gridLayout(1, 1));
    add(firstNamePanel);
    // end::expanded[]
  }
}
