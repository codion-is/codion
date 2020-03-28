package org.jminor.framework.demos.world.ui;

import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import static org.jminor.swing.common.ui.Components.setPreferredWidth;
import static org.jminor.swing.common.ui.layout.Layouts.gridLayout;

public final class CityEditPanel extends EntityEditPanel {

  public CityEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(World.CITY_COUNTRY_FK);

    setPreferredWidth(createForeignKeyComboBox(World.CITY_COUNTRY_FK), 120);
    createTextField(World.CITY_NAME).setColumns(12);
    createTextField(World.CITY_DISTRICT).setColumns(12);
    createTextField(World.CITY_POPULATION);

    setLayout(gridLayout(2, 2));

    addPropertyPanel(World.CITY_COUNTRY_FK);
    addPropertyPanel(World.CITY_NAME);
    addPropertyPanel(World.CITY_DISTRICT);
    addPropertyPanel(World.CITY_POPULATION);
  }
}
