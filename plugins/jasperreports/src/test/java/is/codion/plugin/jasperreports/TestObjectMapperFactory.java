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
package is.codion.plugin.jasperreports;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.json.domain.EntityObjectMapper;
import is.codion.framework.json.domain.EntityObjectMapperFactory;
import is.codion.plugin.jasperreports.TestDomain.Employee;

import com.fasterxml.jackson.core.type.TypeReference;

public final class TestObjectMapperFactory implements EntityObjectMapperFactory {

	@Override
	public boolean compatibleWith(DomainType domainType) {
		return TestDomain.DOMAIN.equals(domainType);
	}

	@Override
	public EntityObjectMapper entityObjectMapper(Entities entities) {
		EntityObjectMapper entityObjectMapper = EntityObjectMapperFactory.super.entityObjectMapper(entities);
		//the reports return bytes, which JSON base64 encodes, so a JSON connection carries them
		entityObjectMapper.parameter(Employee.PDF_REPORT).set(new TypeReference<>() {});
		entityObjectMapper.returnType(Employee.PDF_REPORT).set(byte[].class);
		entityObjectMapper.parameter(Employee.SERIALIZED_REPORT).set(new TypeReference<>() {});
		entityObjectMapper.returnType(Employee.SERIALIZED_REPORT).set(byte[].class);

		return entityObjectMapper;
	}
}
