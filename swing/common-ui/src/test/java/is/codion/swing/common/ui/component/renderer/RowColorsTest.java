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
import static org.junit.jupiter.api.Assertions.*;

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
		UIManager.put(PREFIX + ".selectionInactiveForeground", null);
		UIManager.put(PREFIX + ".selectionInactiveBackground", null);
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

	@Test
	void distinctSelection() {
		// Akusan Dark, its selection a shade lighter than the background, as light as the alternate rows
		theme("#1e1e1e", "#303030", "#ebdbb2", "#262626", "#ebdbb2");
		RowColors colors = new RowColors(PREFIX, true, false);
		Color selection = UIManager.getColor(PREFIX + ".selectionBackground");
		// moved lighter, to the minimum distance from the alternate rows around it
		assertEquals(color("#414141"), colors.background(null, 0, null, true, selection));
		assertEquals(shade(color("#414141")), colors.background(null, 1, null, true, selection));
		// the inactive one to its own minimum
		colors = new RowColors(PREFIX, true, true);
		Color inactive = UIManager.getColor(PREFIX + ".selectionInactiveBackground");
		assertEquals(color("#3c3c3c"), colors.background(null, 0, null, true, inactive));
		// a selection background set on the component left as is
		assertEquals(color("#303030"), colors.background(null, 0, null, true, color("#303030")));
		// as is a selection distinct enough
		theme("#1e1e1e", "#4f4945", "#ebdbb2", "#262626", "#ebdbb2");
		colors = new RowColors(PREFIX, true, false);
		assertEquals(color("#4f4945"), colors.background(null, 0, null, true, null));
	}

	@Test
	void distinctSelectionDirection() {
		// Gruvbox Light Soft, moved darker
		theme("#f2e5bc", "#e6d5ae", "#3c3836", "#e6d5ae", "#3c3836");
		assertEquals(color("#cabb99"), new RowColors(PREFIX, true, false).background(null, 0, null, true, null));
		// Hiberbee Dark, a dark theme with a selection darker than the background, moved darker still
		theme("#373635", "#222120", "#e6e6e6", "#323130", "#e6e6e6");
		assertEquals(color("#151414"), new RowColors(PREFIX, true, false).background(null, 0, null, true, null));
	}

	@Test
	void distinctSelectionTextContrast() {
		// Espresso Light, its pink selection text at 2.7:1, the move held where it would fall lower
		theme("#fbfbfb", "#fffae3", "#f16e8f", "#ffe1af", "#f16e8f");
		assertEquals(color("#fffbe6"), new RowColors(PREFIX, true, false).background(null, 0, null, true, null));
	}

	@Test
	void distinctSelectionTranslucent() {
		// One Pauintxi, a translucent selection, composited over the background
		theme("#202020", "#40db9a35", "#db9a35", "#161b22", "#b1bac4");
		RowColors colors = new RowColors(PREFIX, true, false);
		Color selection = UIManager.getColor(PREFIX + ".selectionBackground");
		// distinct enough, left translucent
		assertEquals(selection, colors.background(null, 0, null, true, null));
		theme("#202030", "#4049a3a6", "#49a3a6", "#161b22", "#b1bac4");
		colors = new RowColors(PREFIX, true, false);
		assertEquals(color("#334955"), colors.background(null, 0, null, true, null));
	}

	@Test
	void distinctSelectionCached() {
		theme("#1e1e1e", "#303030", "#ebdbb2", "#262626", "#ebdbb2");
		RowColors colors = new RowColors(PREFIX, false, false);
		Color selection = colors.background(null, 0, null, true, null);
		assertSame(selection, colors.background(null, 0, null, true, null));
		colors.update();
		assertEquals(selection, colors.background(null, 0, null, true, null));
	}

	private static void theme(String background, String selectionBackground, String selectionForeground,
														String selectionInactiveBackground, String selectionInactiveForeground) {
		UIManager.put(PREFIX + ".background", new ColorUIResource(color(background)));
		UIManager.put(PREFIX + ".selectionBackground", new ColorUIResourceAlpha(color(selectionBackground)));
		UIManager.put(PREFIX + ".selectionForeground", new ColorUIResource(color(selectionForeground)));
		UIManager.put(PREFIX + ".selectionInactiveBackground", new ColorUIResource(color(selectionInactiveBackground)));
		UIManager.put(PREFIX + ".selectionInactiveForeground", new ColorUIResource(color(selectionInactiveForeground)));
	}

	// #rrggbb or #aarrggbb
	private static Color color(String hex) {
		long value = Long.parseLong(hex.substring(1), 16);

		return hex.length() == 9 ? new Color((int) value, true) : new Color((int) value);
	}

	// ColorUIResource drops the alpha
	private static final class ColorUIResourceAlpha extends Color implements UIResource {

		private ColorUIResourceAlpha(Color color) {
			super(color.getRGB(), true);
		}
	}

	private static void assertPlain(Color expected, Color color) {
		assertEquals(expected, color);
		assertFalse(color instanceof UIResource);
	}
}
