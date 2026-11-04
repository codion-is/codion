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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.list;

import is.codion.common.utilities.Text;
import is.codion.swing.common.model.component.list.SwingFilterListModel;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.text.Position;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.IntStream;

import static is.codion.swing.common.ui.color.Colors.shade;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

final class FilterListTest {

	private static final String ONE = "One";
	private static final String TWO = "Two";
	private static final String THREE = "Three";

	@Test
	void test() {
		List<String> items = asList(ONE, TWO, THREE);
		SwingFilterListModel<String> model = SwingFilterListModel.builder()
						.items(items)
						.comparator(Text.collator())
						.build();
		model.selection().item().set(TWO);
		FilterList<String> list = FilterList.builder().model(model).items().build();
		assertEquals(TWO, list.getSelectedValue());
		assertEquals(1, list.model().items().included().indexOf(THREE));
		assertThrows(IllegalStateException.class, () -> list.setSelectionModel(new DefaultListSelectionModel()));
		assertThrows(IllegalStateException.class, () -> list.setModel(new DefaultListModel<>()));
	}

	@Test
	void colors() {
		FilterList<String> list = FilterList.builder()
						.model(createModel())
						.items()
						.background(item -> item.equals(THREE) ? Color.PINK : null)
						.foreground(item -> item.equals(THREE) ? Color.RED : null)
						.build();
		list.setBackground(Color.WHITE);
		list.setSelectionBackground(Color.GREEN);
		list.setSelectionForeground(Color.MAGENTA);
		// alternate rows shaded from the list background
		assertEquals(Color.WHITE, render(list, 0).getBackground());
		assertEquals(shade(Color.WHITE), render(list, 1).getBackground());
		// an item background, shaded on an alternate row
		assertEquals(Color.PINK, render(list, 2).getBackground());
		assertEquals(Color.RED, render(list, 2).getForeground());
		// the selection colors of the list, shaded on an alternate row
		list.setSelectedIndices(new int[] {0, 1});
		assertEquals(Color.GREEN, render(list, 0).getBackground());
		assertEquals(shade(Color.GREEN), render(list, 1).getBackground());
		assertEquals(Color.MAGENTA, render(list, 0).getForeground());
		// an item background blended with the selection, the selection foreground winning
		list.setSelectedIndex(2);
		assertEquals(new Color(127, 214, 87), render(list, 2).getBackground());
		assertEquals(Color.MAGENTA, render(list, 2).getForeground());

		list = FilterList.builder()
						.model(createModel())
						.items()
						.alternateRowColoring(false)
						.build();
		list.setBackground(Color.WHITE);
		assertEquals(Color.WHITE, render(list, 1).getBackground());
	}

	@Test
	void formatter() {
		FilterList<String> list = FilterList.builder()
						.model(createModel())
						.items()
						.formatter(item -> "Item " + item)
						.toolTip(item -> item.equals(TWO) ? "The second" : null)
						.build();
		assertEquals("Item One", render(list, 0).getText());
		assertNull(render(list, 0).getToolTipText());
		assertEquals("The second", render(list, 1).getToolTipText());
		// the prefix matched against the formatted items
		assertEquals(1, list.getNextMatch("item t", 0, Position.Bias.Forward));
		assertEquals(-1, list.getNextMatch("t", 0, Position.Bias.Forward));
		assertEquals(2, list.getNextMatch("item t", 2, Position.Bias.Forward));
		assertEquals(2, list.getNextMatch("item t", 0, Position.Bias.Backward));
		assertThrows(IllegalArgumentException.class, () -> list.getNextMatch("item", 3, Position.Bias.Forward));

		// the items themselves without a formatter
		FilterList<String> unformatted = FilterList.builder()
						.model(createModel())
						.items()
						.build();
		assertEquals(ONE, render(unformatted, 0).getText());
		assertEquals(1, unformatted.getNextMatch("t", 0, Position.Bias.Forward));

		// a null formatter replacing one specified, the default again
		FilterList<String> reset = FilterList.builder()
						.model(createModel())
						.items()
						.formatter(item -> "Item " + item)
						.formatter(null)
						.build();
		assertEquals(ONE, render(reset, 0).getText());
		assertEquals(1, reset.getNextMatch("t", 0, Position.Bias.Forward));
	}

	@Test
	void cellRenderer() {
		ListCellRenderer<Object> renderer = new DefaultListCellRenderer();
		FilterList<String> list = FilterList.builder()
						.model(createModel())
						.items()
						.cellRenderer(renderer)
						.formatter(item -> "Item " + item)
						.build();
		assertSame(renderer, list.getCellRenderer());
		// the formatter still used for matching the prefix
		assertEquals(1, list.getNextMatch("item t", 0, Position.Bias.Forward));
	}

	@Test
	void updateUI() {
		FilterList<String> list = FilterList.builder()
						.model(createModel())
						.items()
						.build();
		ListCellRenderer<? super String> renderer = list.getCellRenderer();
		list.updateUI();
		// not replaced by the look and feel renderer
		assertSame(renderer, list.getCellRenderer());
	}

	@Test
	void rowsFillViewport() {
		FilterList<String> list = FilterList.builder()
						.model(createModel())
						.items()
						.rowsFillViewport(true)
						.build();
		list.setBackground(Color.WHITE);
		BufferedImage image = paint(list);
		Rectangle last = list.getCellBounds(2, 2);
		int y = last.y + last.height;
		// the rows below the items, the first one an alternate row
		assertEquals(shade(Color.WHITE).getRGB(), image.getRGB(1, y + 1));
		assertEquals(Color.WHITE.getRGB(), image.getRGB(1, y + last.height + 1));
		assertEquals(shade(Color.WHITE).getRGB(), image.getRGB(1, y + 2 * last.height + 1));

		list = FilterList.builder()
						.model(createModel())
						.items()
						.build();
		list.setBackground(Color.WHITE);
		image = paint(list);
		assertEquals(Color.WHITE.getRGB(), image.getRGB(1, y + 1));
	}

