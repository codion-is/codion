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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.component.combobox.CompletionDocument.AutoCompletionDocument;
import is.codion.swing.common.ui.component.combobox.CompletionDocument.CompletionFocusListener;
import is.codion.swing.common.ui.component.combobox.CompletionDocument.MaximumMatchDocument;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.enumValue;
import static java.util.Objects.requireNonNull;

/**
 * Selects an item in a JComboBox based on values typed on the keyboard.
 * <pre>
 * {@code
 *   Completion.builder()
 *       .mode(Mode.AUTOCOMPLETE)
 *       .normalize(false)
 *       .enable(comboBox);
 * }
 *</pre>
 * <p>
 * Based on code originally from: <a href="http://www.orbital-computer.de/JComboBox">http://www.orbital-computer.de/JComboBox</a><br>
 * Included with permission.
 * @author Thomas Bierhance
 * @author Björn Darri Sigurðsson
 */
public final class Completion {

	/**
	 * The available completion modes.
	 */
	public enum Mode {
		/**
		 * Identifies the completion mode MaximumMatch
		 */
		MAXIMUM_MATCH,
		/**
		 * Identifies the completion mode AutoCompletion
		 */
		AUTOCOMPLETE,
		/**
		 * Identifies the no completion mode
		 */
		NONE
	}

	/**
	 * Specifies the default completion model used for comboboxes.
	 * <ul>
	 * <li>{@link Completion.Mode#MAXIMUM_MATCH} for maximum match,
	 * <li>{@link Completion.Mode#AUTOCOMPLETE} for auto-completion or
	 * <li>{@link Completion.Mode#NONE} for no completion.
	 * <li>Value type:String
	 * <li>Default value: {@link Completion.Mode#MAXIMUM_MATCH}
	 * </ul>
	 */
	public static final PropertyValue<Mode> COMPLETION_MODE =
					enumValue(Completion.class.getName() + ".completionMode", Mode.class, Mode.MAXIMUM_MATCH);

	/**
	 * Specifies whether strings are normalized during completion by default.
	 * <ul>
	 * <li>Value type:Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> NORMALIZE =
					booleanValue(Completion.class.getName() + ".normalize", true);

	private Completion() {}

	/**
	 * @return a new {@link Builder} instance.
	 */
	public static Builder builder() {
		return new DefaultBuilder();
	}

	/**
	 * A builder for combo box completion.
	 */
	public interface Builder {

		/**
		 * @param mode the mode to enable
		 * @return this builder
		 */
		Builder mode(Mode mode);

		/**
		 * @param normalize true if accented characters should be normalized before matching
		 * @return this builder
		 */
		Builder normalize(boolean normalize);

		/**
		 * @param comboBox the combo box on which to enable completion
		 */
		void enable(JComboBox<?> comboBox);
	}

	private static final class DefaultBuilder implements Builder {

		private Mode mode = COMPLETION_MODE.get();
		private boolean normalize = NORMALIZE.getOrThrow();

		@Override
		public Builder mode(Mode mode) {
			this.mode = requireNonNull(mode);
			return this;
		}

		@Override
		public Builder normalize(boolean normalize) {
			this.normalize = normalize;
			return this;
		}

		@Override
		public void enable(JComboBox<?> comboBox) {
			requireNonNull(comboBox);
			switch (mode) {
				case NONE:
					break;
				case AUTOCOMPLETE:
					new AutoCompletionDocument(comboBox, normalize);
					break;
				case MAXIMUM_MATCH:
					new MaximumMatchDocument(comboBox, normalize);
					break;
				default:
					throw new IllegalArgumentException("Unknown completion mode: " + mode);
			}
			comboBox.addFocusListener(new CompletionFocusListener((JTextComponent) comboBox.getEditor().getEditorComponent()));
		}
	}
}
