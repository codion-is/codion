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
import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.swing.common.ui.control.ControlIcon;
import is.codion.swing.common.ui.icon.SVGIcon;
import is.codion.swing.common.ui.icon.SVGIcons;
import is.codion.swing.common.ui.scaler.Scaler;

import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.net.URL;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static is.codion.swing.common.ui.control.ControlIcon.controlIcon;
import static is.codion.swing.common.ui.icon.SVGIcons.svgIcons;
import static java.util.stream.StreamSupport.stream;

/**
 * A default {@link FrameworkIcons} implementation.
 */
public final class DefaultFrameworkIcons implements FrameworkIcons {

	private static final int LOGO_SIZE = 68;

	private static @Nullable FrameworkIcons instance;

	private final SVGIcons smallIcons = svgIcons(SMALL_SIZE.getOrThrow());
	private final SVGIcons largeIcons = svgIcons(LARGE_SIZE.getOrThrow());

	public DefaultFrameworkIcons() {
		addIcon(FILTER);
		addIcon(SEARCH);
		addIcon(ADD);
		addIcon(DELETE);
		addIcon(UPDATE);
		addIcon(COPY);
		addIcon(REFRESH);
		addIcon(CLEAR);
		addIcon(UP);
		addIcon(DOWN);
		addIcon(DETAIL);
		addIcon(PRINT);
		addIcon(EDIT);
		addIcon(SUMMARY);
		addIcon(EDIT_PANEL);
		addIcon(DEPENDENCIES);
		addIcon(SETTINGS);
		addIcon(CALENDAR);
		addIcon(EDIT_TEXT);
		addIcon(COLUMNS);
		addIcon(EXPORT);
		addLogo();
	}

	@Override
	public Value<Color> color() {
		return smallIcons.color();
	}

	@Override
	public void put(String identifier, URL svgUrl) {
		smallIcons.put(identifier, svgUrl);
		largeIcons.put(identifier, svgUrl);
	}

	@Override
	public ControlIcon get(String identifier) {
		return controlIcon(smallIcons.get(identifier), largeIcons.get(identifier));
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
	public ControlIcon export() {
		return get(EXPORT);
	}

	@Override
	public ControlIcon logo() {
		return get(LOGO);
	}

	private void addIcon(String identifier) {
		URL resource = DefaultFrameworkIcons.class.getResource(identifier + ".svg");
		smallIcons.put(identifier, resource);
		largeIcons.put(identifier, resource);
	}

	private void addLogo() {
		URL resource = FrameworkIcons.class.getResource("logo.svg");
		smallIcons.put(LOGO, resource);
		largeIcons.put(LOGO, SVGIcon.svgIcon(resource, Scaler.scale(LOGO_SIZE), largeIcons.color().getOrThrow()));
	}

	static FrameworkIcons instance() {
		if (instance == null) {
			instance = createInstance();
		}

		return instance;
	}

	private static FrameworkIcons createInstance() {
		String iconsClassName = FRAMEWORK_ICONS.getOrThrow();
		try {
			return stream(ServiceLoader.load(FrameworkIcons.class).spliterator(), false)
							.filter(icons -> icons.getClass().getName().equals(iconsClassName))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("FrameworkIcons implementation " + iconsClassName + " not found"));
		}
		catch (ServiceConfigurationError e) {
			throw Exceptions.runtime(e, ServiceConfigurationError.class);
		}
	}
}
