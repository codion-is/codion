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
package is.codion.swing.common.ui.key;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import java.util.Collection;

import static java.awt.event.KeyEvent.VK_UNDEFINED;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_FOCUSED;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * A factory for key event builders.
 * {@snippet :
 * JTextField textField = new JTextField();
 *
 * KeyEvents.builder()
 * 					.keyCode(VK_DOWN)
 *          .onKeyRelease(false)
 *          .modifiers(CTRL_DOWN_MASK)
 *          .condition(WHEN_FOCUSED)
 *          .action(new FindNextAction())
 *          .enable(textField);
 *}
 * @see #builder()
 */
public final class KeyEvents {

	private KeyEvents() {}

	/**
	 * Instantiates a new {@link KeyEvents.Builder} instance.
	 * Note that an Action must be set via {@link Builder#action(Action)} before enabling/disabling.
	 * @return a {@link Builder} instance.
	 */
	public static Builder builder() {
		return new DefaultBuilder();
	}

	/**
	 * Returns a {@link KeyStroke} with the given keyCode and no modifiers.
	 * @param keyCode the key code
	 * @return a keystroke
	 * @see KeyStroke#getKeyStroke(int, int)
	 */
	public static KeyStroke keyStroke(int keyCode) {
		return keyStroke(keyCode, 0);
	}

	/**
	 * Returns a {@link KeyStroke} with the given keyCode and modifiers.
	 * @param keyCode the key code
	 * @param modifiers the modifiers
	 * @return a keystroke
	 * @see KeyStroke#getKeyStroke(int, int)
	 */
	public static KeyStroke keyStroke(int keyCode, int modifiers) {
		return getKeyStroke(keyCode, modifiers);
	}

	/**
	 * Returns a {@link KeyStroke} parsed from the given string.
	 * @param keyStroke the keystroke to parse
	 * @return a keystroke
	 * @see KeyStroke#getKeyStroke(String)
	 */
	public static KeyStroke keyStroke(String keyStroke) {
		return getKeyStroke(keyStroke);
	}

	/**
	 * A Builder for adding a key event to a component, with a default onKeyRelease trigger
	 * and condition {@link JComponent#WHEN_FOCUSED}.
	 * @see KeyEvents#builder()
	 */
	public interface Builder {

		/**
		 * @param keyCode the key code
		 * @return this builder instance
		 */
		Builder keyCode(int keyCode);

		/**
		 * @param keyChar the key char
		 * @return this builder instance
		 */
		Builder keyChar(char keyChar);

		/**
		 * @param modifiers the modifiers
		 * @return this builder instance
		 */
		Builder modifiers(int modifiers);

		/**
		 * Default false.
		 * @param onKeyRelease true if on key release
		 * @return this builder instance
		 */
		Builder onKeyRelease(boolean onKeyRelease);

		/**
		 * @param keyStroke the keyStroke
		 * @return this builder instance
		 */
		Builder keyStroke(KeyStroke keyStroke);

		/**
		 * Sets the key event condition, {@link JComponent#WHEN_FOCUSED} by default.
		 * @param condition the condition
		 * @return this builder instance
		 */
		Builder condition(int condition);

		/**
		 * @param action the action, if null then the action binding is removed
		 * @return this builder instance
		 */
		Builder action(Action action);

		/**
		 * Builds the key event and enables it on the given components
		 * @param components the components
		 * @return this builder instance
		 * @throws IllegalStateException in case no action has been set
		 */
		Builder enable(JComponent... components);

		/**
		 * Builds the key event and enables it on the given components
		 * @param components the components
		 * @return this builder instance
		 * @throws IllegalStateException in case no action has been set
		 */
		Builder enable(Collection<JComponent> components);

		/**
		 * Disables this key event on the given components
		 * @param components the components
		 * @return this builder instance
		 * @throws IllegalStateException in case no action has been set
		 */
		Builder disable(JComponent... components);

