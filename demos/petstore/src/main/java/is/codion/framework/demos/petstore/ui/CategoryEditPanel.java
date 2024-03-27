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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.petstore.domain.Petstore.Category;

public class CategoryEditPanel extends EntityEditPanel {

	public CategoryEditPanel(SwingEntityEditModel model) {
		super(model);
	}

	@Override
	protected void initializeUI() {
		initialFocusAttribute().set(Category.NAME);

		createTextField(Category.NAME);
		createTextField(Category.DESCRIPTION).columns(18);
		createTextField(Category.IMAGE_URL);

		setLayout(Layouts.flexibleGridLayout(2, 2));
		addInputPanel(Category.NAME);
		addInputPanel(Category.DESCRIPTION);
		addInputPanel(Category.IMAGE_URL);
	}
}
