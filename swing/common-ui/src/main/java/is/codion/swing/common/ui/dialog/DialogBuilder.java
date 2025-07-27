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
package is.codion.swing.common.ui.dialog;

import is.codion.common.observable.Observable;
import is.codion.swing.common.ui.key.KeyEvents;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.util.function.Consumer;

/**
 * A base interface for JDialog builders
 * @param <B> the Builder type
 */
public interface DialogBuilder<B extends DialogBuilder<B>> {

	/**
	 * Sets the dialog owner as the given window.
	 * Also sets the {@link #locationRelativeTo(Component)} to the given window in case one has not been specified.
	 * @param owner the dialog owner
	 * @return this DialogBuilder instance
	 */
	B owner(@Nullable Window owner);

	/**
	 * Sets the dialog owner as the parent window of the given component.
	 * Also sets the {@link #locationRelativeTo(Component)} using the given component in case one has not been specified.
	 * @param owner the dialog parent component
	 * @return this builder instance
	 */
	B owner(@Nullable Component owner);

	/**
	 * @param component the component for the relative location
	 * @return this builder instance
	 */
	B locationRelativeTo(@Nullable Component component);

	/**
	 * Overrides {@link #locationRelativeTo(Component)}.
	 * @param location the location of the dialog
	 * @return this builder instance
	 */
	B location(@Nullable Point location);

	/**
	 * @param title the dialog title
	 * @return this builder instance
	 */
	B title(@Nullable String title);

	/**
	 * @param title an observable for a dynamic dialog title
	 * @return this builder instance
	 */
	B title(@Nullable Observable<String> title);

	/**
	 * @param icon the dialog icon
	 * @return this builder instance
	 */
	B icon(@Nullable ImageIcon icon);

	/**
	 * Enables the key event defined by the given {@link KeyEvents.Builder} on the dialog root pane.
	 * @param keyEventBuilder a key event builder to enable on the dialog root pane
	 * @return this builder instance
	 */
	B keyEvent(KeyEvents.Builder keyEventBuilder);

	/**
	 * @param onBuild called when the dialog has been built, before it is shown
	 * @return this builder instance
	 */
	B onBuild(Consumer<JDialog> onBuild);
}
