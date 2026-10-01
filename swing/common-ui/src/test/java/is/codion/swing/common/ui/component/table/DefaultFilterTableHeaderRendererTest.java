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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.common.model.filter.SortOrder;
import is.codion.swing.common.model.component.table.SwingFilterTableModel;

import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static is.codion.swing.common.ui.component.table.FilterTableHeaderRenderer.FOCUSED_COLUMN_INDICATOR;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DefaultFilterTableHeaderRendererTest {

	@Test
	void focusedColumnShade() {
		FOCUSED_COLUMN_INDICATOR.set(true);
		try {
			FilterTable<String, Integer> table = createTable();
			table.setRowSelectionInterval(0, 0);
			table.getColumnModel().getSelectionModel().setSelectionInterval(1, 1);
			// darker when light
			Color light = new Color(230, 230, 230);
			table.getTableHeader().setBackground(light);
			assertEquals(light, headerBackground(table, 0));
			assertShaded(light, headerBackground(table, 1), false);
			// and lighter when dark
			Color dark = new Color(40, 44, 52);
			table.getTableHeader().setBackground(dark);
			assertEquals(dark, headerBackground(table, 0));
			assertShaded(dark, headerBackground(table, 1), true);
		}
		finally {
			FOCUSED_COLUMN_INDICATOR.set(false);
		}
	}

	@Test
	void sortArrows() {
		FilterTable<String, Integer> table = createTable();
		table.model().sort().ascending(0);
		table.model().sort().order(1).add(SortOrder.DESCENDING);
		for (int column = 0; column < 2; column++) {
			// shaded from the header background, darker when light and lighter when dark
			assertArrow(sortArrow(table, column), new Color(250, 250, 250), false);
			assertArrow(sortArrow(table, column), new Color(40, 44, 52), true);
		}
	}

	@Test
	void sortArrowsAligned() {
		FilterTable<String, Integer> table = createTable();
		table.model().sort().ascending(0);
		table.model().sort().order(1).add(SortOrder.DESCENDING);
		// the primary arrow centered vertically
		int[] primary = paintedRows(sortArrow(table, 0));
		assertEquals((sortArrow(table, 0).getIconHeight() - 1) / 2.0, (primary[0] + primary[1]) / 2.0, 1.0);
		// the smaller secondary one, pointing down, aligned with its bottom
		int[] secondary = paintedRows(sortArrow(table, 1));
		assertTrue(secondary[1] - secondary[0] < primary[1] - primary[0]);
		assertEquals(primary[1], secondary[1]);
	}

	/**
	 * @return the topmost and bottommost painted rows of the given arrow
	 */
	private static int[] paintedRows(Icon arrow) {
		JLabel label = new JLabel();
		label.setBackground(Color.WHITE);
		BufferedImage image = new BufferedImage(arrow.getIconWidth(), arrow.getIconHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		arrow.paintIcon(label, graphics, 0, 0);
		graphics.dispose();
		int top = image.getHeight();
		int bottom = -1;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if (image.getRGB(x, y) != Color.WHITE.getRGB()) {
					top = Math.min(top, y);
					bottom = Math.max(bottom, y);
				}
			}
		}

		return new int[] {top, bottom};
	}

	private static Icon sortArrow(FilterTable<?, ?> table, int column) {
		TableColumn tableColumn = table.getColumnModel().getColumn(column);

		return ((JLabel) tableColumn.getHeaderRenderer()
						.getTableCellRendererComponent(table, tableColumn.getHeaderValue(), false, false, -1, column))
						.getIcon();
	}

	private static void assertArrow(Icon arrow, Color background, boolean lighter) {
		JLabel label = new JLabel();
		label.setBackground(background);
		BufferedImage image = new BufferedImage(arrow.getIconWidth(), arrow.getIconHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(background);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		arrow.paintIcon(label, graphics, 0, 0);
		graphics.dispose();
		int painted = 0;
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				Color pixel = new Color(image.getRGB(x, y));
				if (!pixel.equals(background)) {
					assertShaded(background, pixel, lighter);
					painted++;
				}
			}
		}
		assertTrue(painted > 0);
	}

	private static Color headerBackground(FilterTable<?, ?> table, int column) {
		TableColumn tableColumn = table.getColumnModel().getColumn(column);

		return tableColumn.getHeaderRenderer()
						.getTableCellRendererComponent(table, tableColumn.getHeaderValue(), false, false, -1, column)
						.getBackground();
	}

	private static void assertShaded(Color color, Color shaded, boolean lighter) {
		int[] components = {color.getRed(), color.getGreen(), color.getBlue()};
		int[] shadedComponents = {shaded.getRed(), shaded.getGreen(), shaded.getBlue()};
		for (int i = 0; i < components.length; i++) {
			assertTrue(lighter ? shadedComponents[i] > components[i] : shadedComponents[i] < components[i]);
		}
	}

	private static FilterTable<String, Integer> createTable() {
		SwingFilterTableModel<String, Integer> model = SwingFilterTableModel.<String, Integer>builder()
						.columns(new TableColumns<String, Integer>() {
							@Override
							public List<Integer> identifiers() {
								return asList(0, 1);
							}

							@Override
							public Class<?> type(Integer identifier) {
								return String.class;
							}

							@Override
							public Object value(String row, Integer identifier) {
								return row;
							}
						})
						.items(() -> asList("a", "b"))
						.build();
		model.items().refresh();

		return FilterTable.builder()
						.model(model)
						.build();
	}
}
