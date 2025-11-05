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
import is.codion.common.utilities.Conjunction;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityQueries;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.EntityQueryModel;
import is.codion.swing.common.ui.component.Components;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.function.Supplier;

import static is.codion.framework.domain.entity.condition.Condition.combination;
import static is.codion.swing.common.ui.component.Components.scrollPane;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingUtilities.invokeLater;

final class SelectQueryInspector extends JPanel {

	private final JTextArea textArea = Components.textArea()
					.rowsColumns(30, 80)
					.editable(false)
					.font(SelectQueryInspector::monospaced)
					.build();
	private final EntityQueries queries;
	private final EntityQueryModel queryModel;

	SelectQueryInspector(EntityQueryModel queryModel) {
		requireNonNull(queryModel);
		this.queries = EntityQueries.factory()
						.orElseThrow(() -> new IllegalStateException("No EntityQueries.Factory available"))
						.create(Database.instance(), queryModel.connectionProvider().connection().entities());
		this.queryModel = queryModel;
		this.queryModel.condition().changed().addListener(this::refreshQuery);
		this.queryModel.limit().addListener(this::refreshQuery);
		this.queryModel.orderBy().addListener(this::refreshQuery);
		this.queryModel.attributes().included().addListener(this::refreshQuery);
		initializeUI();
		refreshQuery();
	}

	private void refreshQuery() {
		invokeLater(() -> {
			textArea.setText(createSelectQuery());
			textArea.setCaretPosition(0);
		});
	}

	private String createSelectQuery() {
		Condition where = createCondition(queryModel.condition().where(Conjunction.AND), queryModel.where());
		Condition having = createCondition(queryModel.condition().having(Conjunction.AND), queryModel.having());
		EntityConnection.Select select = EntityConnection.Select.where(where)
						.having(having)
						.attributes(queryModel.attributes().get())
						.limit(queryModel.limit().get())
						.orderBy(queryModel.orderBy().get())
						.build();

		return queries.select(select);
	}

	private void initializeUI() {
		setLayout(new BorderLayout());
		add(scrollPane()
						.view(textArea)
						.build());
	}

	private static Condition createCondition(Condition entityCondition, EntityQueryModel.AdditionalCondition additional) {
		return additional.optional()
						.map(Supplier::get)
						.map(condition -> combination(additional.conjunction().getOrThrow(), entityCondition, condition))
						.map(Condition.class::cast)
						.orElse(entityCondition);
	}

	private static Font monospaced(Font font) {
		return new Font(Font.MONOSPACED, font.getStyle(), font.getSize());
	}
}
