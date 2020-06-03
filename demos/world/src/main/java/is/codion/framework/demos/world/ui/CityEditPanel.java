package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.World.City;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.Components.setPreferredWidth;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class CityEditPanel extends EntityEditPanel {

  public CityEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(City.COUNTRY_FK);

    setPreferredWidth(createForeignKeyComboBox(City.COUNTRY_FK), 120);
    createTextField(City.NAME).setColumns(12);
    createTextField(City.DISTRICT).setColumns(12);
    createTextField(City.POPULATION);

    setLayout(gridLayout(2, 2));

    addPropertyPanel(City.COUNTRY_FK);
    addPropertyPanel(City.NAME);
    addPropertyPanel(City.DISTRICT);
    addPropertyPanel(City.POPULATION);
  }
}
