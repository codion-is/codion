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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common;

import is.codion.common.property.PropertyValue;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static is.codion.common.Configuration.stringValue;
import static java.util.Objects.requireNonNull;

/**
 * A utility class for working with text, such as collating and padding.
 */
public final class Text {

	/**
	 * Specifies the default collator locale language.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: {@code Locale.getDefault().getLanguage()}.
	 * </ul>
	 * @see #collator()
	 * @see #collate(List)
	 * @see Locale#toLanguageTag()
	 */
	public static final PropertyValue<String> DEFAULT_COLLATOR_LANGUAGE =
					stringValue("codion.defaultCollatorLanguage", Locale.getDefault().getLanguage());

	private Text() {}

	/**
	 * Sorts the string representations of the list contents, using the space aware collator
	 * @param values the list to sort (collate)
	 * @param <T> the list element type
	 * @return the sorted list
	 * @see Text#collator()
	 */
	public static <T> List<T> collate(List<T> values) {
		requireNonNull(values).sort(collator());

		return values;
	}

	/**
	 * Creates a Comparator which compares the string representations of the objects
	 * using the default Collator, taking spaces into account.
	 * @param <T> the type of the objects to compare
	 * @return a space aware collator
	 * @see #DEFAULT_COLLATOR_LANGUAGE
	 */
	public static <T> Comparator<T> collator() {
		return collator(new Locale(DEFAULT_COLLATOR_LANGUAGE.getOrThrow()));
	}

	/**
	 * Creates a Comparator which compares the string representations of the objects
	 * using the default Collator, taking spaces into account.
	 * @param <T> the type of the objects to compare
	 * @param locale the collator locale
	 * @return a space aware collator
	 * @see #DEFAULT_COLLATOR_LANGUAGE
	 */
	public static <T> Comparator<T> collator(Locale locale) {
		return new SpaceAwareComparator<>(requireNonNull(locale));
	}

	/**
	 * Right pads the given string with the given pad character until a length of {@code length} has been reached
	 * @param string the string to pad
	 * @param length the desired length
	 * @param padChar the character to use for padding
	 * @return the padded string
	 */
	public static String rightPad(String string, int length, char padChar) {
		return padString(string, length, padChar, false);
	}

	/**
	 * Left pads the given string with the given pad character until a length of {@code length} has been reached
	 * @param string the string to pad
	 * @param length the desired length
	 * @param padChar the character to use for padding
	 * @return the padded string
	 */
	public static String leftPad(String string, int length, char padChar) {
		return padString(string, length, padChar, true);
	}

	/**
	 * Splits and trims the given comma separated string.
	 * Returns an empty list in case of null or empty string argument.
	 * @param csv a String with comma separated values
	 * @return the trimmed values
	 */
	public static List<String> parseCSV(@Nullable String csv) {
		if (nullOrEmpty(csv)) {
			return Collections.emptyList();
		}

		return Arrays.stream(csv.split(","))
						.map(String::trim)
						.filter(string -> !string.isEmpty())
						.collect(Collectors.toList());
	}

	private static String padString(String string, int length, char padChar, boolean left) {
		if (requireNonNull(string).length() >= length) {
			return string;
		}

		StringBuilder stringBuilder = new StringBuilder(string);
		while (stringBuilder.length() < length) {
			if (left) {
				stringBuilder.insert(0, padChar);
			}
			else {
				stringBuilder.append(padChar);
			}
		}

		return stringBuilder.toString();
	}

	/**
	 * Returns true if the given string is null or empty.
	 * @param string the string to check
	 * @return true if the given string is null or empty, false otherwise
	 */
	public static boolean nullOrEmpty(@Nullable String string) {
		return string == null || string.isEmpty();
	}

	/**
	 * Returns true if any of the given strings is null or empty.
	 * @param strings the strings to check
	 * @return true if one of the given strings is null or empty or if no arguments are provided, false otherwise
	 */
	public static boolean nullOrEmpty(@Nullable String... strings) {
		if (strings == null || strings.length == 0) {
			return true;
		}
		for (int i = 0; i < strings.length; i++) {
			if (nullOrEmpty(strings[i])) {
				return true;
			}
		}

		return false;
	}

	private static final class SpaceAwareComparator<T> implements Comparator<T>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private static final char SPACE = ' ';
		private static final char UNDERSCORE = '_';

		private final Locale locale;

		private transient @Nullable Collator collator;

		private SpaceAwareComparator(Locale locale) {
			this.locale = locale;
		}

		@Override
		public int compare(T o1, T o2) {
			String s1 = o1 == null ? "" : o1.toString();
			String s2 = o2 == null ? "" : o2.toString();

			// Optimize string replacement for frequently called comparisons
			return collator().compare(replaceSpacesWithUnderscore(s1), replaceSpacesWithUnderscore(s2));
		}

		/**
		 * Efficiently replaces spaces with underscores using StringBuilder
		 * to avoid creating multiple intermediate String objects.
		 */
		private String replaceSpacesWithUnderscore(String input) {
			int spaceIndex = input.indexOf(SPACE);
			if (spaceIndex == -1) {
				// No spaces found, return original string
				return input;
			}

			StringBuilder sb = new StringBuilder(input.length());
			int start = 0;
			do {
				sb.append(input, start, spaceIndex).append(UNDERSCORE);
				start = spaceIndex + 1;
				spaceIndex = input.indexOf(SPACE, start);
			} while (spaceIndex != -1);

			// Append remaining characters after last space
			sb.append(input, start, input.length());

			return sb.toString();
		}

		private Collator collator() {
			if (collator == null) {
				collator = Collator.getInstance(this.locale);
			}

			return collator;
		}
	}
}
