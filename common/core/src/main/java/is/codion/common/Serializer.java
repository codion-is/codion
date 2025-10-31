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
package is.codion.common;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for serialization.
 */
public final class Serializer {

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	private Serializer() {}

	/**
	 * Serializes the given Object, null object results in an empty byte array
	 * @param object the object
	 * @return a byte array representing the serialized object, an empty byte array in case of null
	 * @throws IOException in case of an exception
	 */
	public static byte[] serialize(@Nullable Object object) throws IOException {
		if (object != null) {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			new ObjectOutputStream(byteArrayOutputStream).writeObject(object);

			return byteArrayOutputStream.toByteArray();
		}

		return EMPTY_BYTE_ARRAY;
	}

	/**
	 * Deserializes the input stream into a T, an empty input stream results in a null return value
	 * @param inputStream the input stream containing the serialized object
	 * @param <T> the type of the object represented in the byte array
	 * @return the deserialized object, null in case of an empty {@code inputStream}
	 * @throws IOException in case of an exception
	 * @throws ClassNotFoundException in case the deserialized class is not found
	 * @throws ClassCastException if the deserialized object cannot be cast to type T
	 */
	public static <T> @Nullable T deserialize(InputStream inputStream) throws IOException, ClassNotFoundException {
		return (T) new ObjectInputStream(requireNonNull(inputStream)).readObject();
	}

	/**
	 * Deserializes the given byte array into a T, null or an empty byte array result in a null return value
	 * @param bytes a byte array representing the serialized object
	 * @param <T> the type of the object represented in the byte array
	 * @return the deserialized object, null in case of an empty {@code bytes} array
	 * @throws IOException in case of an exception
	 * @throws ClassNotFoundException in case the deserialized class is not found
	 * @throws ClassCastException if the deserialized object cannot be cast to type T
	 */
	public static <T> @Nullable T deserialize(byte @Nullable [] bytes) throws IOException, ClassNotFoundException {
		if (bytes != null && bytes.length > 0) {
			return deserialize(new ByteArrayInputStream(bytes));
		}

		return null;
	}
}
