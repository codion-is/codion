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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.icon;

import is.codion.swing.common.ui.icon.FontImageIcon;
import is.codion.swing.common.ui.icon.FontImageIcon.IconPainter;
import is.codion.swing.common.ui.icon.FontImageIcon.ImageIconFactory;
import is.codion.swing.common.ui.icon.Icons;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static is.codion.swing.framework.ui.icon.FrameworkIkon.*;
import static java.util.stream.StreamSupport.stream;

/**
 * A default {@link FrameworkIcons} implementation.
 */
public final class DefaultFrameworkIcons implements FrameworkIcons {

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

	private static FrameworkIcons instance;

	private final Icons icons = Icons.icons();
	private final Map<Integer, FontImageIcon> logos = new HashMap<>();
	private final ImageIcon refreshRequired = FontImageIcon.builder(REFRESH)
					.color(Color.RED.darker())
					.build().imageIcon();

	/**
	 * Instantiates a new {@link DefaultFrameworkIcons} instance
	 */
	public DefaultFrameworkIcons() {
		add(LOGO, FILTER, SEARCH, ADD, DELETE, UPDATE, COPY, REFRESH, CLEAR, UP, DOWN, DETAIL,
						PRINT, EDIT, SUMMARY, EDIT_PANEL, DEPENDENCIES, SETTINGS, CALENDAR, EDIT_TEXT, COLUMNS);
	}

	@Override
	public void add(Ikon... ikons) {
		icons.add(ikons);
	}

	@Override
	public ImageIcon icon(Ikon ikon) {
		return icons.icon(ikon);
	}

	@Override
	public ImageIcon filter() {
		return icon(FILTER);
	}

	@Override
	public ImageIcon search() {
		return icon(SEARCH);
	}

	@Override
	public ImageIcon add() {
		return icon(ADD);
	}

	@Override
	public ImageIcon delete() {
		return icon(DELETE);
	}

	@Override
	public ImageIcon update() {
		return icon(UPDATE);
	}

	@Override
	public ImageIcon copy() {
		return icon(COPY);
	}

	@Override
	public ImageIcon refresh() {
		return icon(REFRESH);
	}

	@Override
	public ImageIcon refreshRequired() {
		return refreshRequired;
	}

	@Override
	public ImageIcon clear() {
		return icon(CLEAR);
	}

	@Override
	public ImageIcon up() {
		return icon(UP);
	}

	@Override
	public ImageIcon down() {
		return icon(DOWN);
	}

	@Override
	public ImageIcon detail() {
		return icon(DETAIL);
	}

	@Override
	public ImageIcon print() {
		return icon(PRINT);
	}

	@Override
	public ImageIcon clearSelection() {
		return icon(CLEAR);
	}

	@Override
	public ImageIcon edit() {
		return icon(EDIT);
	}

	@Override
	public ImageIcon summary() {
		return icon(SUMMARY);
	}

	@Override
	public ImageIcon editPanel() {
		return icon(EDIT_PANEL);
	}

	@Override
	public ImageIcon dependencies() {
		return icon(DEPENDENCIES);
	}

	@Override
	public ImageIcon settings() {
		return icon(SETTINGS);
	}

	@Override
	public ImageIcon calendar() {
		return icon(CALENDAR);
	}

	@Override
	public ImageIcon editText() {
		return icon(EDIT_TEXT);
	}

	@Override
	public ImageIcon columns() {
		return icon(COLUMNS);
	}

	@Override
	public ImageIcon logo() {
		return icon(LOGO);
	}

	@Override
	public ImageIcon logo(int size) {
		return logos.computeIfAbsent(size, k -> FontImageIcon.builder(LOGO)
						.size(size)
						.iconPainter(LOGO_ICON_PAINTER)
						.imageIconFactory(LOGO_ICON_FACTORY)
						.build()).imageIcon();
	}

	@Override
	public void iconColor(Color color) {
		icons.iconColor(color);
	}

	@Override
	public FrameworkIcons enableIconColorConsumer() {
		icons.enableIconColorConsumer();
		return this;
	}

	@Override
	public FrameworkIcons disableIconColorConsumer() {
		icons.disableIconColorConsumer();
		return this;
	}

	static FrameworkIcons instance() {
		if (instance == null) {
			instance = (FrameworkIcons) createInstance().enableIconColorConsumer();
		}

		return instance;
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
}
