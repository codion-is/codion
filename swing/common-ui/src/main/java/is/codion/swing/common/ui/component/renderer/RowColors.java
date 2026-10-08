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

import org.jspecify.annotations.Nullable;

import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import java.awt.Color;

import static is.codion.swing.common.ui.color.Colors.shade;
import static java.util.Objects.requireNonNull;

/**
 * <p>The row colors of a table, list or tree: alternate row shading, item colors and selection colors.
 * <p>The look and feel colors are those under the given prefix, such as {@code Table.selectionBackground},
 * cached until {@link #update()} is called.
 * <p>The colors returned are never {@link UIResource}s, which the Synth based look and feels, such as Nimbus, replace
 * with the colors of their own style when painting a renderer, see {@link javax.swing.plaf.synth.SynthStyle#getColor}.
 */
public final class RowColors {

	private static final float SELECTION_COLOR_BLEND_RATIO = 0.5f;

	private final String prefix;
	private final boolean alternateRowColoring;
	private final boolean inactiveSelection;

	private UIColors colors;

	/**
	 * @param prefix the prefix of the look and feel colors, {@code Table}, {@code List} or {@code Tree}
	 * @param alternateRowColoring true if alternate rows should be shaded
	 * @param inactiveSelection true if the selection colors the look and feel sets on the component should be used,
	 * such as FlatLaf's inactive ones while the component is not focused, instead of the default ones
	 */
	public RowColors(String prefix, boolean alternateRowColoring, boolean inactiveSelection) {
		this.prefix = requireNonNull(prefix);
		this.alternateRowColoring = alternateRowColoring;
		this.inactiveSelection = inactiveSelection;
		this.colors = new UIColors(prefix);
	}

	/**
	 * Updates the look and feel colors, following a look and feel change.
	 */
	public void update() {
		colors = new UIColors(prefix);
	}

	/**
	 * @return the foreground of the look and feel
	 */
	public Color foreground() {
		return colors.foreground;
	}

	/**
	 * @return the inactive selection background of the look and feel, used while the component is not focused,
	 * as FlatLaf provides, null if none
	 */
	public @Nullable Color selectionInactiveBackground() {
		return colors.selectionInactiveBackground;
	}

	/**
	 * @return the inactive selection foreground of the look and feel, used while the component is not focused,
	 * as FlatLaf provides, null if none
	 */
	public @Nullable Color selectionInactiveForeground() {
		return colors.selectionInactiveForeground;
	}

	/**
	 * @param background the background of the component, null for the one of the look and feel
	 * @param row the row
	 * @return the background of the given row, unselected and without an item color
	 */
	public Color background(@Nullable Color background, int row) {
		boolean alternateRow = alternateRow(row);
		if (alternateRowColoring) {
			return plain(alternateRow ? alternateBackground(background) : background(background));
		}
		// If the look and feel sets an alternate row color, respect it
		return plain(alternateRow && colors.alternateRowColor != null ? colors.alternateRowColor : background(background));
	}

	/**
	 * <p>An item background is shaded on alternate rows and blended with the selection background on selected rows.
	 * @param background the background of the component, null for the one of the look and feel
	 * @param row the row
	 * @param itemBackground the background of the item, null for none
	 * @param selected true if the row is selected
	 * @param selectionBackground the selection background of the component, null for the one of the look and feel
	 * @return the background of the given row
	 */
	public Color background(@Nullable Color background, int row, @Nullable Color itemBackground,
													boolean selected, @Nullable Color selectionBackground) {
		boolean alternateRow = alternateRow(row);
		if (itemBackground != null && alternateRowColoring && alternateRow) {
			itemBackground = shade(itemBackground);
		}
		Color selection = selected ? selectionColor(selectionBackground, colors.selectionBackground) : null;
		if (selection != null) {
			if (alternateRowColoring && alternateRow) {
				selection = shade(selection);
			}

			return itemBackground == null ? plain(selection) : blend(itemBackground, selection);
		}

		return itemBackground == null ? background(background, row) : plain(itemBackground);
	}

	/**
	 * <p>The selection foreground wins over an item foreground, which may not be readable on the selection background.
	 * @param itemForeground the foreground of the item, null for none
	 * @param selected true if the row is selected
	 * @param selectionForeground the selection foreground of the component, null for the one of the look and feel
	 * @return the foreground of the given row
	 */
	public Color foreground(@Nullable Color itemForeground, boolean selected, @Nullable Color selectionForeground) {
		Color selection = selected ? selectionColor(selectionForeground, colors.selectionForeground) : null;
		if (selection != null) {
			return plain(selection);
		}

		return plain(itemForeground == null ? colors.foreground : itemForeground);
	}

	// A selection color set by the look and feel, such as FlatLaf's inactive one while the component is not focused,
	// is replaced by the default one of the look and feel, unless inactive selection is enabled, or the look and feel
	// has no default one, as Nimbus has none for lists
	private @Nullable Color selectionColor(@Nullable Color color, @Nullable Color defaultColor) {
		if (color == null || (!inactiveSelection && color instanceof UIResource && defaultColor != null)) {
			return defaultColor;
		}

		return color;
	}

	// The background of the component itself, the reference for shading the alternate rows
	private Color background(@Nullable Color background) {
		return background == null ? colors.background : background;
	}

	private Color alternateBackground(@Nullable Color background) {
		return colors.alternateRowColor == null ? shade(background(background)) : colors.alternateRowColor;
	}

	private static Color plain(Color color) {
		return color instanceof UIResource ? new Color(color.getRGB(), true) : color;
	}

	private static boolean alternateRow(int row) {
		return row % 2 != 0;
	}

	private static Color blend(Color color1, Color color2) {
		int r = (int) (color1.getRed() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getRed() * SELECTION_COLOR_BLEND_RATIO);
		int g = (int) (color1.getGreen() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getGreen() * SELECTION_COLOR_BLEND_RATIO);
		int b = (int) (color1.getBlue() * SELECTION_COLOR_BLEND_RATIO) + (int) (color2.getBlue() * SELECTION_COLOR_BLEND_RATIO);

		return new Color(r, g, b, color1.getAlpha());
	}

	private static final class UIColors {

		private final Color foreground;
		private final Color background;
		private final @Nullable Color alternateRowColor;
		private final @Nullable Color selectionForeground;
		private final @Nullable Color selectionBackground;
		private final @Nullable Color selectionInactiveForeground;
		private final @Nullable Color selectionInactiveBackground;

		private UIColors(String prefix) {
			foreground = UIManager.getColor(prefix + ".foreground");
			background = UIManager.getColor(prefix + ".background");
			alternateRowColor = UIManager.getColor(prefix + ".alternateRowColor");
			selectionForeground = UIManager.getColor(prefix + ".selectionForeground");
			selectionBackground = UIManager.getColor(prefix + ".selectionBackground");
			selectionInactiveForeground = UIManager.getColor(prefix + ".selectionInactiveForeground");
			selectionInactiveBackground = UIManager.getColor(prefix + ".selectionInactiveBackground");
		}
	}
}
