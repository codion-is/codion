/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.chinook.beans.InvoiceModel;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class CustomerPanel extends EntityPanel {

  public CustomerPanel(final EntityModel model) {
    super(model, "Customers");
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        final JTextField txtFirstName = createTextField(CUSTOMER_FIRSTNAME);
        txtFirstName.setColumns(16);
        setDefaultFocusComponent(txtFirstName);
        final JTextField txtLastName = createTextField(CUSTOMER_LASTNAME);
        txtLastName.setColumns(16);
        final JTextField txtCompany = createTextField(CUSTOMER_COMPANY);
        txtCompany.setColumns(16);
        final JTextField txtAddress = createTextField(CUSTOMER_ADDRESS);
        txtAddress.setColumns(16);
        final JTextField txtCity = createTextField(CUSTOMER_CITY);
        txtCity.setColumns(16);
        final JTextField txtState = (JTextField) UiUtil.makeUpperCase(createTextField(CUSTOMER_STATE));
        txtState.setColumns(16);
        final JTextField txtCountry = createTextField(CUSTOMER_COUNTRY);
        txtCountry.setColumns(16);
        final JTextField txtPostalcode = createTextField(CUSTOMER_POSTALCODE);
        txtPostalcode.setColumns(16);
        final JTextField txtPhone = createTextField(CUSTOMER_PHONE);
        txtPhone.setColumns(16);
        final JTextField txtFax = createTextField(CUSTOMER_FAX);
        txtFax.setColumns(16);
        final JTextField txtEmail = createTextField(CUSTOMER_EMAIL);
        txtEmail.setColumns(16);
        final EntityComboBox boxEmp = createEntityComboBox(CUSTOMER_SUPPORTREPID_FK);

        setLayout(new FlexibleGridLayout(3, 4, 5, 5));
        add(createPropertyPanel(CUSTOMER_FIRSTNAME, txtFirstName));
        add(createPropertyPanel(CUSTOMER_LASTNAME, txtLastName));
        add(createPropertyPanel(CUSTOMER_COMPANY, txtCompany));
        add(createPropertyPanel(CUSTOMER_ADDRESS, txtAddress));
        add(createPropertyPanel(CUSTOMER_CITY, txtCity));
        add(createPropertyPanel(CUSTOMER_STATE, txtState));
        add(createPropertyPanel(CUSTOMER_COUNTRY, txtCountry));
        add(createPropertyPanel(CUSTOMER_POSTALCODE, txtPostalcode));
        add(createPropertyPanel(CUSTOMER_PHONE, txtPhone));
        add(createPropertyPanel(CUSTOMER_FAX, txtFax));
        add(createPropertyPanel(CUSTOMER_EMAIL, txtEmail));
        add(createPropertyPanel(CUSTOMER_SUPPORTREPID_FK, boxEmp));
      }
    };
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(InvoiceModel.class, InvoicePanel.class));
  }
}