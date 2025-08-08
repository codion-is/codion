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
package is.codion.swing.common.ui.component.splitpane;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultSplitPaneBuilder extends AbstractComponentBuilder<Void, JSplitPane, SplitPaneBuilder> implements SplitPaneBuilder {

	private int orientation = JSplitPane.HORIZONTAL_SPLIT;
	private boolean oneTouchExpandable = false;
	private @Nullable JComponent leftTopComponent;
	private @Nullable JComponent rightBottomComponent;
	private double resizeWeight;
	private boolean continuousLayout;
	private int dividerSize;

	@Override
	public SplitPaneBuilder orientation(int orientation) {
		this.orientation = orientation;
		return this;
	}

	@Override
	public SplitPaneBuilder oneTouchExpandable(boolean oneTouchExpandable) {
		this.oneTouchExpandable = oneTouchExpandable;
		return this;
	}

	@Override
	public SplitPaneBuilder leftComponent(@Nullable JComponent leftComponent) {
		this.leftTopComponent = leftComponent;
		return this;
	}

	@Override
	public SplitPaneBuilder leftComponent(Supplier<? extends JComponent> leftComponent) {
		return leftComponent(requireNonNull(leftComponent).get());
	}

	@Override
	public SplitPaneBuilder rightComponent(@Nullable JComponent rightComponent) {
		this.rightBottomComponent = rightComponent;
		return this;
	}

	@Override
	public SplitPaneBuilder rightComponent(Supplier<? extends JComponent> rightComponent) {
		return rightComponent(requireNonNull(rightComponent).get());
	}

	@Override
	public SplitPaneBuilder topComponent(@Nullable JComponent topComponent) {
		this.leftTopComponent = topComponent;
		return this;
	}

	@Override
	public SplitPaneBuilder topComponent(Supplier<? extends JComponent> topComponent) {
		return topComponent(requireNonNull(topComponent).get());
	}

	@Override
	public SplitPaneBuilder bottomComponent(@Nullable JComponent bottomComponent) {
		this.rightBottomComponent = bottomComponent;
		return this;
	}

	@Override
	public SplitPaneBuilder bottomComponent(Supplier<? extends JComponent> bottomComponent) {
		return bottomComponent(requireNonNull(bottomComponent).get());
	}

	@Override
	public SplitPaneBuilder resizeWeight(double resizeWeight) {
		this.resizeWeight = resizeWeight;
		return this;
	}

	@Override
	public SplitPaneBuilder continuousLayout(boolean continuousLayout) {
		this.continuousLayout = continuousLayout;
		return this;
	}

	@Override
	public SplitPaneBuilder dividerSize(int dividerSize) {
		this.dividerSize = dividerSize;
		return this;
	}

	@Override
	protected JSplitPane createComponent() {
		JSplitPane splitPane = new JSplitPane(orientation);
		splitPane.setLeftComponent(leftTopComponent);
		splitPane.setRightComponent(rightBottomComponent);
		splitPane.setResizeWeight(resizeWeight);
		splitPane.setOneTouchExpandable(oneTouchExpandable);
		splitPane.setContinuousLayout(continuousLayout);
		if (dividerSize > 0) {
			splitPane.setDividerSize(dividerSize);
		}

		return splitPane;
	}

	@Override
	protected ComponentValue<Void, JSplitPane> createComponentValue(JSplitPane component) {
		return new SplitPaneComponentValue(component);
	}

	private static final class SplitPaneComponentValue extends AbstractComponentValue<Void, JSplitPane> {

		private SplitPaneComponentValue(JSplitPane component) {
			super(component);
		}

		@Override
		protected Void getComponentValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setComponentValue(Void value) {
			throw new UnsupportedOperationException();
		}
	}
}
