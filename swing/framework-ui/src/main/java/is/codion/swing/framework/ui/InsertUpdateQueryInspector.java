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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.Database;
import is.codion.framework.db.EntityQueries;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.framework.ui.SelectQueryInspector.BasicFormatterImpl;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;

import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.scrollPane;
import static java.util.Objects.requireNonNull;

final class InsertUpdateQueryInspector extends JPanel {

	private final JTextArea insertTextArea = Components.textArea()
					.rowsColumns(30, 42)
					.editable(false)
					.onBuild(InsertUpdateQueryInspector::setMonospaceFont)
					.build();
	private final JTextArea updateTextArea = Components.textArea()
					.rowsColumns(30, 42)
					.editable(false)
					.onBuild(InsertUpdateQueryInspector::setMonospaceFont)
					.build();
	private final EntityQueries queries;
	private final EntityEditModel editModel;

	InsertUpdateQueryInspector(EntityEditModel editModel) {
		requireNonNull(editModel);
		this.queries = EntityQueries.instance()
						.orElseThrow(() -> new IllegalStateException("No EntityQueries instance available"))
						.create(Database.instance(), editModel.connectionProvider().connection().entities());
		this.editModel = editModel;
		this.editModel.editor().valueChanged().addListener(this::refreshQuery);
		initializeUI();
		refreshQuery();
	}

	private void refreshQuery() {
		insertTextArea.setText(createInsertQuery());
		updateTextArea.setText(createUpdateQuery());
	}

	private String createInsertQuery() {
		return BasicFormatterImpl.format(queries.insert(editModel.editor().getOrThrow()));
	}

	private String createUpdateQuery() {
		return BasicFormatterImpl.format(queries.update(editModel.editor().getOrThrow()));
	}

	private void initializeUI() {
		setLayout(new BorderLayout());
		add(gridLayoutPanel(2, 1)
						.add(scrollPane(insertTextArea).build())
						.add(scrollPane(updateTextArea).build())
						.build());
	}

	private static void setMonospaceFont(JTextArea textArea) {
		Font font = textArea.getFont();
		textArea.setFont(new Font(Font.MONOSPACED, font.getStyle(), font.getSize()));
	}
}
