/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.model.ChangeValueMapEditModel;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.chinook.beans.CustomerModel;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class EmployeePanel extends EntityPanel {

  public EmployeePanel(final EntityModel model) {
    super(model, "Employees", true, false, false, HIDDEN);
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel) {
    return new EntityEditPanel((EntityEditModel) editModel) {
      @Override
      protected void initializeUI() {
        final JTextField txtFirstName = createTextField(EMPLOYEE_FIRSTNAME);
        txtFirstName.setColumns(16);
        setDefaultFocusComponent(txtFirstName);
        final JTextField txtLastName = createTextField(EMPLOYEE_LASTNAME);
        txtLastName.setColumns(16);
        final DateInputPanel birthdateInputPanel = createDateInputPanel(EMPLOYEE_BIRTHDATE);
        birthdateInputPanel.getInputField().setColumns(16);
        final JTextField txtAddress = createTextField(EMPLOYEE_ADDRESS);
        txtAddress.setColumns(16);
        final JTextField txtCity = createTextField(EMPLOYEE_CITY);
        txtCity.setColumns(16);
        final JTextField txtState = (JTextField) UiUtil.makeUpperCase(createTextField(EMPLOYEE_STATE));
        txtState.setColumns(16);
        final JTextField txtCountry = createTextField(EMPLOYEE_COUNTRY);
        txtCountry.setColumns(16);
        final JTextField txtPostalcode = createTextField(EMPLOYEE_POSTALCODE);
        txtPostalcode.setColumns(16);
        final JTextField txtPhone = createTextField(EMPLOYEE_PHONE);
        txtPhone.setColumns(16);
        final JTextField txtFax = createTextField(EMPLOYEE_FAX);
        txtFax.setColumns(16);
        final JTextField txtEmail = createTextField(EMPLOYEE_EMAIL);
        txtEmail.setColumns(16);
        final EntityComboBox boxEmp = createEntityComboBox(EMPLOYEE_REPORTSTO_FK);
        final DateInputPanel hiredateInputPanel = createDateInputPanel(EMPLOYEE_HIREDATE);
        hiredateInputPanel.getInputField().setColumns(16);
        final JTextField txtTitle = createTextField(EMPLOYEE_TITLE);
        txtTitle.setColumns(16);

        setLayout(new FlexibleGridLayout(4, 4, 5, 5));
        add(createPropertyPanel(EMPLOYEE_FIRSTNAME, txtFirstName));
        add(createPropertyPanel(EMPLOYEE_LASTNAME, txtLastName));
        add(createPropertyPanel(EMPLOYEE_BIRTHDATE, birthdateInputPanel));
        add(createPropertyPanel(EMPLOYEE_ADDRESS, txtAddress));
        add(createPropertyPanel(EMPLOYEE_CITY, txtCity));
        add(createPropertyPanel(EMPLOYEE_STATE, txtState));
        add(createPropertyPanel(EMPLOYEE_COUNTRY, txtCountry));
        add(createPropertyPanel(EMPLOYEE_POSTALCODE, txtPostalcode));
        add(createPropertyPanel(EMPLOYEE_PHONE, txtPhone));
        add(createPropertyPanel(EMPLOYEE_FAX, txtFax));
        add(createPropertyPanel(EMPLOYEE_EMAIL, txtEmail));
        add(createPropertyPanel(EMPLOYEE_REPORTSTO_FK, boxEmp));
        add(createPropertyPanel(EMPLOYEE_HIREDATE, hiredateInputPanel));
        add(createPropertyPanel(EMPLOYEE_TITLE, txtTitle));
      }
    };
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(CustomerModel.class, CustomerPanel.class));
  }
}