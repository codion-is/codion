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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.test;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A base class for testing a {@link EntityEditPanel}
 */
public class EntityEditPanelTestUnit {

	private final SwingEntityEditModel editModel;
	private final Function<SwingEntityEditModel, EntityEditPanel> editPanel;

	/**
	 * Instantiates a new edit panel test unit for the given edit panel
	 * @param editModel the edit model
	 * @param editPanel provides the edit panel to test
	 */
	protected EntityEditPanelTestUnit(SwingEntityEditModel editModel,
																		Function<SwingEntityEditModel, EntityEditPanel> editPanel) {
		this.editModel = requireNonNull(editModel);
		this.editPanel = requireNonNull(editPanel);
	}

	/**
	 * Initializes the edit panel.
	 */
	protected final void testInitialize() {
		editPanel.apply(editModel).initialize();
	}

	/**
	 * @return the edit model
	 */
	protected final SwingEntityEditModel editModel() {
		return editModel;
	}
}