	@Test
	void scrollToSelected() {
		List<String> items = IntStream.range(0, 100)
						.mapToObj(i -> String.format("%02d", i))
						.collect(toList());
		SwingFilterListModel<String> model = SwingFilterListModel.builder()
						.items(items)
						.comparator(Text.collator())
						.build();
		FilterList<String> list = FilterList.builder()
						.model(model)
						.items()
						.build();
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setSize(200, 100);
		scrollPane.doLayout();
		list.setSize(list.getPreferredSize());
		scrollPane.getViewport().doLayout();
		JViewport viewport = scrollPane.getViewport();
		assertTrue(visible(list, 0));
		assertFalse(visible(list, 50));

		// Extending the selection downwards follows it, the first selected scrolled out of view
		list.setSelectedIndex(0);
		for (int i = 0; i < 30; i++) {
			action(list, "selectNextRowExtendSelection");
		}
		assertEquals(31, list.getSelectedIndices().length);
		assertTrue(visible(list, 30));
		assertFalse(visible(list, 0));
		// by the page as well
		int lead = list.getLeadSelectionIndex();
		action(list, "scrollDownExtendSelection");
		assertTrue(list.getLeadSelectionIndex() > lead);
		assertTrue(visible(list, list.getLeadSelectionIndex()));
		assertFalse(visible(list, 0));

		// Upwards, from the bottom, as it always did
		list.setSelectedIndex(99);
		assertTrue(visible(list, 99));
		for (int i = 0; i < 30; i++) {
			action(list, "selectPreviousRowExtendSelection");
		}
		assertTrue(visible(list, 69));
		assertFalse(visible(list, 99));

		// Added, out of view, another one selected in view
		list.setSelectedIndex(0);
		assertTrue(visible(list, 0));
		model.selection().indexes().add(50);
		assertTrue(visible(list, 50));
		assertFalse(visible(list, 0));
		// Several added, one of them in view, is no reason to scroll
		Point position = viewport.getViewPosition();
		model.selection().indexes().set(asList(0, 49, 50, 80));
		assertEquals(position, viewport.getViewPosition());

		// Removed, is no reason to scroll to those still selected
		list.ensureIndexIsVisible(30);
		position = viewport.getViewPosition();
		model.selection().indexes().remove(80);
		assertEquals(position, viewport.getViewPosition());

		// Narrowing the selection to one of those selected adds nothing, so no reason either
		list.setSelectedIndex(0);
		assertEquals(position, viewport.getViewPosition());

		// A sort moving the selected ones is
		list.ensureIndexIsVisible(0);
		assertTrue(visible(list, 0));
		model.sort().descending();
		assertEquals(99, list.getSelectedIndex());
		assertTrue(visible(list, 99));
	}

	@Test
	void scrollToSelectedDisabled() {
		SwingFilterListModel<String> model = SwingFilterListModel.builder()
						.items(IntStream.range(0, 100)
										.mapToObj(i -> String.format("%02d", i))
										.collect(toList()))
						.build();
		// Deliberately not setting scrollToSelected, pinning its default to true
		assertTrue(FilterList.builder()
						.model(model)
						.items()
						.build()
						.scrollTo().selected().is());

		FilterList<String> list = FilterList.builder()
						.model(model)
						.items()
						.scrollToSelected(false)
						.build();
		assertFalse(list.scrollTo().selected().is());
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setSize(200, 100);
		scrollPane.doLayout();
		list.setSize(list.getPreferredSize());
		scrollPane.getViewport().doLayout();

		// Disabled, an item added out of view is not scrolled to
		model.selection().indexes().set(asList(50));
		assertEquals(0, scrollPane.getViewport().getViewPosition().y);

		// The look and feel still scrolls for the keyboard
		action(list, "selectNextRow");
		assertTrue(visible(list, 51));

		// Enabled while displayed
		list.ensureIndexIsVisible(0);
		list.scrollTo().selected().set(true);
		model.selection().indexes().set(asList(80));
		assertTrue(visible(list, 80));
	}

	private static boolean visible(FilterList<?> list, int index) {
		return list.getVisibleRect().contains(list.getCellBounds(index, index));
	}

	private static void action(FilterList<?> list, String name) {
		list.getActionMap().get(name).actionPerformed(new ActionEvent(list, ActionEvent.ACTION_PERFORMED, name));
	}

	private static SwingFilterListModel<String> createModel() {
		return SwingFilterListModel.builder()
						.items(asList(ONE, TWO, THREE))
						.build();
	}

	private static JLabel render(FilterList<String> list, int index) {
		return (JLabel) list.getCellRenderer().getListCellRendererComponent(list,
						list.getModel().getElementAt(index), index, list.isSelectedIndex(index), false);
	}

	private static BufferedImage paint(FilterList<String> list) {
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setSize(200, 300);
		scrollPane.doLayout();
		scrollPane.getViewport().doLayout();
		assertTrue(list.getHeight() > list.getPreferredSize().height);
		BufferedImage image = new BufferedImage(list.getWidth(), list.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		list.paint(graphics);
		graphics.dispose();

		return image;
	}
}
