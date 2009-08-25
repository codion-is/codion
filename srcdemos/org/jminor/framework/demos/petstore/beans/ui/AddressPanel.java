/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.demos.petstore.domain.Petstore;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 14:05:58
 */
public class AddressPanel extends EntityPanel {

  public AddressPanel(final EntityModel model) {
    super(model, "Address");
  }

  /** {@inheritDoc} */
  @Override
  protected JPanel initializePropertyPanel() {
    final JPanel ret = new JPanel(new FlexibleGridLayout(4,2,5,5));
    JTextField txt = createTextField(Petstore.ADDRESS_CITY);
    setDefaultFocusComponent(txt);
    txt.setColumns(12);
    ret.add(createControlPanel(Petstore.ADDRESS_CITY, txt));
    txt = createTextField(Petstore.ADDRESS_STATE);
    txt.setColumns(12);
    ret.add(createControlPanel(Petstore.ADDRESS_STATE, txt));
    ret.add(new JLabel());
    txt = createTextField(Petstore.ADDRESS_ZIP);
    txt.setColumns(12);
    ret.add(createControlPanel(Petstore.ADDRESS_ZIP, txt));
    txt = createTextField(Petstore.ADDRESS_STREET_1);
    txt.setColumns(12);
    ret.add(createControlPanel(Petstore.ADDRESS_STREET_1, txt));
    txt = createTextField(Petstore.ADDRESS_STREET_2);
    txt.setColumns(12);
    ret.add(createControlPanel(Petstore.ADDRESS_STREET_2, txt));
    ret.add(createControlPanel(Petstore.ADDRESS_LATITUDE, createTextField(Petstore.ADDRESS_LATITUDE)));
    ret.add(createControlPanel(Petstore.ADDRESS_LONGITUDE, createTextField(Petstore.ADDRESS_LONGITUDE)));

    return ret;
  }
}