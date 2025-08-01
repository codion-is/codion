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
 * Copyright (c) 2017 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.component.Sizes;
import is.codion.swing.common.ui.component.button.ButtonPanelBuilder;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.Controls.ControlsBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.flowLayout;
import static java.util.Objects.requireNonNull;

/**
 * A dialog containing a progress bar.
 * @see Dialogs#progress()
 */
public final class ProgressDialog extends JDialog {

	private static final int DEFAULT_PROGRESS_BAR_WIDTH = 400;

	private final JProgressBar progressBar;

	private ProgressDialog(DefaultBuilder builder) {
		super(builder.owner, builder.owner == null ? ModalityType.MODELESS : ModalityType.APPLICATION_MODAL);
		if (builder.title != null) {
			setTitle(builder.title.get());
			builder.title.addConsumer(this::setTitle);
		}
		if (builder.icon != null) {
			setIconImage(builder.icon.getImage());
		}
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		progressBar = createProgressBar(builder);
		initializeUI(builder);
		setLocationRelativeTo(builder.locationRelativeTo == null ? builder.owner : builder.locationRelativeTo);
	}

	/**
	 * Sets the progress in the underlying JProgressBar
	 * @param progress the progress (0 - maximum)
	 */
	public void setProgress(int progress) {
		progressBar.getModel().setValue(progress);
	}

	/**
	 * Sets the message displayed on the JProgress bar
	 * @param message the message
	 */
	public void setMessage(@Nullable String message) {
		progressBar.setString(message);
	}

	private void initializeUI(DefaultBuilder builder) {
		setLayout(borderLayout());
		add(createCenterPanel(builder), BorderLayout.CENTER);
		pack();
	}

	private JPanel createCenterPanel(DefaultBuilder builder) {
		BorderLayoutPanelBuilder basePanelBuilder = BorderLayoutPanelBuilder.builder();
		if (builder.border != null) {
			basePanelBuilder.border(builder.border);
		}
		if (builder.northPanel != null) {
			basePanelBuilder.northComponent(builder.northPanel);
		}
		if (builder.westPanel != null) {
			basePanelBuilder.westComponent(builder.westPanel);
		}
		if (builder.eastPanel != null) {
			basePanelBuilder.eastComponent(builder.eastPanel);
		}
		basePanelBuilder.centerComponent(progressBar);
		Controls controls = builder.controls.build();
		if (controls.size() > 0) {
			basePanelBuilder.southComponent(PanelBuilder.builder()
							.layout(flowLayout(FlowLayout.TRAILING))
							.add(ButtonPanelBuilder.builder()
											.controls(controls)
											.build())
							.build());
		}

		return basePanelBuilder.build();
	}

	private static JProgressBar createProgressBar(DefaultBuilder builder) {
		JProgressBar progressBar = new JProgressBar();
		progressBar.setStringPainted(builder.stringPainted);
		if (builder.progressBarSize != null) {
			progressBar.setPreferredSize(builder.progressBarSize);
		}
		else {
			Sizes.preferredWidth(progressBar, DEFAULT_PROGRESS_BAR_WIDTH);
		}
		if (builder.indeterminate) {
			progressBar.setIndeterminate(true);
		}
		else {
			progressBar.setMaximum(builder.maximum);
		}

		return progressBar;
	}

	/**
	 * A builder for {@link ProgressDialog}.
	 */
	public interface Builder extends DialogBuilder<Builder> {

		/**
		 * @param indeterminate the indeterminate status of the progress bar
		 * @return this ProgressDialogBuilder instance
		 */
		Builder indeterminate(boolean indeterminate);

		/**
		 * Note that calling this method renders the progress bar determinate
		 * @param maximum the maximum progress, 100 by default
		 * @return this ProgressDialogBuilder instance
		 * @see #indeterminate(boolean)
		 */
		Builder maximum(int maximum);

		/**
		 * @param stringPainted the string painted status of the progress bar
		 * @return this ProgressDialogBuilder instance
		 */
		Builder stringPainted(boolean stringPainted);

		/**
		 * @param northPanel if specified this panel is added to the {@link BorderLayout#NORTH} position
		 * @return this ProgressDialogBuilder instance
		 */
		Builder northPanel(@Nullable JPanel northPanel);

		/**
		 * @param westPanel if specified this panel is added to the {@link BorderLayout#WEST} position
		 * @return this ProgressDialogBuilder instance
		 */
		Builder westPanel(@Nullable JPanel westPanel);

		/**
		 * @param eastPanel if specified this panel is added to the {@link BorderLayout#EAST} position
		 * @return this ProgressDialogBuilder instance
		 */
		Builder eastPanel(@Nullable JPanel eastPanel);

		/**
		 * Adds a button based on the given control to the {@link BorderLayout#SOUTH} position
		 * @param controlBuilder the builder for the control to add
		 * @return this ProgressDialogBuilder instance
		 */
		Builder control(Control.Builder<?, ?> controlBuilder);

		/**
		 * Adds a button based on the given control to the {@link BorderLayout#SOUTH} position
		 * @param control the control to add
		 * @return this ProgressDialogBuilder instance
		 */
		Builder control(Control control);

		/**
		 * @param progressBarSize the progress bar size
		 * @return this ProgressDialogBuilder instance
		 */
		Builder progressBarSize(@Nullable Dimension progressBarSize);

		/**
		 * @param border the border to add around the progress bar
		 * @return this ProgressDialogBuilder instance
		 */
		Builder border(@Nullable Border border);

		/**
		 * @return a new ProgressDialog
		 */
		ProgressDialog build();
	}

	static class DefaultBuilder extends AbstractDialogBuilder<Builder> implements Builder {

		private final ControlsBuilder controls = Controls.builder();

		private int maximum = 100;
		private boolean indeterminate = true;
		private boolean stringPainted = false;
		private @Nullable JPanel northPanel;
		private @Nullable JPanel westPanel;
		private @Nullable JPanel eastPanel;
		private @Nullable Dimension progressBarSize;
		private @Nullable Border border;

		@Override
		public Builder indeterminate(boolean indeterminate) {
			this.indeterminate = indeterminate;
			return this;
		}

		@Override
		public Builder maximum(int maximum) {
			if (maximum < 0) {
				throw new IllegalArgumentException("Maximum progress must be a positive integer");
			}
			this.maximum = maximum;
			return indeterminate(false);
		}

		@Override
		public Builder stringPainted(boolean stringPainted) {
			this.stringPainted = stringPainted;
			return this;
		}

		@Override
		public Builder northPanel(@Nullable JPanel northPanel) {
			this.northPanel = northPanel;
			return this;
		}

		@Override
		public Builder westPanel(@Nullable JPanel westPanel) {
			this.westPanel = westPanel;
			return this;
		}

		@Override
		public Builder eastPanel(@Nullable JPanel eastPanel) {
			this.eastPanel = eastPanel;
			return this;
		}

		@Override
		public Builder control(Control.Builder<?, ?> controlBuilder) {
			return control(requireNonNull(controlBuilder).build());
		}

		@Override
		public Builder control(Control control) {
			this.controls.control(requireNonNull(control));
			return this;
		}

		@Override
		public Builder progressBarSize(@Nullable Dimension progressBarSize) {
			this.progressBarSize = progressBarSize;
			return this;
		}

		@Override
		public Builder border(@Nullable Border border) {
			this.border = border;
			return this;
		}

		@Override
		public ProgressDialog build() {
			ProgressDialog progressDialog = new ProgressDialog(this);
			onBuildConsumers.forEach(consumer -> consumer.accept(progressDialog));

			return progressDialog;
		}
	}
}