		/**
		 * Disables this key event on the given components
		 * @param components the components
		 * @return this builder instance
		 * @throws IllegalStateException in case no action has been set
		 */
		Builder disable(Collection<JComponent> components);
	}

	private static final class DefaultBuilder implements Builder {

		private KeyStroke keyStroke;
		private int condition = WHEN_FOCUSED;
		private @Nullable Action action;

		private DefaultBuilder() {
			this(getKeyStroke(VK_UNDEFINED, 0, false));
		}

		private DefaultBuilder(KeyStroke keyStroke) {
			this.keyStroke = requireNonNull(keyStroke);
		}

		@Override
		public Builder keyCode(int keyCode) {
			this.keyStroke = getKeyStroke(keyCode, keyStroke.getModifiers(), keyStroke.isOnKeyRelease());
			return this;
		}

		@Override
		public Builder keyChar(char keyChar) {
			this.keyStroke = getKeyStroke(keyChar, keyStroke.getModifiers(), keyStroke.isOnKeyRelease());
			return this;
		}

		@Override
		public Builder modifiers(int modifiers) {
			this.keyStroke = getKeyStroke(keyStroke.getKeyCode(), modifiers, keyStroke.isOnKeyRelease());
			return this;
		}

		@Override
		public Builder onKeyRelease(boolean onKeyRelease) {
			this.keyStroke = getKeyStroke(keyStroke.getKeyCode(), keyStroke.getModifiers(), onKeyRelease);
			return this;
		}

		@Override
		public Builder keyStroke(KeyStroke keyStroke) {
			this.keyStroke = requireNonNull(keyStroke);
			return this;
		}

		@Override
		public Builder condition(int condition) {
			this.condition = condition;
			return this;
		}

		@Override
		public Builder action(Action action) {
			this.action = requireNonNull(action);
			return this;
		}

		@Override
		public Builder enable(JComponent... components) {
			return enable(asList(components));
		}

		@Override
		public Builder enable(Collection<JComponent> components) {
			for (JComponent component : requireNonNull(components)) {
				enable(requireNonNull(component), actionMapKey(component));
			}
			return this;
		}

		@Override
		public Builder disable(JComponent... components) {
			return disable(asList(components));
		}

		@Override
		public Builder disable(Collection<JComponent> components) {
			for (JComponent component : requireNonNull(components)) {
				disable(requireNonNull(component), actionMapKey(component));
			}
			return this;
		}

		private Object actionMapKey(JComponent component) {
			if (action == null) {
				throw new IllegalStateException("Unable to enable/disable a key event without an associated action");
			}

			return createActionMapKey(component);
		}

		private String createActionMapKey(JComponent component) {
			Object actionName = action.getValue(Action.NAME);

			return new StringBuilder(component.getClass().getSimpleName())
							.append(actionName == null ? "" : (" " + actionName))
							.append(" ").append(keyStroke)
							.append(" ").append(condition)
							.toString();
		}

		private void enable(JComponent component, Object actionMapKey) {
			component.getActionMap().put(actionMapKey, action);
			component.getInputMap(condition).put(keyStroke, actionMapKey);
			if (component instanceof JComboBox<?>) {
				enable((JComponent) ((JComboBox<?>) component).getEditor().getEditorComponent(), actionMapKey);
			}
			if (component instanceof JSpinner && ((JSpinner) component).getEditor() instanceof JSpinner.DefaultEditor) {
				enable(((JSpinner.DefaultEditor) ((JSpinner) component).getEditor()).getTextField());
			}
		}

		private void disable(JComponent component, Object actionMapKey) {
			component.getActionMap().put(actionMapKey, null);
			component.getInputMap(condition).put(keyStroke, null);
			if (component instanceof JComboBox<?>) {
				disable((JComponent) ((JComboBox<?>) component).getEditor().getEditorComponent(), actionMapKey);
			}
			if (component instanceof JSpinner && ((JSpinner) component).getEditor() instanceof JSpinner.DefaultEditor) {
				disable(((JSpinner.DefaultEditor) ((JSpinner) component).getEditor()).getTextField());
			}
		}
	}
}
