/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.ui;

import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static org.jminor.swing.common.ui.Components.setPreferredWidth;
import static org.jminor.swing.common.ui.textfield.TextFields.makeUpperCase;

public final class CountryEditPanel extends EntityEditPanel {

  private static final int COMBOBOX_WIDTH = 120;
  private static final int TEXT_FIELD_COLUMNS = 12;

  public CountryEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(World.COUNTRY_CODE);

    makeUpperCase(createTextField(World.COUNTRY_CODE)).setColumns(TEXT_FIELD_COLUMNS);
    makeUpperCase(createTextField(World.COUNTRY_CODE2)).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(World.COUNTRY_NAME).setColumns(TEXT_FIELD_COLUMNS);
    setPreferredWidth(createPropertyComboBox(World.COUNTRY_CONTINENT), COMBOBOX_WIDTH);
    setPreferredWidth(createPropertyComboBox(World.COUNTRY_REGION), COMBOBOX_WIDTH);
    createTextField(World.COUNTRY_SURFACEAREA);
    createTextField(World.COUNTRY_INDEPYEAR);
    createTextField(World.COUNTRY_POPULATION);
    createTextField(World.COUNTRY_LIFEEXPECTANCY);
    createTextField(World.COUNTRY_GNP);
    createTextField(World.COUNTRY_GNPOLD);
    createTextField(World.COUNTRY_LOCALNAME).setColumns(TEXT_FIELD_COLUMNS);
    setPreferredWidth(createPropertyComboBox(World.COUNTRY_GOVERNMENTFORM), COMBOBOX_WIDTH);
    createTextField(World.COUNTRY_HEADOFSTATE).setColumns(TEXT_FIELD_COLUMNS);
    setPreferredWidth(createForeignKeyComboBox(World.COUNTRY_CAPITAL_FK), COMBOBOX_WIDTH);

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
