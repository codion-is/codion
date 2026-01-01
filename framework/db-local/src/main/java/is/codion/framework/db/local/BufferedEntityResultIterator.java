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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.domain.entity.Entity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An {@link EntityResultIterator} wrapper that buffers entities in batches and populates
 * their foreign key references before returning them. This allows efficient foreign key
 * population when iterating over large result sets without loading all entities into memory.
 * <p>
 * Each batch of entities has its foreign keys populated via
 * {@link DefaultLocalEntityConnection#populateForeignKeys(List, Select, int)} before
 * the entities are returned through {@link #next()}.
 * @see LocalEntityConnection#ITERATOR_BUFFER_SIZE
 * @see LocalEntityConnection#iteratorBufferSize()
 */
final class BufferedEntityResultIterator implements EntityResultIterator {

	private final DefaultLocalEntityConnection connection;
	private final EntityResultIterator iterator;
	private final Select select;
	private final int bufferSize;
	private final List<Entity> buffer;

	private int bufferIndex = 0;

	BufferedEntityResultIterator(DefaultLocalEntityConnection connection, EntityResultIterator iterator, Select select, int bufferSize) {
		this.iterator = iterator;
		this.connection = connection;
		this.select = select;
		this.bufferSize = bufferSize;
		this.buffer = new ArrayList<>(bufferSize);
	}

	@Override
	public boolean hasNext() {
		return bufferIndex < buffer.size() || iterator.hasNext();
	}

	@Override
	public Entity next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		try {
			if (bufferIndex >= buffer.size()) {
				fillBuffer();
			}

			return buffer.get(bufferIndex++);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	@Override
	public void close() {
		buffer.clear();
		iterator.close();
	}

	private void fillBuffer() throws SQLException {
		buffer.clear();
		bufferIndex = 0;
		while (buffer.size() < bufferSize && iterator.hasNext()) {
			buffer.add(iterator.next());
		}
		if (!buffer.isEmpty()) {
			connection.populateForeignKeys(buffer, select, 0);
		}
	}
}
