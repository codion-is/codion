package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;

import static is.codion.swing.common.ui.component.Components.panel;
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
    createTextField(CountryLanguage.PERCENTAGE)
            .columns(4);

    JPanel percentageOfficialPanel = panel(gridLayout(1, 2))
            .add(createInputPanel(CountryLanguage.PERCENTAGE))
            .add(createInputPanel(CountryLanguage.IS_OFFICIAL))
            .build();

    setLayout(gridLayout(0, 1));

    addInputPanel(CountryLanguage.COUNTRY_FK);
    addInputPanel(CountryLanguage.LANGUAGE);
    add(percentageOfficialPanel);
  }
}
