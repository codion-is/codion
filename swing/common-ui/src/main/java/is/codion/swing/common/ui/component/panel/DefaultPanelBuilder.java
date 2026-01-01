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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class DefaultPanelBuilder<L extends LayoutManager, B extends PanelBuilder<L, B>>
				extends AbstractComponentBuilder<JPanel, B> implements PanelBuilder<L, B> {

	static final PanelBuilderFactory FACTORY = new DefaultPanelBuilderFactory();

	private final List<ComponentConstraints> componentConstraints = new ArrayList<>();

	private @Nullable JPanel panel;
	private @Nullable L layout;

	DefaultPanelBuilder() {}

	@Override
	public final B panel(@Nullable JPanel panel) {
		this.panel = panel;
		return (B) this;
	}

	@Override
	public final B layout(@Nullable L layoutManager) {
		layout = layoutManager;
		return (B) this;
	}

	@Override
	public final B add(JComponent component) {
		componentConstraints.add(new ComponentConstraints(requireNonNull(component)));
		return (B) this;
	}

	@Override
	public final B add(Supplier<? extends JComponent> component) {
		return add(requireNonNull(component).get());
	}

	@Override
	public final B add(JComponent component, Object constraints) {
		validateConstraints(constraints);
		componentConstraints.add(new ComponentConstraints(requireNonNull(component), requireNonNull(constraints)));
		return (B) this;
	}

	@Override
	public final B add(Supplier<? extends JComponent> component, Object constraints) {
		return add(requireNonNull(component).get(), constraints);
	}

	@Override
	public final B addAll(JComponent... components) {
		addAll(Arrays.asList(components));
		return (B) this;
	}

	@Override
	public final B addAll(Collection<? extends JComponent> components) {
		requireNonNull(components).forEach(new AddComponents(componentConstraints));
		return (B) this;
	}

	@Override
	protected JPanel createComponent() {
		JPanel component = panel == null ? new JPanel() : panel;
		if (layout != null) {
			component.setLayout(layout);
		}
		componentConstraints.forEach(new AddToPanel(component));

		return component;
	}

	/**
	 * @param constraints the constraints to validate
	 * @throws IllegalArgumentException in case the constraints don't match the layout
	 */
	protected void validateConstraints(Object constraints) {
		if (constraints instanceof JComponent) {
			throw new IllegalArgumentException("Use addAll() when adding multiple components");
		}
	}

	private static final class DefaultPanelBuilderFactory implements PanelBuilderFactory {

		@Override
		public <L extends LayoutManager, B extends PanelBuilder<L, B>> PanelBuilder<L, B> layout(L layout) {
			return (PanelBuilder<L, B>) new DefaultPanelBuilder<>().layout(layout);
		}

		@Override
		public BorderLayoutPanelBuilder borderLayout() {
			return new DefaultBorderLayoutPanelBuilder();
		}

		@Override
		public BorderLayoutPanelBuilder borderLayout(BorderLayout layout) {
			return new DefaultBorderLayoutPanelBuilder(requireNonNull(layout));
		}

		@Override
		public FlowLayoutPanelBuilder flowLayout(int align) {
			return new DefaultFlowLayoutPanelBuilder(align);
		}

		@Override
		public FlowLayoutPanelBuilder flowLayout(FlowLayout layout) {
			return new DefaultFlowLayoutPanelBuilder(requireNonNull(layout));
		}

		@Override
		public GridLayoutPanelBuilder gridLayout(int rows, int columns) {
			return new DefaultGridLayoutPanelBuilder(rows, columns);
		}

		@Override
		public GridLayoutPanelBuilder gridLayout(GridLayout layout) {
			return new DefaultGridLayoutPanelBuilder(requireNonNull(layout));
		}

		@Override
		public FlexibleGridLayoutPanelBuilder flexibleGridLayout(int rows, int columns) {
			return new DefaultFlexibleGridLayoutPanelBuilder(rows, columns);
		}

		@Override
		public FlexibleGridLayoutPanelBuilder flexibleGridLayout(FlexibleGridLayout layout) {
			return new DefaultFlexibleGridLayoutPanelBuilder(requireNonNull(layout));
		}
	}

	private static final class ComponentConstraints {

		private final JComponent component;
		private final @Nullable Object constraints;

		private ComponentConstraints(JComponent component) {
			this(component, null);
		}

		private ComponentConstraints(JComponent component, @Nullable Object constraints) {
			this.component = component;
			this.constraints = constraints;
		}
	}

	private static final class AddComponents implements Consumer<JComponent> {

		private final List<ComponentConstraints> componentConstraints;

		private AddComponents(List<ComponentConstraints> componentConstraints) {
			this.componentConstraints = componentConstraints;
		}

		@Override
		public void accept(JComponent component) {
			componentConstraints.add(new ComponentConstraints(requireNonNull(component)));
		}
	}

	private static final class AddToPanel implements Consumer<ComponentConstraints> {

		private final JPanel panel;

		private AddToPanel(JPanel panel) {
			this.panel = panel;
		}

		@Override
		public void accept(ComponentConstraints componentConstraint) {
			if (componentConstraint.constraints != null) {
				panel.add(componentConstraint.component, componentConstraint.constraints);
			}
			else {
				panel.add(componentConstraint.component);
			}
		}
	}
}
