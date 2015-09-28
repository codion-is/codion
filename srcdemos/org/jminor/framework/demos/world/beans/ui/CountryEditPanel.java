/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.beans.ui;

import org.jminor.common.swing.ui.UiUtil;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.swing.model.EntityEditModel;
import org.jminor.framework.swing.ui.EntityEditPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;

public final class CountryEditPanel extends EntityEditPanel {

  private static final int COMBOBOX_WIDTH = 120;
  private static final int TEXT_FIELD_COLUMNS = 12;

  public CountryEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(World.COUNTRY_CODE);

    final JTextField txtCode = createTextField(World.COUNTRY_CODE);
    txtCode.setColumns(TEXT_FIELD_COLUMNS);
    UiUtil.makeUpperCase(txtCode);
    final JTextField txtCode2 = createTextField(World.COUNTRY_CODE2);
    txtCode2.setColumns(TEXT_FIELD_COLUMNS);
    UiUtil.makeUpperCase(txtCode2);
    createTextField(World.COUNTRY_NAME).setColumns(TEXT_FIELD_COLUMNS);
    UiUtil.setPreferredWidth(createPropertyComboBox(World.COUNTRY_CONTINENT), COMBOBOX_WIDTH);
    UiUtil.setPreferredWidth(createPropertyComboBox(World.COUNTRY_REGION), COMBOBOX_WIDTH);
    createTextField(World.COUNTRY_SURFACEAREA);
    createTextField(World.COUNTRY_INDEPYEAR);
    createTextField(World.COUNTRY_POPULATION);
    createTextField(World.COUNTRY_LIFEEXPECTANCY);
    createTextField(World.COUNTRY_GNP);
    createTextField(World.COUNTRY_GNPOLD);
    createTextField(World.COUNTRY_LOCALNAME).setColumns(TEXT_FIELD_COLUMNS);
    UiUtil.setPreferredWidth(createPropertyComboBox(World.COUNTRY_GOVERNMENTFORM), COMBOBOX_WIDTH);
    createTextField(World.COUNTRY_HEADOFSTATE).setColumns(TEXT_FIELD_COLUMNS);
    UiUtil.setPreferredWidth(createEntityComboBox(World.COUNTRY_CAPITAL_FK), COMBOBOX_WIDTH);

    setLayout(new GridLayout(4, 5, 5, 5));

    addPropertyPanel(World.COUNTRY_CODE);
    addPropertyPanel(World.COUNTRY_CODE2);
    addPropertyPanel(World.COUNTRY_NAME);
    addPropertyPanel(World.COUNTRY_CONTINENT);
    addPropertyPanel(World.COUNTRY_REGION);
    addPropertyPanel(World.COUNTRY_SURFACEAREA);
    addPropertyPanel(World.COUNTRY_INDEPYEAR);
    addPropertyPanel(World.COUNTRY_POPULATION);
    addPropertyPanel(World.COUNTRY_LIFEEXPECTANCY);
    addPropertyPanel(World.COUNTRY_GNP);
    addPropertyPanel(World.COUNTRY_GNPOLD);
    addPropertyPanel(World.COUNTRY_LOCALNAME);
    addPropertyPanel(World.COUNTRY_GOVERNMENTFORM);
    addPropertyPanel(World.COUNTRY_HEADOFSTATE);
    addPropertyPanel(World.COUNTRY_CAPITAL_FK);
  }
}
