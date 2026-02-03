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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.Preferences;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

public final class PreferencesEditPanel extends EntityEditPanel {

	public PreferencesEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}

	@Override
	protected void initializeUI() {
		createSearchField(Preferences.CUSTOMER_FK)
						.columns(14);
		createComboBox(Preferences.PREFERRED_GENRE_FK)
						.preferredWidth(160);
		component(Preferences.NEWSLETTER).set(new TriStateCheckBoxBuilder())
						.altStateCycleOrder(true);

		// Automatically update without confirmation when newsletter is toggled
		editModel().editor().value(Preferences.NEWSLETTER)
						.edited().addListener(this::updateNewsletter);

		setLayout(flexibleGridLayout(3, 1));
		addInputPanel(Preferences.CUSTOMER_FK);
		addInputPanel(Preferences.PREFERRED_GENRE_FK);
		addInputPanel(Preferences.NEWSLETTER);
	}

	private void updateNewsletter() {
		// Only when we're editing an existing record
		if (editModel().editor().exists().is()) {
			updateCommand()
							.confirm(false)
							.execute();
		}
	}
}