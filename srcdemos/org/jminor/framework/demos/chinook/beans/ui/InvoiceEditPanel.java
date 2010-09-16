/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.ui.DateInputPanel;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.demos.chinook.domain.Chinook;
import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICE_BILLINGADDRESS;
import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICE_BILLINGCITY;
import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICE_BILLINGCOUNTRY;
import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICE_BILLINGPOSTALCODE;
import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICE_BILLINGSTATE;
import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICE_CUSTOMERID_FK;
import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICE_INVOICEDATE;
import static org.jminor.framework.demos.chinook.domain.Chinook.INVOICE_TOTAL;
import org.jminor.framework.domain.Entities;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class InvoiceEditPanel extends EntityEditPanel {

  public InvoiceEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusComponentKey(INVOICE_CUSTOMERID_FK);
    final JTextField txtCustomer = createEntityLookupField(INVOICE_CUSTOMERID_FK);
    txtCustomer.setColumns(16);
    final DateInputPanel datePanel = createDateInputPanel(INVOICE_INVOICEDATE);
    datePanel.getInputField().setColumns(16);
    final JTextField txtAddress = createTextField(INVOICE_BILLINGADDRESS);
    txtAddress.setColumns(16);
    final JTextField txtCity = createTextField(INVOICE_BILLINGCITY);
    txtCity.setColumns(16);
    final JTextField txtState = createTextField(INVOICE_BILLINGSTATE);
    txtState.setColumns(16);
    final JTextField txtCountry = createTextField(INVOICE_BILLINGCOUNTRY);
    txtCountry.setColumns(16);
    final JTextField txtPostalcode = createTextField(INVOICE_BILLINGPOSTALCODE);
    txtPostalcode.setColumns(16);
    final JTextField txtTotal = createTextField(INVOICE_TOTAL);
    txtTotal.setColumns(16);

    final JPanel propertyBase = new JPanel(new GridLayout(4, 2, 5, 5));
    propertyBase.add(createPropertyPanel(INVOICE_CUSTOMERID_FK));
    propertyBase.add(createPropertyPanel(INVOICE_INVOICEDATE));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGADDRESS));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGCITY));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGSTATE));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGCOUNTRY));
    propertyBase.add(createPropertyPanel(INVOICE_BILLINGPOSTALCODE));
    propertyBase.add(createPropertyPanel(INVOICE_TOTAL));

    final JPanel centerBase = new JPanel(new BorderLayout(5, 5));
    centerBase.add(propertyBase, BorderLayout.NORTH);
    setLayout(new BorderLayout(5, 5));
    add(centerBase, BorderLayout.CENTER);

    final EntityModel lineModel = new DefaultEntityModel(Chinook.T_INVOICELINE, getEntityEditModel().getDbProvider());
    getEntityEditModel().addValueMapSetListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        lineModel.initialize(Chinook.T_INVOICE, Arrays.asList(getEntityEditModel().getEntityCopy()));
      }
    });

    final EntityTableModel lineTableModel = lineModel.getTableModel();
    lineTableModel.setDetailModel(true);
    lineTableModel.setQueryConfigurationAllowed(false);
    lineTableModel.getPropertySummaryModel(Chinook.INVOICELINE_UNITPRICE)
            .setSummaryType(PropertySummaryModel.SummaryType.MINIMUM_MAXIMUM);
    lineTableModel.getPropertySummaryModel(Chinook.INVOICELINE_TOTAL)
            .setSummaryType(PropertySummaryModel.SummaryType.SUM);
    lineTableModel.getPropertySummaryModel(Chinook.INVOICELINE_QUANTITY)
            .setSummaryType(PropertySummaryModel.SummaryType.SUM);

    final EntityTablePanel lineTablePanel = new EntityTablePanel(lineTableModel) {
      @Override
      protected JPanel initializeSouthPanel() {
        return null;
      }
    };
    //NB setColumnVis... must be called after table panel is initialized
    lineTableModel.setColumnVisible(Entities.getProperty(Chinook.T_INVOICELINE, Chinook.INVOICELINE_INVOICEID_FK), false);
    lineTablePanel.setSummaryPanelVisible(true);
    lineTablePanel.getJTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

    final EntityEditPanel lineEditPanel = new InvoiceLineEditPanel(lineModel.getEditModel());
    final EntityPanel linePanel = new EntityPanel(lineModel, lineEditPanel, lineTablePanel);

    linePanel.setEditPanelState(EntityPanel.HIDDEN);
    linePanel.initializePanel();
    linePanel.setPreferredSize(new Dimension(360, 50));
    linePanel.setBorder(BorderFactory.createTitledBorder("Invoice lines"));
    add(linePanel, BorderLayout.EAST);
  }
}