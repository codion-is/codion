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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.exception;

import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.util.ResourceBundle.getBundle;

/**
 * The messages of the exceptions providing their own, each one having a resource bundle of its own,
 * looked up when the message is read, in the language of the reader, not the one throwing.
 */
final class Messages {

	private Messages() {}

	static String message(Class<?> exceptionClass, String key, Locale locale) {
		return messageBundle(exceptionClass, bundle(exceptionClass, locale)).getString(key);
	}

	private static ResourceBundle bundle(Class<?> exceptionClass, Locale locale) {
		ResourceBundle bundle = getBundle(exceptionClass.getName(), locale);
		String language = bundle.getLocale().getLanguage();
		if (!language.isEmpty() && !language.equals(locale.getLanguage())) {
			// a locale without a bundle falls back on the one of the default locale, before the root one,
			// an english reader on an icelandic web server getting icelandic, the root bundle is the one to use
			return getBundle(exceptionClass.getName(), Locale.ROOT);
		}

		return bundle;
	}
}
