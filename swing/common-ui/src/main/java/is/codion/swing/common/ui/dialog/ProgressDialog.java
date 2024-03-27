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
 * Copyright (c) 2017 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.component.button.ButtonPanelBuilder;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.control.Controls;

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

/**
 * A dialog containing a progress bar.
 * @see Dialogs#progressDialog()
 */
public final class ProgressDialog extends JDialog {

	private static final int DEFAULT_PROGRESS_BAR_WIDTH = 400;

	private final JProgressBar progressBar;

	private ProgressDialog(DefaultBuilder builder) {
		super(builder.owner, builder.owner == null ? ModalityType.MODELESS : ModalityType.APPLICATION_MODAL);
		if (builder.titleProvider != null) {
			setTitle(builder.titleProvider.get());
			builder.titleProvider.addDataListener(this::setTitle);
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
	 * @param progress the progress (0 - maximumProgress)
	 */
	public void setProgress(int progress) {
		progressBar.getModel().setValue(progress);
	}

	/**
	 * Sets the message displayed on the JProgress bar
	 * @param message the message
	 */
	public void setMessage(String message) {
		progressBar.setString(message);
	}

	private void initializeUI(DefaultBuilder builder) {
		setLayout(borderLayout());
		add(createCenterPanel(builder), BorderLayout.CENTER);
		pack();
	}

	private JPanel createCenterPanel(DefaultBuilder builder) {
		BorderLayoutPanelBuilder basePanelBuilder = BorderLayoutPanelBuilder.builder(borderLayout());
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
		if (builder.controls != null) {
			basePanelBuilder.southComponent(PanelBuilder.builder(flowLayout(FlowLayout.TRAILING))
							.add(ButtonPanelBuilder.builder(builder.controls).build())
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
			Sizes.setPreferredWidth(progressBar, DEFAULT_PROGRESS_BAR_WIDTH);
		}
		if (builder.indeterminate) {
			progressBar.setIndeterminate(true);
		}
		else {
			progressBar.setMaximum(builder.maximumProgress);
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
		 * @param maximumProgress the maximum progress, 100 by default
		 * @return this ProgressDialogBuilder instance
		 * @see #indeterminate(boolean)
		 */
		Builder maximumProgress(int maximumProgress);

		/**
		 * @param stringPainted the string painted status of the progress bar
		 * @return this ProgressDialogBuilder instance
		 */
		Builder stringPainted(boolean stringPainted);

		/**
		 * @param northPanel if specified this panel is added to the {@link BorderLayout#NORTH} position
		 * @return this ProgressDialogBuilder instance
		 */
		Builder northPanel(JPanel northPanel);

		/**
		 * @param westPanel if specified this panel is added to the {@link BorderLayout#WEST} position
		 * @return this ProgressDialogBuilder instance
		 */
		Builder westPanel(JPanel westPanel);

		/**
		 * @param eastPanel if specified this panel is added to the {@link BorderLayout#EAST} position
		 * @return this ProgressDialogBuilder instance
		 */
		Builder eastPanel(JPanel eastPanel);

		/**
		 * @param controls if specified buttons based on these controls are added to the {@link BorderLayout#SOUTH} position
		 * @return this ProgressDialogBuilder instance
		 */
		Builder controls(Controls controls);

		/**
		 * @param progressBarSize the progress bar size
		 * @return this ProgressDialogBuilder instance
		 */
		Builder progressBarSize(Dimension progressBarSize);

		/**
		 * @param border the border to add around the progress bar
		 * @return this ProgressDialogBuilder instance
		 */
		Builder border(Border border);

		/**
		 * @return a new ProgressDialog
		 */
		ProgressDialog build();
	}

	static class DefaultBuilder extends AbstractDialogBuilder<Builder> implements Builder {

		private int maximumProgress = 100;
		private boolean indeterminate = true;
		private boolean stringPainted = false;
		private JPanel northPanel;
		private JPanel westPanel;
		private JPanel eastPanel;
		private Controls controls;
		private Dimension progressBarSize;
		private Border border;

		@Override
		public Builder indeterminate(boolean indeterminate) {
			this.indeterminate = indeterminate;
			return this;
		}

		@Override
		public Builder maximumProgress(int maximumProgress) {
			if (maximumProgress < 0) {
				throw new IllegalArgumentException("Maximum progress must be a positive integer");
			}
			this.maximumProgress = maximumProgress;
			return indeterminate(false);
		}

		@Override
		public Builder stringPainted(boolean stringPainted) {
			this.stringPainted = stringPainted;
			return this;
		}

		@Override
		public Builder northPanel(JPanel northPanel) {
			this.northPanel = northPanel;
			return this;
		}

		@Override
		public Builder westPanel(JPanel westPanel) {
			this.westPanel = westPanel;
			return this;
		}

		@Override
		public Builder eastPanel(JPanel eastPanel) {
			this.eastPanel = eastPanel;
			return this;
		}

		@Override
		public Builder controls(Controls controls) {
			this.controls = controls;
			return this;
		}

		@Override
		public Builder progressBarSize(Dimension progressBarSize) {
			this.progressBarSize = progressBarSize;
			return this;
		}

		@Override
		public Builder border(Border border) {
			this.border = border;
			return this;
		}

		@Override
		public ProgressDialog build() {
			return new ProgressDialog(this);
		}
	}
}
