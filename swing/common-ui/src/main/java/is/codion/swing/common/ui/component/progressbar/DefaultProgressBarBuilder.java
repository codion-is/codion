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
package is.codion.swing.common.ui.component.progressbar;

import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JProgressBar;

import static java.util.Objects.requireNonNull;

final class DefaultProgressBarBuilder extends AbstractComponentValueBuilder<Integer, JProgressBar, ProgressBarBuilder> implements ProgressBarBuilder {

	private BoundedRangeModel boundedRangeModel = new DefaultBoundedRangeModel();
	private boolean borderPainted;
	private boolean stringPainted;
	private int orientation;
	private boolean indeterminate = true;
	private @Nullable String string;

	DefaultProgressBarBuilder() {}

	@Override
	public ProgressBarBuilder model(BoundedRangeModel model) {
		this.boundedRangeModel = requireNonNull(model);
		return indeterminate(false);
	}

	@Override
	public ProgressBarBuilder string(@Nullable String string) {
		this.string = string;
		return this;
	}

	@Override
	public ProgressBarBuilder borderPainted(boolean borderPainted) {
		this.borderPainted = borderPainted;
		return this;
	}

	@Override
	public ProgressBarBuilder stringPainted(boolean stringPainted) {
		this.stringPainted = stringPainted;
		return this;
	}

	@Override
	public ProgressBarBuilder orientation(int orientation) {
		this.orientation = orientation;
		return this;
	}

	@Override
	public ProgressBarBuilder indeterminate(boolean indeterminate) {
		this.indeterminate = indeterminate;
		return this;
	}

	@Override
	protected JProgressBar createComponent() {
		JProgressBar progressBar = new JProgressBar(boundedRangeModel);
		progressBar.setBorderPainted(borderPainted);
		progressBar.setString(string);
		progressBar.setStringPainted(stringPainted);
		progressBar.setOrientation(orientation);
		progressBar.setIndeterminate(indeterminate);

		return progressBar;
	}

	@Override
	protected ComponentValue<Integer, JProgressBar> createComponentValue(JProgressBar component) {
		return new IntegerProgressBarValue(component);
	}

	private static final class IntegerProgressBarValue extends AbstractComponentValue<Integer, JProgressBar> {

		private IntegerProgressBarValue(JProgressBar progressBar) {
			super(progressBar, 0);
			progressBar.getModel().addChangeListener(e -> notifyListeners());
		}

		@Override
		protected Integer getComponentValue() {
			return component().getValue();
		}

		@Override
		protected void setComponentValue(Integer value) {
			component().setValue(value == null ? 0 : value);
		}
	}
}
