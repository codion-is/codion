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
package is.codion.swing.common.ui.font;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.value.Value;

import javax.swing.UIDefaults;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.util.Enumeration;

import static javax.swing.UIManager.getLookAndFeelDefaults;

/**
 * Provies a central location for font size configuration.
 */
public final class FontSize {

	private static final int DEFAULT_FONT_SIZE_RATIO = 100;

	/**
	 * Specifies the global font size ratio.<br>
	 * 85 = decrease the default font size by 15%<br>
	 * 100 = use the default font size<br>
	 * 125 = increase the default font size by 25%<br>
	 * <p>Note that this does not support dynamic updates, application must be restarted for changes to take effect.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 100
	 * <li>Valid range: 50 - 200</li>
	 * </ul>
	 */
	public static final PropertyValue<Integer> RATIO = Configuration.integerValue(FontSize.class.getName() + ".ratio", DEFAULT_FONT_SIZE_RATIO);

	static {
		RATIO.addValidator(new FontSizeRatioValidator());
	}

	/**
	 * Updates the font sizes for the current {@link UIDefaults} using the ratio from {@link #RATIO}.
	 * Note that this must be done before the UI is initialized, dynamic font size changes are not supported.
	 */
	public static void updateFontSize() {
		UIDefaults defaults = getLookAndFeelDefaults();
		float multiplier = RATIO.getOrThrow() / 100f;
		Enumeration<Object> enumeration = defaults.keys();
		while (enumeration.hasMoreElements()) {
			Object key = enumeration.nextElement();
			Object defaultValue = defaults.get(key);
			if (defaultValue instanceof Font) {
				Font font = (Font) defaultValue;
				int newSize = Math.round(font.getSize() * multiplier);
				if (defaultValue instanceof FontUIResource) {
					defaults.put(key, new FontUIResource(font.getName(), font.getStyle(), newSize));
				}
				else {
					defaults.put(key, font.deriveFont(newSize));
				}
			}
		}
	}

	private static final class FontSizeRatioValidator implements Value.Validator<Integer> {

		@Override
		public void validate(Integer value) {
			if (value < 50 || value > 200) {
				throw new IllegalStateException("Font size ratio must be between 50 and 200");
			}
		}
	}
}
