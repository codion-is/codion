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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.domain;

import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.domain.api.Chinook.Customer;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.demos.chinook.domain.api.Chinook.Playlist;
import is.codion.demos.chinook.domain.api.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.demos.chinook.domain.api.Chinook.Track.RaisePriceParameters;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.json.domain.DefaultEntityObjectMapperFactory;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.type.TypeReference;

// tag::procedureFunction[]
public final class ChinookObjectMapperFactory extends DefaultEntityObjectMapperFactory {

	public ChinookObjectMapperFactory() {
		super(Chinook.DOMAIN);
	}

	@Override
	public EntityObjectMapper entityObjectMapper(Entities entities) {
		EntityObjectMapper objectMapper = super.entityObjectMapper(entities);
		objectMapper.define(Invoice.UPDATE_TOTALS)
						.argumentType(new TypeReference<>() {});
		objectMapper.define(Track.RAISE_PRICE)
						.returnType(new TypeReference<>() {})
						.argumentType(RaisePriceParameters.class);
		objectMapper.define(Playlist.RANDOM_PLAYLIST)
						.returnType(Entity.class)
						.argumentType(RandomPlaylistParameters.class);
		objectMapper.define(Customer.REPORT)
						.parameterType(new TypeReference<>() {});

		return objectMapper;
	}
}
// end::procedureFunction[]