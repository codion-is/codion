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

import is.codion.common.reactive.value.Value;
import is.codion.swing.common.ui.scaler.Scaler;

import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static javax.swing.UIManager.getColor;

final class DefaultIcons implements Icons {

	private final Map<Ikon, FontImageIcon> icons = new HashMap<>();

	private final OnLookAndFeelChanged onLookAndFeelChanged = new OnLookAndFeelChanged();
	private final OnScalingChanged onScalingChanged = new OnScalingChanged();

	private final int size;
	private final Value<Color> color = Value.builder()
					.nonNull(COLOR.getOrThrow())
					.consumer(this::onColorChanged)
					.build();

	DefaultIcons(int size) {
		this.size = size;
		UIManager.addPropertyChangeListener(onLookAndFeelChanged);
		Scaler.SCALING.addWeakListener(onScalingChanged);
	}

	@Override
	public Value<Color> color() {
		return color;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public void add(Ikon... ikons) {
		int iconSize = Scaler.scale(size);
		Color iconColor = color.getOrThrow();
		synchronized (icons) {
			for (Ikon ikon : requireNonNull(ikons)) {
				if (icons.containsKey(requireNonNull(ikon))) {
					throw new IllegalArgumentException("Icon has already been added: " + ikon);
				}
			}
			for (Ikon ikon : ikons) {
				icons.put(ikon, FontImageIcon.builder()
								.ikon(ikon)
								.size(iconSize)
								.color(iconColor)
								.build());
			}
		}
	}

	@Override
	public ImageIcon get(Ikon ikon) {
		synchronized (icons) {
			if (!icons.containsKey(requireNonNull(ikon))) {
				throw new IllegalArgumentException("No icon has been added for key: " + ikon);
			}

			return icons.get(ikon).imageIcon();
		}
	}

	private void onColorChanged(Color color) {
		synchronized (icons) {
			icons.values().forEach(icon -> icon.color(color));
		}
	}

	private final class OnScalingChanged implements Runnable {

		@Override
		public void run() {
			int iconSize = Scaler.scale(size);
			Color iconColor = color.getOrThrow();
			synchronized (icons) {
				icons.replaceAll((ikon, fontImageIcon) -> FontImageIcon.builder()
								.ikon(ikon)
								.size(iconSize)
								.color(iconColor)
								.build());
			}
		}
	}

	private final class OnLookAndFeelChanged implements PropertyChangeListener {

		private static final String LOOK_AND_FEEL_PROPERTY = "lookAndFeel";

		@Override
		public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
			if (propertyChangeEvent.getPropertyName().equals(LOOK_AND_FEEL_PROPERTY)) {
				color.set(getColor("Button.foreground"));
			}
		}
	}
}
