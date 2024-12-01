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
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.toolBar;
import static is.codion.swing.common.ui.component.text.TextComponents.preferredTextFieldHeight;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.INSERT;
import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.UPDATE;

public final class InvoiceLineEditPanel extends EntityEditPanel {

	private final JTextField tableSearchField;

	public InvoiceLineEditPanel(SwingEntityEditModel editModel, JTextField tableSearchField) {
		super(editModel);
		this.tableSearchField = tableSearchField;
		// We do not want the track to persist when the model is cleared.
		editModel.value(InvoiceLine.TRACK_FK).persist().set(false);
	}

	@Override
	protected void initializeUI() {
		initialFocusAttribute().set(InvoiceLine.TRACK_FK);

		createForeignKeySearchField(InvoiceLine.TRACK_FK)
						.selectorFactory(new TrackSelectorFactory())
						.columns(15);
		createTextField(InvoiceLine.QUANTITY)
						.selectAllOnFocusGained(true)
						.columns(2)
						.action(control(INSERT).get());

		JToolBar updateToolBar = toolBar()
						.floatable(false)
						.action(control(UPDATE).get())
						.preferredHeight(preferredTextFieldHeight())
						.build();

		JPanel centerPanel = flexibleGridLayoutPanel(1, 0)
						.add(createInputPanel(InvoiceLine.TRACK_FK))
						.add(createInputPanel(InvoiceLine.QUANTITY))
						.add(createInputPanel(new JLabel(" "), updateToolBar))
						.add(createInputPanel(new JLabel(" "), tableSearchField))
						.build();

		setLayout(borderLayout());
		add(centerPanel, BorderLayout.CENTER);
	}
}