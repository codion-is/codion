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
import java.util.HashMap;
import java.util.Map;

import static is.codion.swing.common.ui.color.Colors.shade;
import static java.util.Objects.requireNonNull;

/**
 * <p>The row colors of a table, list or tree: alternate row shading, item colors and selection colors.
 * <p>The look and feel colors are those under the given prefix, such as {@code Table.selectionBackground},
 * cached until {@link #update()} is called.
 * <p>The colors returned are never {@link UIResource}s, which the Synth based look and feels, such as Nimbus, replace
 * with the colors of their own style when painting a renderer, see {@link javax.swing.plaf.synth.SynthStyle#getColor}.
 * <p>A selection background of the look and feel too close to the rows around it to stand out, as in many themes once
 * alternate rows are shaded, is moved toward white or black, the way it differs from the background first, until it is
 * a minimum distance from them, in OKLab, 0.08 for the selection and 0.05 for the inactive one, a just noticeable
 * difference being about 0.02. The move stops where the contrast of the selection foreground with it would fall below
 * 3:1, or below the contrast before the move, if lower. A selection background set on the component is left as is.
 */
public final class RowColors {

	private static final float SELECTION_COLOR_BLEND_RATIO = 0.5f;
	private static final double SELECTION_DISTANCE = 0.08;
	private static final double INACTIVE_SELECTION_DISTANCE = 0.05;
	private static final double SELECTION_TEXT_CONTRAST = 3;
	private static final int SELECTION_STEPS = 50;

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
			selection = distinct(selection, background(background));
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

	// A selection background of the look and feel, moved if needed to stand out from the rows around it
	private Color distinct(Color selection, Color background) {
		if (!(selection instanceof UIResource)) {
			return selection;
		}

		return colors.distinct.computeIfAbsent(((long) selection.getRGB() << 32) | (background.getRGB() & 0xffffffffL),
						key -> distinctSelection(selection, background));
	}

	private Color distinctSelection(Color selection, Color background) {
		boolean inactive = selection.equals(colors.selectionInactiveBackground) && !selection.equals(colors.selectionBackground);
		double minimum = inactive ? INACTIVE_SELECTION_DISTANCE : SELECTION_DISTANCE;
		Color opaque = over(selection, background);
		double separation = separation(opaque, background);
		if (separation >= minimum) {
			return selection;
		}
		Color foreground = inactive && colors.selectionInactiveForeground != null ?
						colors.selectionInactiveForeground : colors.selectionForeground;
		double textContrast = foreground == null ? 0 : Math.min(contrast(foreground, opaque), SELECTION_TEXT_CONTRAST);
		double lightness = oklab(opaque)[0];
		double backgroundLightness = oklab(background)[0];
		boolean lighter = lightness == backgroundLightness ? dark(background) : lightness > backgroundLightness;
		Color best = opaque;
		double bestSeparation = separation;
		for (boolean towardWhite : new boolean[] {lighter, !lighter}) {
			for (int step = 1; step < SELECTION_STEPS; step++) {
				Color candidate = mix(opaque, towardWhite ? 255 : 0, step / (double) SELECTION_STEPS);
				if (foreground != null && contrast(foreground, candidate) < textContrast) {
					break;
				}
				double candidateSeparation = separation(candidate, background);
				if (candidateSeparation >= minimum) {
					return candidate;
				}
				if (candidateSeparation > bestSeparation) {
					best = candidate;
					bestSeparation = candidateSeparation;
				}
			}
		}

		return bestSeparation > separation ? best : selection;
	}

	// The smallest distance of a selected row from the rows around it: with alternate row coloring, a selected row
	// between alternate rows, and a selected alternate row, shaded, between plain ones
	private double separation(Color selection, Color background) {
		if (alternateRowColoring) {
			return Math.min(distance(selection, alternateBackground(background)), distance(shade(selection), background));
		}
		double distance = distance(selection, background);

		return colors.alternateRowColor == null ? distance : Math.min(distance, distance(selection, colors.alternateRowColor));
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

	// A translucent color as painted over the background
	private static Color over(Color color, Color background) {
		if (color.getAlpha() == 255) {
			return color;
		}
		double alpha = color.getAlpha() / 255d;

		return new Color((int) Math.round(color.getRed() * alpha + background.getRed() * (1 - alpha)),
						(int) Math.round(color.getGreen() * alpha + background.getGreen() * (1 - alpha)),
						(int) Math.round(color.getBlue() * alpha + background.getBlue() * (1 - alpha)));
	}

	private static Color mix(Color color, int target, double fraction) {
		return new Color(mix(color.getRed(), target, fraction), mix(color.getGreen(), target, fraction),
						mix(color.getBlue(), target, fraction));
	}

	private static int mix(int component, int target, double fraction) {
		return (int) Math.round(component + (target - component) * fraction);
	}

	// by perceived luminance, ITU-R BT.601, as Colors.shade() decides
	private static boolean dark(Color color) {
		return 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue() < 128;
	}

	// The Euclidean distance in OKLab, perceptually uniform
	private static double distance(Color color1, Color color2) {
		double[] lab1 = oklab(color1);
		double[] lab2 = oklab(color2);

		return Math.sqrt(square(lab1[0] - lab2[0]) + square(lab1[1] - lab2[1]) + square(lab1[2] - lab2[2]));
	}

	// https://bottosson.github.io/posts/oklab/
	private static double[] oklab(Color color) {
		double red = linear(color.getRed());
		double green = linear(color.getGreen());
		double blue = linear(color.getBlue());
		double l = Math.cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue);
		double m = Math.cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue);
		double s = Math.cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue);

		return new double[] {
						0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
						1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
						0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
		};
	}

	// The WCAG contrast ratio
	private static double contrast(Color color1, Color color2) {
		double luminance1 = luminance(color1);
		double luminance2 = luminance(color2);

		return (Math.max(luminance1, luminance2) + 0.05) / (Math.min(luminance1, luminance2) + 0.05);
	}

	private static double luminance(Color color) {
		return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
	}

	// sRGB to linear
	private static double linear(int component) {
		double value = component / 255d;

		return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
	}

	private static double square(double value) {
		return value * value;
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
		// the selection backgrounds by their rgb and the background's, moved if needed to stand out
		private final Map<Long, Color> distinct = new HashMap<>();

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
