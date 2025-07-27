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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.control.Control;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Insets;
import java.awt.event.ActionListener;

/**
 * Builds buttons.
 * @param <T> the value type
 * @param <C> the button type
 * @param <B> the builder type
 */
public interface ButtonBuilder<T, C extends AbstractButton, B extends ButtonBuilder<T, C, B>> extends ComponentBuilder<T, C, B> {

	/**
	 * @param text the button caption text
	 * @return this builder instance
	 * @see JButton#setText(String)
	 */
	B text(@Nullable String text);

	/**
	 * @param mnemonic the mnemonic
	 * @return this builder instance
	 * @see JButton#setMnemonic(int)
	 */
	B mnemonic(int mnemonic);

	/**
	 * Note that setting this to false overrides the caption text from the action, if one is specified.
	 * @param includeText specifies whether a caption text should be included
	 * @return this builder instance
	 */
	B includeText(boolean includeText);

	/**
	 * @param horizontalAlignment the horizontal alignment
	 * @return this builder instance
	 * @see AbstractButton#setHorizontalAlignment(int)
	 */
	B horizontalAlignment(int horizontalAlignment);

	/**
	 * @param verticalAlignment the vertical alignment
	 * @return this builder instance
	 * @see AbstractButton#setVerticalAlignment(int)
	 */
	B verticalAlignment(int verticalAlignment);

	/**
	 * @param horizontalTextPosition the horizontal text position
	 * @return this builder instance
	 * @see AbstractButton#setHorizontalTextPosition(int)
	 */
	B horizontalTextPosition(int horizontalTextPosition);

	/**
	 * @param verticalTextPosition the vertical text position
	 * @return this builder instance
	 * @see AbstractButton#setVerticalTextPosition(int)
	 */
	B verticalTextPosition(int verticalTextPosition);

	/**
	 * @param icon the icon
	 * @return this builder instance
	 * @see JButton#setIcon(Icon)
	 */
	B icon(@Nullable Icon icon);

	/**
	 * @param pressedIcon the icon
	 * @return this builder instance
	 * @see JButton#setPressedIcon(Icon)
	 */
	B pressedIcon(@Nullable Icon pressedIcon);

	/**
	 * @param selectedIcon the icon
	 * @return this builder instance
	 * @see JButton#setSelectedIcon(Icon)
	 */
	B selectedIcon(@Nullable Icon selectedIcon);

	/**
	 * @param rolloverIcon the icon
	 * @return this builder instance
	 * @see JButton#setRolloverIcon(Icon)
	 */
	B rolloverIcon(@Nullable Icon rolloverIcon);

	/**
	 * @param rolloverSelectedIcon the icon
	 * @return this builder instance
	 * @see JButton#setRolloverSelectedIcon(Icon)
	 */
	B rolloverSelectedIcon(@Nullable Icon rolloverSelectedIcon);

	/**
	 * @param disabledIcon the icon
	 * @return this builder instance
	 * @see JButton#setDisabledIcon(Icon)
	 */
	B disabledIcon(@Nullable Icon disabledIcon);

	/**
	 * @param disabledSelectedIcon the icon
	 * @return this builder instance
	 * @see JButton#setIcon(Icon)
	 */
	B disabledSelectedIcon(@Nullable Icon disabledSelectedIcon);

	/**
	 * @param iconTextGap the icon text gap
	 * @return this builder instance
	 * @see AbstractButton#setIconTextGap(int)
	 */
	B iconTextGap(int iconTextGap);

	/**
	 * @param insets the margin insets
	 * @return this builder instance
	 * @see JButton#setMargin(Insets)
	 */
	B margin(@Nullable Insets insets);

	/**
	 * @param borderPainted true if the border should be painted
	 * @return this builder instance
	 * @see AbstractButton#setBorderPainted(boolean)
	 */
	B borderPainted(boolean borderPainted);

	/**
	 * @param contentAreaFilled true if content area should be filled
	 * @return this builder instance
	 * @see AbstractButton#setContentAreaFilled(boolean)
	 */
	B contentAreaFilled(boolean contentAreaFilled);

	/**
	 * @param focusPainted true if focus should be painted
	 * @return this builder instance
	 * @see AbstractButton#setFocusPainted(boolean)
	 */
	B focusPainted(boolean focusPainted);

	/**
	 * @param rolloverEnabled true if rollover should be enabled
	 * @return this builder instance
	 * @see AbstractButton#setRolloverEnabled(boolean)
	 */
	B rolloverEnabled(boolean rolloverEnabled);

	/**
	 * @param multiClickThreshold the multi click threshold
	 * @return this builder instance
	 * @see AbstractButton#setMultiClickThreshhold(long)
	 */
	B multiClickThreshold(long multiClickThreshold);

	/**
	 * @param buttonGroup the group to add the button to
	 * @return this builder instance
	 */
	B buttonGroup(@Nullable ButtonGroup buttonGroup);

	/**
	 * Sets the inital selected status of the button, overridden by initial value.
	 * @param selected the initial selected status of the button
	 * @return this builder instance
	 * @see AbstractButton#setSelected(boolean)
	 */
	B selected(boolean selected);

	/**
	 * @param action the button action
	 * @return this builder instance
	 * @see AbstractButton#setAction(Action)
	 */
	B action(@Nullable Action action);

	/**
	 * @param control the control to base the button on
	 * @return this builder instance
	 */
	B control(Control control);

	/**
	 * @param controlBuilder the builder for the control to base the button on
	 * @return this builder instance
	 */
	B control(Control.Builder<?, ?> controlBuilder);

	/**
	 * @param actionListener the action listener
	 * @return this builder instance
	 * @see AbstractButton#addActionListener(ActionListener)
	 */
	B actionListener(ActionListener actionListener);

	/**
	 * @param <B> the builder type
	 * @return a builder for a JButton
	 */
	static <B extends ButtonBuilder<Void, JButton, B>> ButtonBuilder<Void, JButton, B> builder() {
		return new DefaultButtonBuilder<>();
	}
}
