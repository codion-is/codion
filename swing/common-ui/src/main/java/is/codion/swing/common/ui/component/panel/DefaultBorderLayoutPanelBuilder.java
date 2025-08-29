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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.panel;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;

final class DefaultBorderLayoutPanelBuilder extends DefaultPanelBuilder<BorderLayout, BorderLayoutPanelBuilder> implements BorderLayoutPanelBuilder {

	private static final Set<String> CONSTRAINTS = new HashSet<>(Arrays.asList(
					BorderLayout.CENTER,
					BorderLayout.NORTH,
					BorderLayout.SOUTH,
					BorderLayout.EAST,
					BorderLayout.WEST
	));

	DefaultBorderLayoutPanelBuilder() {
		this(borderLayout());
	}

	DefaultBorderLayoutPanelBuilder(BorderLayout layout) {
		layout(layout);
	}

	@Override
	public BorderLayoutPanelBuilder center(JComponent centerComponent) {
		return add(centerComponent, BorderLayout.CENTER);
	}

	@Override
	public BorderLayoutPanelBuilder center(Supplier<? extends JComponent> centerComponent) {
		return center(requireNonNull(centerComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder north(JComponent northComponent) {
		return add(northComponent, BorderLayout.NORTH);
	}

	@Override
	public BorderLayoutPanelBuilder north(Supplier<? extends JComponent> northComponent) {
		return north(requireNonNull(northComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder south(JComponent southComponent) {
		return add(southComponent, BorderLayout.SOUTH);
	}

	@Override
	public BorderLayoutPanelBuilder south(Supplier<? extends JComponent> southComponent) {
		return south(requireNonNull(southComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder east(JComponent eastComponent) {
		return add(eastComponent, BorderLayout.EAST);
	}

	@Override
	public BorderLayoutPanelBuilder east(Supplier<? extends JComponent> eastComponent) {
		return east(requireNonNull(eastComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder west(JComponent westComponent) {
		return add(westComponent, BorderLayout.WEST);
	}

	@Override
	public BorderLayoutPanelBuilder west(Supplier<? extends JComponent> westComponent) {
		return west(requireNonNull(westComponent).get());
	}

	@Override
	protected void validateConstraints(Object constraints) {
		super.validateConstraints(constraints);
		if (!CONSTRAINTS.contains(constraints)) {
			throw new IllegalArgumentException("Unknown BorderLayout constraints: " + constraints);
		}
	}
}
