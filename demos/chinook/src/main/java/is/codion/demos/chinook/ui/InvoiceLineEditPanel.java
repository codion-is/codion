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

import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.INSERT;
import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.UPDATE;

public final class InvoiceLineEditPanel extends EntityEditPanel {

	private final JTextField tableSearchField;

	public InvoiceLineEditPanel(SwingEntityEditModel editModel, JTextField tableSearchField) {
		super(editModel, config ->
						// Update without confirmation
						config.confirmUpdate(false));
		this.tableSearchField = tableSearchField;
		// We do not want the track to persist when the model is cleared.
		editModel.editor().value(InvoiceLine.TRACK_FK).persist().set(false);
	}

	@Override
	protected void initializeUI() {
		createSearchField(InvoiceLine.TRACK_FK)
						.selector(new TrackSelector())
						.columns(15);
		createTextField(InvoiceLine.QUANTITY)
						.selectAllOnFocusGained(true)
						.columns(2)
						// Set the INSERT control as the quantity field
						// action, triggering insert on Enter
						.action(control(INSERT).get());

		JButton updateButton = button()
						.control(control(UPDATE).get())
						.includeText(false)
						.focusable(false)
						.build();

		JPanel centerPanel = flexibleGridLayoutPanel(1, 0)
						.add(createInputPanel(InvoiceLine.TRACK_FK))
						.add(createInputPanel(InvoiceLine.QUANTITY))
						.add(borderLayoutPanel()
										.north(label(" "))
										.center(updateButton))
						.add(borderLayoutPanel()
										.north(label(" "))
										.center(tableSearchField))
						.build();

		setLayout(borderLayout());
		add(centerPanel, BorderLayout.CENTER);
	}
}