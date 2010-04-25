/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import static org.jminor.framework.demos.petstore.domain.Petstore.*;

import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 14:05:58
 */
public class AddressPanel extends EntityPanel {

  public AddressPanel(final EntityModel model) {
    super(model, "Address");
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel) {
    return new EntityEditPanel((EntityEditModel) editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new FlexibleGridLayout(4,2,5,5));
        JTextField txt = createTextField(ADDRESS_CITY);
        setDefaultFocusComponent(txt);
        txt.setColumns(12);
        add(createPropertyPanel(ADDRESS_CITY, txt));
        txt = createTextField(ADDRESS_STATE);
        txt.setColumns(12);
        add(createPropertyPanel(ADDRESS_STATE, txt));
        add(new JLabel());
        txt = createTextField(ADDRESS_ZIP);
        txt.setColumns(12);
        add(createPropertyPanel(ADDRESS_ZIP, txt));
        txt = createTextField(ADDRESS_STREET_1);
        txt.setColumns(12);
        add(createPropertyPanel(ADDRESS_STREET_1, txt));
        txt = createTextField(ADDRESS_STREET_2);
        txt.setColumns(12);
        add(createPropertyPanel(ADDRESS_STREET_2, txt));
        add(createPropertyPanel(ADDRESS_LATITUDE, createTextField(ADDRESS_LATITUDE)));
        add(createPropertyPanel(ADDRESS_LONGITUDE, createTextField(ADDRESS_LONGITUDE)));
      }
    };
  }
}