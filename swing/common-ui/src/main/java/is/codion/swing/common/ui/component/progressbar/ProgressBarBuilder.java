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

import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.BoundedRangeModel;
import javax.swing.JProgressBar;

/**
 * Builds a JProgressBar.
 */
public interface ProgressBarBuilder extends ComponentValueBuilder<JProgressBar, Integer, ProgressBarBuilder> {

	/**
	 * Note: setting the model also sets {@link #indeterminate(boolean)} to false
	 * @param model the model
	 * @return this builder
	 * @see JProgressBar#setModel(BoundedRangeModel)
	 */
	ProgressBarBuilder model(BoundedRangeModel model);

	/**
	 * @param string a string to paint
	 * @return this builder
	 * @see JProgressBar#setString(String)
	 */
	ProgressBarBuilder string(@Nullable String string);

	/**
	 * @param borderPainted true if a border should be painted
	 * @return this builder
	 * @see JProgressBar#setBorderPainted(boolean)
	 */
	ProgressBarBuilder borderPainted(boolean borderPainted);

	/**
	 * @param stringPainted true if a progress string should be painted
	 * @return this builder
	 * @see JProgressBar#setStringPainted(boolean)
	 */
	ProgressBarBuilder stringPainted(boolean stringPainted);

	/**
	 * @param orientation the orientiation
	 * @return this builder
	 * @see JProgressBar#setOrientation(int)
	 */
	ProgressBarBuilder orientation(int orientation);

	/**
	 * @param indeterminate true if the progress bar should be inditerminate
	 * @return this builder
	 * @see JProgressBar#setIndeterminate(boolean)
	 */
	ProgressBarBuilder indeterminate(boolean indeterminate);

	/**
	 * @return a new JProgressBar
	 */
	JProgressBar build();

	/**
	 * @return a new indeterminate {@link ProgressBarBuilder} instance
	 */
	static ProgressBarBuilder builder() {
		return new DefaultProgressBarBuilder();
	}
}
