package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

final class CountryLanguageEditPanel extends EntityEditPanel {

  CountryLanguageEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(CountryLanguage.COUNTRY_FK);

    createForeignKeyComboBox(CountryLanguage.COUNTRY_FK)
            .preferredWidth(120);
    createTextField(CountryLanguage.LANGUAGE);
    createCheckBox(CountryLanguage.IS_OFFICIAL);
    createTextField(CountryLanguage.PERCENTAGE);

    setLayout(gridLayout(0, 1));

    addInputPanel(CountryLanguage.COUNTRY_FK);
    addInputPanel(CountryLanguage.LANGUAGE);
    addInputPanel(CountryLanguage.IS_OFFICIAL);
    addInputPanel(CountryLanguage.PERCENTAGE);
  }
}
