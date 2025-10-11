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
package is.codion.plugin.swing.robot;

import is.codion.plugin.swing.robot.Controller.KeyStrokeDescription;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.frame.Frames;

import org.jspecify.annotations.Nullable;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static java.util.Arrays.asList;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS;

/**
 * An automation narrator to use alongside {@link Controller}.
 */
public final class Narrator {

	private final JTextArea narration = textArea()
					.rowsColumns(5, 20)
					.editable(false)
					.lineWrap(true)
					.wrapStyleWord(true)
					.build();
	private final FilterTableModel<KeyStrokeDescription, Integer> keyStrokeModel =
					FilterTableModel.builder()
									.columns(new KeyStrokeColumns())
									.build();
	private final FilterTable<KeyStrokeDescription, Integer> keyStrokeTable =
					FilterTable.builder()
									.model(keyStrokeModel)
									.columns(Narrator::configureColumns)
									.autoResizeMode(AUTO_RESIZE_ALL_COLUMNS)
									.build();
	private final JFrame frame;

	Narrator(Controller controller, Window window) {
		frame = Frames.builder()
						.title("Narrator")
						.component(createMainPanel())
						.size(new Dimension(400, window.getHeight()))
						.location(new Point(window.getX() + window.getWidth(), window.getY()))
						.focusableWindowState(false)
						.show();
		controller.key().addConsumer(this::onKeyStroke);
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});
	}

	/**
	 * @param text the narration text to display
	 */
	public void narrate(String text) {
		narration.setText("");
		if (text != null) {
			narration.append(text);
		}
	}

	/**
	 * Clears the narration text.
	 */
	public void clearNarration() {
		narration.setText("");
	}

	/**
	 * Clears the keystrokes table.
	 */
	public void clearKeyStrokes() {
		keyStrokeTable.model().items().clear();
	}

	/**
	 * Clears both narration and keystrokes
	 */
	public void clear() {
		clearNarration();
		clearKeyStrokes();
	}

	/**
	 * Closes this narrator along with its frame
	 */
	void close() {
		frame.dispose();
	}

	private JPanel createMainPanel() {
		return borderLayoutPanel()
						.border(emptyBorder())
						.north(borderLayoutPanel()
										.border(createTitledBorder("Narration"))
										.center(narration))
						.center(borderLayoutPanel()
										.border(createTitledBorder("Keystrokes"))
										.center(scrollPane()
														.view(keyStrokeTable)))
						.build();
	}

	private void onKeyStroke(KeyStrokeDescription keyStroke) {
		keyStrokeTable.model().items().included().add(0, keyStroke);
		keyStrokeTable.model().selection().index().set(0);
	}

	private static void configureColumns(FilterTableColumn.Builder<Integer> column) {
		if (column.identifier() == 0) {
			column.fixedWidth(140);
		}
	}

	private static final class KeyStrokeColumns implements TableColumns<KeyStrokeDescription, Integer> {

		private static final List<Integer> IDENTIFIERS = asList(0, 1);

		@Override
		public List<Integer> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public String caption(Integer identifier) {
			if (identifier == 0) {
				return "Keystroke";
			}
			else if (identifier == 1) {
				return "Description";
			}

			throw new IllegalArgumentException();
		}

		@Override
		public Class<?> columnClass(Integer identifier) {
			return String.class;
		}

		@Override
		public @Nullable Object value(KeyStrokeDescription row, Integer identifier) {
			if (identifier == 0) {
				return row.keyStroke();
			}
			else if (identifier == 1) {
				return row.description();
			}

			throw new IllegalArgumentException();
		}
	}
}
