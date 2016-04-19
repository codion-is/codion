/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.beans.ui;

import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

public final class CityEditPanel extends EntityEditPanel {

  private static final int COMBOBOX_WIDTH = 120;
  private static final int TEXT_FIELD_COLUMNS = 12;

  public CityEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(World.CITY_COUNTRYCODE_FK);

    UiUtil.setPreferredWidth(createForeignKeyComboBox(World.CITY_COUNTRYCODE_FK), COMBOBOX_WIDTH);
    createTextField(World.CITY_NAME).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(World.CITY_DISTRICT).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(World.CITY_POPULATION);

    setLayout(new GridLayout(2, 2, 5, 5));

    addPropertyPanel(World.CITY_COUNTRYCODE_FK);
    addPropertyPanel(World.CITY_NAME);
    addPropertyPanel(World.CITY_DISTRICT);
    addPropertyPanel(World.CITY_POPULATION);
  }
}
