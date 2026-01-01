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
package is.codion.swing.common.ui.layout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.*;

public final class FlexibleGridLayoutTest {

	private JPanel panel;

	@BeforeEach
	void setUp() {
		panel = new JPanel();
	}

	private Component createSizedLabel(int width, int height) {
		JLabel label = new JLabel("X");
		label.setPreferredSize(new Dimension(width, height));
		return label;
	}

	@Test
	void emptyContainer() {
		panel.setLayout(FlexibleGridLayout.builder().build());
		Dimension size = panel.getPreferredSize();
		assertEquals(0, size.width);
		assertEquals(0, size.height);
	}

	@Test
	void singleComponentNoHints() {
		panel.setLayout(FlexibleGridLayout.builder().build());
		panel.add(createSizedLabel(50, 30));
		Dimension size = panel.getPreferredSize();
		assertTrue(size.width > 0);
		assertTrue(size.height > 0);
	}

	@Test
	void unbalancedGrid2xN() {
		panel.setLayout(FlexibleGridLayout.builder().rows(2).horizontalGap(5).verticalGap(5).build());
		for (int i = 0; i < 5; i++) panel.add(createSizedLabel(50 + i * 10, 30));
		Dimension size = panel.getPreferredSize();
		assertTrue(size.width > 0);
		assertTrue(size.height > 0);
	}

	@Test
	void unbalancedGridNx2() {
		panel.setLayout(FlexibleGridLayout.builder().columns(2).horizontalGap(5).verticalGap(5).build());
		for (int i = 0; i < 5; i++) {
			panel.add(createSizedLabel(50, 20 + i * 5));
		}
		Dimension size = panel.getPreferredSize();
		assertTrue(size.width > 0);
		assertTrue(size.height > 0);
	}

	@Test
	void fixedRowHeight() {
		panel.setLayout(FlexibleGridLayout.builder().rows(2).fixRowHeights(true).build());
		panel.add(createSizedLabel(30, 10));
		panel.add(createSizedLabel(30, 50));
		panel.doLayout();
		Rectangle b1 = panel.getComponent(0).getBounds();
		Rectangle b2 = panel.getComponent(1).getBounds();
		assertEquals(b1.height, b2.height);
	}

	@Test
	void fixedColWidth() {
		panel.setLayout(FlexibleGridLayout.builder().columns(2).fixColumnWidths(true).build());
		panel.add(createSizedLabel(10, 30));
		panel.add(createSizedLabel(50, 30));
		panel.doLayout();
		Rectangle b1 = panel.getComponent(0).getBounds();
		Rectangle b2 = panel.getComponent(1).getBounds();
		assertEquals(b1.width, b2.width);
	}

	@Test
	void explicitFixedSizeValues() {
		panel.setLayout(FlexibleGridLayout.builder()
						.rows(1)
						.fixedRowHeight(40)
						.fixedColumnWidth(80)
						.build());
		panel.add(createSizedLabel(30, 20));
		panel.doLayout();
		Rectangle b = panel.getComponent(0).getBounds();
		assertEquals(40, b.height);
		assertEquals(80, b.width);
	}

	@Test
	void componentOverflow() {
		panel.setLayout(FlexibleGridLayout.builder().rows(1).build());
		for (int i = 0; i < 10; i++) {
			panel.add(createSizedLabel(20, 20));
		}
		assertDoesNotThrow(() -> panel.getPreferredSize());
	}

	@Test
	void componentUnderflow() {
		panel.setLayout(FlexibleGridLayout.builder().rows(4).columns(4).build());
		panel.add(createSizedLabel(20, 20));
		assertDoesNotThrow(() -> panel.getPreferredSize());
	}

	@Test
	void nonRectangularComponentSizes() {
		panel.setLayout(FlexibleGridLayout.builder().rows(2).columns(2).build());
		panel.add(createSizedLabel(20, 10));
		panel.add(createSizedLabel(40, 30));
		panel.add(createSizedLabel(10, 70));
		panel.add(createSizedLabel(60, 15));
		assertDoesNotThrow(() -> panel.getPreferredSize());
		panel.doLayout();
	}

	@Test
	void nestedLayouts() {
		JPanel inner = new JPanel();
		inner.setLayout(FlexibleGridLayout.builder().rows(1).columns(2).build());
		inner.add(createSizedLabel(50, 50));
		inner.add(createSizedLabel(30, 30));

		panel.setLayout(FlexibleGridLayout.builder().columns(1).build());
		panel.add(inner);

		assertDoesNotThrow(() -> {
			panel.getPreferredSize();
			panel.doLayout();
		});
	}
}
