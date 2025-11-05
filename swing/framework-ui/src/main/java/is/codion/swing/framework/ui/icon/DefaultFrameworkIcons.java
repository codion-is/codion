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
package is.codion.swing.framework.ui.icon;

import is.codion.common.reactive.value.Value;
import is.codion.swing.common.ui.control.ControlIcon;
import is.codion.swing.common.ui.icon.FontImageIcon;
import is.codion.swing.common.ui.icon.FontImageIcon.IconPainter;
import is.codion.swing.common.ui.icon.FontImageIcon.ImageIconFactory;
import is.codion.swing.common.ui.icon.Icons;
import is.codion.swing.common.ui.scaler.Scaler;

import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static is.codion.swing.common.ui.control.ControlIcon.controlIcon;
import static is.codion.swing.framework.ui.icon.FrameworkIkon.*;
import static java.util.stream.StreamSupport.stream;

/**
 * A default {@link FrameworkIcons} implementation.
 */
public final class DefaultFrameworkIcons implements FrameworkIcons {

	private static final int LOGO_SIZE = 68;

	private static final IconPainter LOGO_ICON_PAINTER = new IconPainter() {

		@Override
		public void paintIcon(FontIcon fontIcon, ImageIcon imageIcon) {
			//center on y-axis
			int yOffset = (fontIcon.getIconHeight() - fontIcon.getIconWidth()) / 2;

			fontIcon.paintIcon(null, imageIcon.getImage().getGraphics(), 0, -yOffset);
		}
	};

	private static final ImageIconFactory LOGO_ICON_FACTORY = new ImageIconFactory() {
		@Override
		public ImageIcon createImageIcon(FontIcon fontIcon) {
			int yCorrection = (fontIcon.getIconHeight() - fontIcon.getIconWidth());

			return new ImageIcon(new BufferedImage(fontIcon.getIconWidth(), fontIcon.getIconHeight() - yCorrection, BufferedImage.TYPE_INT_ARGB));
		}
	};

	private static @Nullable FrameworkIcons instance;

	private final Icons smallIcons = Icons.icons(SMALL_SIZE.getOrThrow());
	private final Icons largeIcons = Icons.icons(LARGE_SIZE.getOrThrow());

	private FontImageIcon logo = createLogo();

	/**
	 * Instantiates a new {@link DefaultFrameworkIcons} instance
	 */
	public DefaultFrameworkIcons() {
		add(FILTER, SEARCH, ADD, DELETE, UPDATE, COPY, REFRESH, CLEAR, UP, DOWN, DETAIL,
						PRINT, EDIT, SUMMARY, EDIT_PANEL, DEPENDENCIES, SETTINGS, CALENDAR, EDIT_TEXT, COLUMNS);
		largeIcons.color().link(smallIcons.color());
		smallIcons.color().addConsumer(this::onColorChanged);
		Scaler.SCALING.addWeakListener(this::onScalingChanged);
	}

	@Override
	public Value<Color> color() {
		return smallIcons.color();
	}

	@Override
	public void add(Ikon... ikons) {
		smallIcons.add(ikons);
		largeIcons.add(ikons);
	}

	@Override
	public ControlIcon get(Ikon ikon) {
		return controlIcon(smallIcons.get(ikon), largeIcons.get(ikon));
	}

	@Override
	public ControlIcon filter() {
		return get(FILTER);
	}

	@Override
	public ControlIcon search() {
		return get(SEARCH);
	}

	@Override
	public ControlIcon add() {
		return get(ADD);
	}

	@Override
	public ControlIcon delete() {
		return get(DELETE);
	}

	@Override
	public ControlIcon update() {
		return get(UPDATE);
	}

	@Override
	public ControlIcon copy() {
		return get(COPY);
	}

	@Override
	public ControlIcon refresh() {
		return get(REFRESH);
	}

	@Override
	public ControlIcon clear() {
		return get(CLEAR);
	}

	@Override
	public ControlIcon up() {
		return get(UP);
	}

	@Override
	public ControlIcon down() {
		return get(DOWN);
	}

	@Override
	public ControlIcon detail() {
		return get(DETAIL);
	}

	@Override
	public ControlIcon print() {
		return get(PRINT);
	}

	@Override
	public ControlIcon clearSelection() {
		return get(CLEAR);
	}

	@Override
	public ControlIcon edit() {
		return get(EDIT);
	}

	@Override
	public ControlIcon summary() {
		return get(SUMMARY);
	}

	@Override
	public ControlIcon editPanel() {
		return get(EDIT_PANEL);
	}

	@Override
	public ControlIcon dependencies() {
		return get(DEPENDENCIES);
	}

	@Override
	public ControlIcon settings() {
		return get(SETTINGS);
	}

	@Override
	public ControlIcon calendar() {
		return get(CALENDAR);
	}

	@Override
	public ControlIcon editText() {
		return get(EDIT_TEXT);
	}

	@Override
	public ControlIcon columns() {
		return get(COLUMNS);
	}

	@Override
	public ImageIcon logo() {
		return logo.imageIcon();
	}

	static FrameworkIcons instance() {
		if (instance == null) {
			instance = createInstance();
		}

		return instance;
	}

	private FontImageIcon createLogo() {
		return FontImageIcon.builder()
						.ikon(LOGO)
						.size(Scaler.scale(LOGO_SIZE))
						.color(smallIcons.color().getOrThrow())
						.iconPainter(LOGO_ICON_PAINTER)
						.imageIconFactory(LOGO_ICON_FACTORY)
						.build();
	}

	private static FrameworkIcons createInstance() {
		String iconsClassName = FRAMEWORK_ICONS_CLASSNAME.get();
		try {
			return stream(ServiceLoader.load(FrameworkIcons.class).spliterator(), false)
							.filter(icons -> icons.getClass().getName().equals(iconsClassName))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("FrameworkIcons implementation " + iconsClassName + " not found"));
		}
		catch (ServiceConfigurationError e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		}
	}

	private void onColorChanged(Color color) {
		logo.color(color);
	}

	private void onScalingChanged() {
		logo = createLogo();
	}
}
