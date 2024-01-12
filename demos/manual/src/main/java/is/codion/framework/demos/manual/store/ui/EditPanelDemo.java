/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
            editModel().entityDefinition().columns().definition(Customer.FIRST_NAME);

    //create the text field
    JTextField firstNameField = new JTextField();
    firstNameField.setColumns(12);
    firstNameField.setToolTipText(firstNameDefinition.description());
    //associate the text field with the first name attribute
    component(Customer.FIRST_NAME).set(firstNameField);

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
