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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.icon;

import is.codion.common.value.Value;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultIcons implements Icons {

	private static final String BUTTON_FOREGROUND_PROPERTY = "Button.foreground";

	static final Value<Color> ICON_COLOR = Value.nonNull(UIManager.getColor(BUTTON_FOREGROUND_PROPERTY));

	private final Map<Ikon, FontImageIcon> icons = new HashMap<>();

	private final OnIconColorChanged onIconColorChanged = new OnIconColorChanged();

	static {
		UIManager.addPropertyChangeListener(new OnLookAndFeelChanged());
	}

	DefaultIcons() {
		ICON_COLOR.addWeakConsumer(onIconColorChanged);
	}

	@Override
	public void add(Ikon... ikons) {
		for (Ikon ikon : requireNonNull(ikons)) {
			if (icons.containsKey(requireNonNull(ikon))) {
				throw new IllegalArgumentException("Icon has already been added: " + ikon);
			}
		}
		for (Ikon ikon : ikons) {
			icons.put(ikon, FontImageIcon.builder(ikon).build());
		}
	}

	@Override
	public ImageIcon icon(Ikon ikon) {
		if (!icons.containsKey(requireNonNull(ikon))) {
			throw new IllegalArgumentException("No icon has been added for key: " + ikon);
		}

		return icons.get(ikon).imageIcon();
	}

	private final class OnIconColorChanged implements Consumer<Color> {

		@Override
		public void accept(Color color) {
			if (color != null) {
				icons.values().forEach(icon -> icon.color(color));
			}
		}
	}

	private static final class OnLookAndFeelChanged implements PropertyChangeListener {

		private static final String LOOK_AND_FEEL_PROPERTY = "lookAndFeel";

		@Override
		public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
			if (propertyChangeEvent.getPropertyName().equals(LOOK_AND_FEEL_PROPERTY) && ICON_COLOR != null) {
				ICON_COLOR.set(UIManager.getColor(BUTTON_FOREGROUND_PROPERTY));
			}
		}
	}
}
