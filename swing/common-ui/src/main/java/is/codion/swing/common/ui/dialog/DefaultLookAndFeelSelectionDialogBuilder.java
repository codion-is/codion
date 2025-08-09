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
package is.codion.swing.common.ui.dialog;

import is.codion.common.resource.MessageBundle;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

final class DefaultLookAndFeelSelectionDialogBuilder implements LookAndFeelSelectionDialogBuilder {

	private static final MessageBundle MESSAGES =
					messageBundle(DefaultLookAndFeelSelectionDialogBuilder.class,
									getBundle(DefaultLookAndFeelSelectionDialogBuilder.class.getName()));

	private static final int PADDING = 10;

	private @Nullable JComponent owner;
	private boolean enableOnSelection = LookAndFeelComboBox.ENABLE_ON_SELECTION.getOrThrow();

	@Override
	public LookAndFeelSelectionDialogBuilder owner(@Nullable JComponent owner) {
		this.owner = owner;
		return this;
	}

	@Override
	public LookAndFeelSelectionDialogBuilder enableOnSelection(boolean enableOnSelection) {
		this.enableOnSelection = enableOnSelection;
		return this;
	}

	@Override
	public Control createControl() {
		return createControl(enabler -> {});
	}

	@Override
	public Control createControl(Consumer<LookAndFeelEnabler> selectedLookAndFeel) {
		requireNonNull(selectedLookAndFeel);
		String caption = MESSAGES.getString("look_and_feel");

		return Control.builder()
						.command(() -> selectLookAndFeel(selectedLookAndFeel))
						.caption(caption)
						.build();
	}

	@Override
	public void selectLookAndFeel(Consumer<LookAndFeelEnabler> selectedLookAndFeel) {
		requireNonNull(selectedLookAndFeel);
		LookAndFeelComboBox lookAndFeelComboBox = LookAndFeelComboBox.builder()
						.enableOnSelection(enableOnSelection)
						.build();
		JPanel basePanel = new JPanel(new BorderLayout());
		basePanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, 0, PADDING));
		basePanel.add(lookAndFeelComboBox, BorderLayout.CENTER);
		if (auxiliaryLookAndFeelsAvailable()) {
			basePanel.add(PanelBuilder.builder()
							.layout(new FlowLayout(FlowLayout.TRAILING))
							.add(CheckBoxBuilder.builder()
											.link(lookAndFeelComboBox.includeInstalled())
											.text(MESSAGES.getString("include_installed"))
											.mnemonic(MESSAGES.getString("include_installed_mnemonic").charAt(0))
											.includeText(true))
							.build(), BorderLayout.SOUTH);
		}
		new DefaultOkCancelDialogBuilder()
						.component(basePanel)
						.owner(owner)
						.title(MESSAGES.getString("look_and_feel"))
						.onOk(() -> selectedLookAndFeel.accept(lookAndFeelComboBox.selectedLookAndFeel()))
						.onCancel(lookAndFeelComboBox::revert)
						.show();
	}

	private static boolean auxiliaryLookAndFeelsAvailable() {
		return LookAndFeelProvider.instances().size() > 1;
	}
}
