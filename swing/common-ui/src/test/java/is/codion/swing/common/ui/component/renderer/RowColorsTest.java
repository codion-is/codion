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
package is.codion.swing.common.ui.component.renderer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;
import java.awt.Color;

import static is.codion.swing.common.ui.color.Colors.shade;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class RowColorsTest {

	private static final String PREFIX = "RowColorsTest";

	private static final Color FOREGROUND = new Color(20, 20, 20);
	private static final Color BACKGROUND = new Color(250, 250, 250);
	private static final Color SELECTION_FOREGROUND = new Color(255, 255, 255);
	private static final Color SELECTION_BACKGROUND = new Color(40, 110, 190);
	private static final Color ITEM = new Color(255, 200, 200);

	@BeforeEach
	void setUp() {
		UIManager.put(PREFIX + ".foreground", FOREGROUND);
		UIManager.put(PREFIX + ".background", BACKGROUND);
		UIManager.put(PREFIX + ".selectionForeground", SELECTION_FOREGROUND);
		UIManager.put(PREFIX + ".selectionBackground", SELECTION_BACKGROUND);
	}

	@AfterEach
	void tearDown() {
		UIManager.put(PREFIX + ".foreground", null);
		UIManager.put(PREFIX + ".background", null);
		UIManager.put(PREFIX + ".alternateRowColor", null);
		UIManager.put(PREFIX + ".selectionForeground", null);
		UIManager.put(PREFIX + ".selectionBackground", null);
	}

	@Test
	void alternateRows() {
		RowColors colors = new RowColors(PREFIX, true, false);
		// shaded from the background of the component, or of the look and feel
		Color background = new Color(240, 240, 230);
		assertEquals(background, colors.background(background, 0));
		assertEquals(shade(background), colors.background(background, 1));
		assertEquals(BACKGROUND, colors.background(null, 0));
		assertEquals(shade(BACKGROUND), colors.background(null, 1));
		// an item background shaded as well
		assertEquals(ITEM, colors.background(null, 0, ITEM, false, null));
		assertEquals(shade(ITEM), colors.background(null, 1, ITEM, false, null));
		// and the selection
		assertEquals(SELECTION_BACKGROUND, colors.background(null, 0, null, true, null));
		assertEquals(shade(SELECTION_BACKGROUND), colors.background(null, 1, null, true, null));
	}

	@Test
	void alternateRowColor() {
		Color alternateRowColor = new Color(230, 240, 250);
		UIManager.put(PREFIX + ".alternateRowColor", alternateRowColor);
		// the alternate row color of the look and feel instead of the shade
		RowColors colors = new RowColors(PREFIX, true, false);
		assertEquals(alternateRowColor, colors.background(null, 1));
		// and respected without alternate row coloring
		colors = new RowColors(PREFIX, false, false);
		assertEquals(BACKGROUND, colors.background(null, 0));
		assertEquals(alternateRowColor, colors.background(null, 1));
		// neither the item background nor the selection being shaded
		assertEquals(ITEM, colors.background(null, 1, ITEM, false, null));
		assertEquals(SELECTION_BACKGROUND, colors.background(null, 1, null, true, null));
	}

	@Test
	void noAlternateRows() {
		RowColors colors = new RowColors(PREFIX, false, false);
		assertEquals(BACKGROUND, colors.background(null, 1));
		assertEquals(ITEM, colors.background(null, 1, ITEM, false, null));
		assertEquals(SELECTION_BACKGROUND, colors.background(null, 1, null, true, null));
	}

	@Test
	void itemBackgroundSelected() {
		// blended with the selection background
		RowColors colors = new RowColors(PREFIX, false, false);
		assertEquals(new Color(147, 155, 195), colors.background(null, 0, ITEM, true, null));
	}

	@Test
	void foreground() {
		RowColors colors = new RowColors(PREFIX, true, false);
		assertEquals(FOREGROUND, colors.foreground(null, false, null));
		assertEquals(Color.RED, colors.foreground(Color.RED, false, null));
		// the selection foreground wins over an item foreground
		assertEquals(SELECTION_FOREGROUND, colors.foreground(Color.RED, true, null));
		assertEquals(Color.GREEN, colors.foreground(Color.RED, true, Color.GREEN));
	}

	@Test
	void selectionColors() {
		RowColors colors = new RowColors(PREFIX, false, false);
		// set on the component
		assertEquals(Color.GREEN, colors.background(null, 0, null, true, Color.GREEN));
		// set by the look and feel, as FlatLaf's inactive ones, giving way to the default ones
		Color inactive = new ColorUIResource(Color.GRAY);
		assertEquals(SELECTION_BACKGROUND, colors.background(null, 0, null, true, inactive));
		assertEquals(SELECTION_FOREGROUND, colors.foreground(null, true, inactive));
		// unless enabled
		colors = new RowColors(PREFIX, false, true);
		assertEquals(inactive, colors.background(null, 0, null, true, inactive));
		assertEquals(inactive, colors.foreground(null, true, inactive));
	}

	@Test
	void noDefaultSelectionColors() {
		// as Nimbus has none for lists, the selection colors it sets on the list being used
		UIManager.put(PREFIX + ".selectionForeground", null);
		UIManager.put(PREFIX + ".selectionBackground", null);
		RowColors colors = new RowColors(PREFIX, false, false);
		Color selection = new ColorUIResource(Color.BLUE);
		assertEquals(selection, colors.background(null, 0, null, true, selection));
		assertEquals(selection, colors.foreground(null, true, selection));
	}

	@Test
	void plainColors() {
		// as Nimbus paints a renderer with its own colors in place of UIResource ones
		UIManager.put(PREFIX + ".foreground", new ColorUIResource(FOREGROUND));
		UIManager.put(PREFIX + ".background", new ColorUIResource(BACKGROUND));
		UIManager.put(PREFIX + ".selectionForeground", null);
		UIManager.put(PREFIX + ".selectionBackground", null);
		RowColors colors = new RowColors(PREFIX, false, false);
		Color selection = new ColorUIResource(Color.BLUE);
		assertPlain(selection, colors.background(null, 0, null, true, selection));
		assertPlain(selection, colors.foreground(null, true, selection));
		assertPlain(BACKGROUND, colors.background(null, 0));
		assertPlain(BACKGROUND, colors.background(new ColorUIResource(BACKGROUND), 0, null, false, null));
		assertPlain(ITEM, colors.background(null, 0, new ColorUIResource(ITEM), false, null));
		assertPlain(FOREGROUND, colors.foreground(null, false, null));
	}

	@Test
	void update() {
		RowColors colors = new RowColors(PREFIX, false, false);
		UIManager.put(PREFIX + ".background", Color.YELLOW);
		assertEquals(BACKGROUND, colors.background(null, 0));
		colors.update();
		assertEquals(Color.YELLOW, colors.background(null, 0));
	}

	private static void assertPlain(Color expected, Color color) {
		assertEquals(expected, color);
		assertFalse(color instanceof UIResource);
	}
}
