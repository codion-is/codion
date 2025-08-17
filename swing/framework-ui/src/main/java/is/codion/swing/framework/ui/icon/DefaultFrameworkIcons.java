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

import is.codion.common.value.Value;
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

	private final Icons icons = Icons.icons();
	private final Map<Integer, FontImageIcon> logos = new HashMap<>();

	private ImageIcon logo = createLogo();
	private ImageIcon refreshRequired = createRefreshRequiredIcon();

	/**
	 * Instantiates a new {@link DefaultFrameworkIcons} instance
	 */
	public DefaultFrameworkIcons() {
		add(LOGO, FILTER, SEARCH, ADD, DELETE, UPDATE, COPY, REFRESH, CLEAR, UP, DOWN, DETAIL,
						PRINT, EDIT, SUMMARY, EDIT_PANEL, DEPENDENCIES, SETTINGS, CALENDAR, EDIT_TEXT, COLUMNS);
		icons.color().addConsumer(this::onColorChanged);
		Scaler.RATIO.addWeakListener(this::onScalingChanged);
	}

	@Override
	public Value<Color> color() {
		return icons.color();
	}

	@Override
	public int size() {
		return icons.size();
	}

	@Override
	public void add(Ikon... ikons) {
		icons.add(ikons);
	}

	@Override
	public ImageIcon get(Ikon ikon) {
		return icons.get(ikon);
	}

	@Override
	public ImageIcon filter() {
		return get(FILTER);
	}

	@Override
	public ImageIcon search() {
		return get(SEARCH);
	}

	@Override
	public ImageIcon add() {
		return get(ADD);
	}

	@Override
	public ImageIcon delete() {
		return get(DELETE);
	}

	@Override
	public ImageIcon update() {
		return get(UPDATE);
	}

	@Override
	public ImageIcon copy() {
		return get(COPY);
	}

	@Override
	public ImageIcon refresh() {
		return get(REFRESH);
	}

	@Override
	public ImageIcon refreshRequired() {
		return refreshRequired;
	}

	@Override
	public ImageIcon clear() {
		return get(CLEAR);
	}

	@Override
	public ImageIcon up() {
		return get(UP);
	}

	@Override
	public ImageIcon down() {
		return get(DOWN);
	}

	@Override
	public ImageIcon detail() {
		return get(DETAIL);
	}

	@Override
	public ImageIcon print() {
		return get(PRINT);
	}

	@Override
	public ImageIcon clearSelection() {
		return get(CLEAR);
	}

	@Override
	public ImageIcon edit() {
		return get(EDIT);
	}

	@Override
	public ImageIcon summary() {
		return get(SUMMARY);
	}

	@Override
	public ImageIcon editPanel() {
		return get(EDIT_PANEL);
	}

	@Override
	public ImageIcon dependencies() {
		return get(DEPENDENCIES);
	}

	@Override
	public ImageIcon settings() {
		return get(SETTINGS);
	}

	@Override
	public ImageIcon calendar() {
		return get(CALENDAR);
	}

	@Override
	public ImageIcon editText() {
		return get(EDIT_TEXT);
	}

	@Override
	public ImageIcon columns() {
		return get(COLUMNS);
	}

	@Override
	public ImageIcon logo() {
		return logo;
	}

	static FrameworkIcons instance() {
		if (instance == null) {
			instance = createInstance();
		}

		return instance;
	}

	private ImageIcon createLogo() {
		return FontImageIcon.builder()
						.ikon(LOGO)
						.size(Scaler.scale(LOGO_SIZE))
						.color(icons.color().getOrThrow())
						.iconPainter(LOGO_ICON_PAINTER)
						.imageIconFactory(LOGO_ICON_FACTORY)
						.build()
						.imageIcon();
	}

	private ImageIcon createRefreshRequiredIcon() {
		return FontImageIcon.builder()
						.ikon(REFRESH)
						.size(Scaler.scale(icons.size()))
						.color(Color.RED.darker())
						.build()
						.imageIcon();
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
		synchronized (logos) {
			logos.values().forEach(icon -> icon.color(color));
		}
	}

	private void onScalingChanged() {
		logo = createLogo();
		refreshRequired = createRefreshRequiredIcon();
	}
}
