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
package is.codion.swing.common.ui.scaler;

import javax.swing.UIDefaults;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static javax.swing.UIManager.getLookAndFeelDefaults;

public final class FontSizeScaler implements Scaler {

	private static final Set<String> SUPPORTED_LOOK_AND_FEELS = unmodifiableSet(new HashSet<>(asList(
					"javax.swing.plaf.metal.MetalLookAndFeel",
					"javax.swing.plaf.nimbus.NimbusLookAndFeel",
					"com.sun.java.swing.plaf.windows.WindowsLookAndFeel",
					"com.sun.java.swing.plaf.gtk.GTKLookAndFeel",
					"com.apple.laf.AquaLookAndFeel"
	)));

	public FontSizeScaler() {}

	@Override
	public void apply() {
		if (RATIO.isNotEqualTo(DEFAULT_RATIO)) {
			updateFontSize();
		}
	}

	@Override
	public boolean supports(String lookAndFeelClassName) {
		return SUPPORTED_LOOK_AND_FEELS.contains(requireNonNull(lookAndFeelClassName));
	}

	/**
	 * Updates the font sizes for the current {@link UIDefaults} using the ratio from {@link #RATIO}.
	 * Note that this must be done before the UI is initialized, dynamic scaling is not supported.
	 */
	private static void updateFontSize() {
		UIDefaults defaults = getLookAndFeelDefaults();
		float multiplier = RATIO.getOrThrow() / 100f;
		Enumeration<Object> enumeration = defaults.keys();
		while (enumeration.hasMoreElements()) {
			Object key = enumeration.nextElement();
			Object defaultValue = defaults.get(key);
			if (defaultValue instanceof Font) {
				Font font = (Font) defaultValue;
				Font derived = font.deriveFont((float) Math.round(font.getSize() * multiplier));
				if (defaultValue instanceof FontUIResource) {
					defaults.put(key, new FontUIResource(derived));
				}
				else {
					defaults.put(key, derived);
				}
			}
		}
	}
}
