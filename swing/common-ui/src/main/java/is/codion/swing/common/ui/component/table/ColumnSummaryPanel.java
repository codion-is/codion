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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.summary.SummaryModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.util.Objects.requireNonNull;

/**
 * A panel that shows a summary value for a numerical column property.
 * For instances use the {@link #columnSummaryPanel(SummaryModel, int)} factory method.
 */
public final class ColumnSummaryPanel extends JPanel {

	private ColumnSummaryPanel(SummaryModel model, int horizontalAlignment) {
		setLayout(new BorderLayout());
		add(createSummaryField(requireNonNull(model), horizontalAlignment), BorderLayout.CENTER);
	}

	/**
	 * @param summaryModel the {@link SummaryModel} instance
	 * @param horizontalAlignment the horizontal alignment
	 * @return a new {@link ColumnSummaryPanel} instance.
	 */
	public static ColumnSummaryPanel columnSummaryPanel(SummaryModel summaryModel, int horizontalAlignment) {
		return new ColumnSummaryPanel(summaryModel, horizontalAlignment);
	}

	private static JTextField createSummaryField(SummaryModel model, int horizontalAlignment) {
		JPopupMenu popupMenu = createPopupMenu(model);
		return Components.stringField()
						.link(model.summaryText())
						.horizontalAlignment(horizontalAlignment)
						.editable(false)
						.focusable(false)
						.popupMenu(summaryField -> popupMenu)
						.mouseListener(new MouseAdapter() {
							@Override
							public void mouseReleased(MouseEvent e) {
								if (!model.locked().get()) {
									popupMenu.show(e.getComponent(), e.getX(), e.getY() - popupMenu.getPreferredSize().height);
								}
							}
						})
						.build();
	}

	private static JPopupMenu createPopupMenu(SummaryModel model) {
		JPopupMenu popupMenu = new JPopupMenu();
		ButtonGroup group = new ButtonGroup();
		for (SummaryModel.Summary summary : model.summaries()) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(Control.builder()
							.command(() -> model.summary().set(summary))
							.caption(summary.toString())
							.build());
			model.summary().addConsumer(newSummary -> item.setSelected(newSummary.equals(summary)));
			item.setSelected(model.summary().getOrThrow().equals(summary));
			group.add(item);
			popupMenu.add(item);
		}

		return popupMenu;
	}
}
