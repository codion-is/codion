/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.ui;

import is.codion.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityEditor;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;

import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.util.Collections.singleton;

final class CountryLanguageEditPanel extends EntityEditPanel {

	CountryLanguageEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
		// Perform an update each time the IS_OFFICIAL
		// value is edited, bypassing the update confirmation
		editor().value(CountryLanguage.IS_OFFICIAL)
						.edited().addListener(this::updateIsOfficial);
	}

	@Override
	protected void initializeUI() {
		create().comboBox(CountryLanguage.COUNTRY_FK)
						.preferredWidth(120);
		create().textField(CountryLanguage.LANGUAGE);
		create().checkBox(CountryLanguage.IS_OFFICIAL);
		create().doubleField(CountryLanguage.PERCENTAGE)
						.range(0, 100)
						.silentValidation(true)
						.columns(4);
		create().textField(CountryLanguage.NO_OF_SPEAKERS)
						.columns(6);

		JPanel percentageOfficialPanel = gridLayoutPanel(1, 3)
						.add(create().inputPanel(CountryLanguage.PERCENTAGE))
						.add(create().inputPanel(CountryLanguage.IS_OFFICIAL))
						.add(create().inputPanel(CountryLanguage.NO_OF_SPEAKERS))
						.build();

		setLayout(gridLayout(0, 1));

		addInputPanel(CountryLanguage.COUNTRY_FK);
		addInputPanel(CountryLanguage.LANGUAGE);
		add(percentageOfficialPanel);
	}

	private void updateIsOfficial() {
		SwingEntityEditor editor = editor();
		//Only when IS_OFFICIAL is the only attribute being edited in an existing entity
		if (editor.modified().attributes().is(singleton(CountryLanguage.IS_OFFICIAL))) {
			try {
				updateCommand()
								.confirm(false)
								.execute();
			}
			catch (EntityValidationException e) {
				onValidationException(e);
			}
		}
	}
}
