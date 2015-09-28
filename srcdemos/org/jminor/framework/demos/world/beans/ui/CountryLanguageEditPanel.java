/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.beans.ui;

import org.jminor.common.swing.ui.UiUtil;
import org.jminor.framework.demos.world.domain.World;
import org.jminor.framework.swing.model.EntityEditModel;
import org.jminor.framework.swing.ui.EntityEditPanel;

import java.awt.GridLayout;

public final class CountryLanguageEditPanel extends EntityEditPanel {

  private static final int COMBOBOX_WIDTH = 120;
  private static final int TEXT_FIELD_COLUMNS = 12;

  public CountryLanguageEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(World.COUNTRYLANGUAGE_COUNTRYCODE_FK);

    UiUtil.setPreferredWidth(createEntityComboBox(World.COUNTRYLANGUAGE_COUNTRYCODE_FK), COMBOBOX_WIDTH);
    createTextField(World.COUNTRYLANGUAGE_LANGUAGE).setColumns(TEXT_FIELD_COLUMNS);
    createCheckBox(World.COUNTRYLANGUAGE_ISOFFICIAL, null, false);
    createTextField(World.COUNTRYLANGUAGE_PERCENTAGE);

    setLayout(new GridLayout(2, 4, 5, 5));

    addPropertyPanel(World.COUNTRYLANGUAGE_COUNTRYCODE_FK);
    addPropertyPanel(World.COUNTRYLANGUAGE_LANGUAGE);
    addPropertyPanel(World.COUNTRYLANGUAGE_ISOFFICIAL);
    addPropertyPanel(World.COUNTRYLANGUAGE_PERCENTAGE);
  }
}
