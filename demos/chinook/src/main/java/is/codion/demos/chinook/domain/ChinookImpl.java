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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.domain;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.result.ResultPacker;
import is.codion.common.utilities.format.LocaleDateTimePattern;
import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.domain.api.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.demos.chinook.domain.api.Chinook.Track.RaisePriceParameters;
import is.codion.demos.chinook.migration.MigrationManager;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityFormatter;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Converter;
import is.codion.framework.domain.entity.attribute.ColumnTemplate;
import is.codion.framework.domain.entity.query.EntitySelectQuery;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static is.codion.demos.chinook.domain.api.Chinook.*;
import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;
import static is.codion.plugin.jasperreports.JasperReports.classPathReport;

public final class ChinookImpl extends DomainModel {

	// tag::columnTemplates[]
	private static final ColumnTemplate<String> REQUIRED_SEARCHABLE =
					column -> column
									.nullable(false)
									.searchable(true);
	private static final ColumnTemplate<LocalDateTime> INSERT_TIME =
					column -> column
									.readOnly(true)
									.captionResource(Chinook.class.getName(), "insert_time");
	private static final ColumnTemplate<String> INSERT_USER =
					column -> column
									.readOnly(true)
									.captionResource(Chinook.class.getName(), "insert_user");
	// end::columnTemplates[]

	// tag::proceduresFunctions[]
	public ChinookImpl() {
		super(DOMAIN);
		add(artist(), album(), employee(), customer(), genre(), preferences(), mediaType(),
						track(), invoice(), invoiceLine(), playlist(), playlistTrack(), artistRevenue());
		add(Customer.REPORT, classPathReport(ChinookImpl.class, "customer_report.jasper"));
		add(Track.RAISE_PRICE, new RaisePrice());
		add(Invoice.UPDATE_TOTALS, new UpdateTotals());
		add(Playlist.RANDOM_PLAYLIST, new CreateRandomPlaylist(entities()));
	}
	// end::proceduresFunctions[]

	@Override
	public void configure(Database database) throws DatabaseException {
		MigrationManager.migrate(database);
	}

	EntityDefinition artist() {
		return Artist.TYPE.as(
										Artist.ID.as()
														.primaryKey()
														.generator(identity()),
										Artist.NAME.as()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(120),
										Artist.NUMBER_OF_ALBUMS.as()
														.subquery("""
																		SELECT COUNT(*)
																		FROM chinook.album
																		WHERE album.artist_id = artist.id"""),
										Artist.NUMBER_OF_TRACKS.as()
														.subquery("""
																		SELECT count(*)
																		FROM chinook.track
																		JOIN chinook.album ON track.album_id = album.id
																		WHERE album.artist_id = artist.id"""))
						.orderBy(ascending(Artist.NAME))
						.formatter(Artist.NAME)
						.build();
	}

	EntityDefinition album() {
		return Album.TYPE.as(
										Album.ID.as()
														.primaryKey()
														.generator(identity()),
										Album.ARTIST_ID.as()
														.column()
														.nullable(false),
										Album.ARTIST_FK.as()
														.foreignKey()
														.include(Artist.NAME),
										// tag::columnTemplateUsage1[]
										Album.TITLE.as()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(160),
										// end::columnTemplateUsage1[]
										Album.COVER.as()
														.column()
														.format(new CoverFormat()),
										Album.NUMBER_OF_TRACKS.as()
														.subquery("""
																		SELECT COUNT(*) FROM chinook.track
																		WHERE track.album_id = album.id"""),
										Album.TAGS.as()
														.column()
														.converter(Array.class, new TagsConverter(), ResultSet::getArray),
										Album.RATING.as()
														.subquery("""
																		SELECT AVG(rating) FROM chinook.track
																		WHERE track.album_id = album.id"""),
										// tag::columnTemplateUsage2[]
										Album.INSERT_TIME.as()
														.column(INSERT_TIME),
										Album.INSERT_USER.as()
														.column(INSERT_USER))
						// end::columnTemplateUsage2[]
						.orderBy(ascending(Album.ARTIST_ID, Album.TITLE))
						.formatter(Album.TITLE)
						.build();
	}

	EntityDefinition employee() {
		return Employee.TYPE.as(
										Employee.ID.as()
														.primaryKey()
														.generator(identity()),
										Employee.LASTNAME.as()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(20),
										Employee.FIRSTNAME.as()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(20),
										Employee.TITLE.as()
														.column()
														.maximumLength(30),
										Employee.REPORTSTO.as()
														.column(),
										Employee.REPORTSTO_FK.as()
														.foreignKey()
														.include(Employee.FIRSTNAME, Employee.LASTNAME),
										Employee.BIRTHDATE.as()
														.column(),
										Employee.HIREDATE.as()
														.column()
														.dateTimePattern(LocaleDateTimePattern.builder()
																		.delimiterDot()
																		.yearFourDigits()
																		.build()),
										Employee.ADDRESS.as()
														.column()
														.maximumLength(70),
										Employee.CITY.as()
														.column()
														.maximumLength(40),
										Employee.STATE.as()
														.column()
														.maximumLength(40),
										Employee.COUNTRY.as()
														.column()
														.maximumLength(40),
										Employee.POSTALCODE.as()
														.column()
														.maximumLength(10),
										Employee.PHONE.as()
														.column()
														.maximumLength(24),
										Employee.FAX.as()
														.column()
														.maximumLength(24),
										Employee.EMAIL.as()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(60),
										Employee.INSERT_TIME.as()
														.column(INSERT_TIME),
										Employee.INSERT_USER.as()
														.column(INSERT_USER))
						.validator(new EmailValidator(Employee.EMAIL))
						.orderBy(name(Employee.FIRSTNAME, Employee.LASTNAME))
						.formatter(EntityFormatter.builder()
										.value(Employee.LASTNAME)
										.text(", ")
										.value(Employee.FIRSTNAME)
										.build())
						.build();
	}

	EntityDefinition customer() {
		return Customer.TYPE.as(
										Customer.ID.as()
														.primaryKey()
														.generator(identity()),
										Customer.LASTNAME.as()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(20),
										Customer.FIRSTNAME.as()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(40),
										Customer.COMPANY.as()
														.column()
														.maximumLength(80),
										Customer.ADDRESS.as()
														.column()
														.maximumLength(70),
										Customer.CITY.as()
														.column()
														.maximumLength(40),
										Customer.STATE.as()
														.column()
														.maximumLength(40),
										Customer.COUNTRY.as()
														.column()
														.maximumLength(40),
										Customer.POSTALCODE.as()
														.column()
														.maximumLength(10),
										Customer.PHONE.as()
														.column()
														.maximumLength(24),
										Customer.FAX.as()
														.column()
														.maximumLength(24),
										Customer.EMAIL.as()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(60),
										Customer.SUPPORTREP_ID.as()
														.column(),
										Customer.SUPPORTREP_FK.as()
														.foreignKey()
														.include(Employee.FIRSTNAME, Employee.LASTNAME),
										Customer.INSERT_TIME.as()
														.column(INSERT_TIME),
										Customer.INSERT_USER.as()
														.column(INSERT_USER))
						.validator(new EmailValidator(Customer.EMAIL))
						.orderBy(name(Customer.FIRSTNAME, Customer.LASTNAME))
						.formatter(new CustomerFormatter())
						.build();
	}

	EntityDefinition preferences() {
		return Preferences.TYPE.as(
										Preferences.CUSTOMER_ID.as()
														.primaryKey(),
										Preferences.CUSTOMER_FK.as()
														.foreignKey(),
										Preferences.PREFERRED_GENRE_ID.as()
														.column(),
										Preferences.PREFERRED_GENRE_FK.as()
														.foreignKey(),
										Preferences.NEWSLETTER_SUBSCRIBED.as()
														.column())
						.caption("Preferences")
						.build();
	}

	EntityDefinition genre() {
		return Genre.TYPE.as(
										Genre.ID.as()
														.primaryKey()
														.generator(identity()),
										Genre.NAME.as()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(120))
						.orderBy(ascending(Genre.NAME))
						.formatter(Genre.NAME)
						.smallDataset(true)
						.build();
	}

	EntityDefinition mediaType() {
		return MediaType.TYPE.as(
										MediaType.ID.as()
														.primaryKey()
														.generator(identity()),
										MediaType.NAME.as()
														.column()
														.nullable(false)
														.maximumLength(120))
						.formatter(MediaType.NAME)
						.smallDataset(true)
						.build();
	}

	EntityDefinition track() {
		return Track.TYPE.as(
										Track.ID.as()
														.primaryKey()
														.generator(identity())
														.expression("track.id"),
										Track.ALBUM_ID.as()
														.column()
														.nullable(false),
										// tag::referenceDepth2[]
										Track.ALBUM_FK.as()
														.foreignKey()
														.referenceDepth(2)
														.include(Album.ARTIST_FK, Album.TITLE),
										// end::referenceDepth2[]
										Track.ARTIST_NAME.as()
														.column()
														.expression("artist.name")
														.readOnly(true),
										Track.NAME.as()
														.column(REQUIRED_SEARCHABLE)
														.expression("track.name")
														.maximumLength(200),
										Track.GENRE_ID.as()
														.column(),
										Track.GENRE_FK.as()
														.foreignKey(),
										Track.COMPOSER.as()
														.column()
														.maximumLength(220),
										Track.MEDIATYPE_ID.as()
														.column()
														.nullable(false),
										Track.MEDIATYPE_FK.as()
														.foreignKey(),
										Track.MILLISECONDS.as()
														.column()
														.nullable(false)
														.format(NumberFormat.getIntegerInstance()),
										Track.BYTES.as()
														.column()
														.format(NumberFormat.getIntegerInstance()),
										Track.RATING.as()
														.column()
														.nullable(false)
														.defaultValue(5)
														.range(1, 10),
										Track.UNITPRICE.as()
														.column()
														.nullable(false)
														.minimum(0)
														.fractionDigits(2),
										Track.PLAY_COUNT.as()
														.column()
														.nullable(false)
														.defaultValue(0),
										Track.RANDOM.as()
														.column()
														.readOnly(true)
														.selected(false),
										Track.INSERT_TIME.as()
														.column(INSERT_TIME)
														.expression("track.insert_time"),
										Track.INSERT_USER.as()
														.column(INSERT_USER)
														.expression("track.insert_user"))
						.selectQuery(EntitySelectQuery.builder()
										// Override the default FROM clause, joining
										// the ALBUM and ARTIST tables in order to
										// have the ARTIST.NAME column available
										.from("chinook.track " +
														"JOIN chinook.album ON track.album_id = album.id " +
														"JOIN chinook.artist ON album.artist_id = artist.id")
										.build())
						.orderBy(ascending(Track.NAME))
						// Implement a custom condition for specifying
						// tracks that are not in a given playlist
						.condition(Track.NOT_IN_PLAYLIST, (columns, values) -> """
										track.id NOT IN (
												SELECT track_id
												FROM chinook.playlisttrack
												WHERE playlist_id = ?
										)""")
						.formatter(Track.NAME)
						.build();
	}

	EntityDefinition invoice() {
		return Invoice.TYPE.as(
										Invoice.ID.as()
														.primaryKey()
														// tag::identity[]
														.generator(identity()),
										// end::identity[]
										Invoice.CUSTOMER_ID.as()
														.column()
														.nullable(false),
										Invoice.CUSTOMER_FK.as()
														.foreignKey()
														.include(Customer.FIRSTNAME, Customer.LASTNAME, Customer.EMAIL),
										Invoice.DATE.as()
														.column()
														.nullable(false)
														.defaultValue(Invoice.DATE_DEFAULT_VALUE)
														.dateTimePattern(LocaleDateTimePattern.builder()
																		.delimiterDot()
																		.yearFourDigits()
																		.build()),
										Invoice.BILLINGADDRESS.as()
														.column()
														.maximumLength(70),
										Invoice.BILLINGCITY.as()
														.column()
														.maximumLength(40),
										Invoice.BILLINGSTATE.as()
														.column()
														.maximumLength(40),
										Invoice.BILLINGCOUNTRY.as()
														.column()
														.maximumLength(40),
										Invoice.BILLINGPOSTALCODE.as()
														.column()
														.maximumLength(10),
										Invoice.TOTAL.as()
														.column()
														.fractionDigits(2)
														.nullable(false)
														.withDefault(true),
										Invoice.CALCULATED_TOTAL.as()
														.subquery("""
																		SELECT SUM(unitprice * quantity)
																		FROM chinook.invoiceline
																		WHERE invoice_id = invoice.id""")
														.fractionDigits(2),
										Invoice.INSERT_TIME.as()
														.column(INSERT_TIME),
										Invoice.INSERT_USER.as()
														.column(INSERT_USER))
						.orderBy(OrderBy.builder()
										.ascending(Invoice.CUSTOMER_ID)
										.descending(Invoice.DATE)
										.build())
						.formatter(Invoice.ID)
						.build();
	}

	EntityDefinition invoiceLine() {
		return InvoiceLine.TYPE.as(
										InvoiceLine.ID.as()
														.primaryKey()
														.generator(identity()),
										InvoiceLine.INVOICE_ID.as()
														.column()
														.nullable(false),
										// tag::referenceDepth0[]
										InvoiceLine.INVOICE_FK.as()
														.foreignKey()
														.referenceDepth(0)
														.hidden(true),
										// end::referenceDepth0[]
										InvoiceLine.TRACK_ID.as()
														.column()
														.nullable(false),
										InvoiceLine.TRACK_FK.as()
														.foreignKey()
														.include(Track.NAME, Track.UNITPRICE),
										InvoiceLine.UNITPRICE.as()
														.column()
														.nullable(false),
										InvoiceLine.QUANTITY.as()
														.column()
														.nullable(false)
														.defaultValue(1),
										InvoiceLine.TOTAL.as()
														.derived()
														.from(InvoiceLine.QUANTITY, InvoiceLine.UNITPRICE)
														.with(new InvoiceLineTotal()),
										InvoiceLine.INSERT_TIME.as()
														.column(INSERT_TIME)
														.hidden(true),
										InvoiceLine.INSERT_USER.as()
														.column(INSERT_USER)
														.hidden(true))
						.build();
	}

	EntityDefinition playlist() {
		return Playlist.TYPE.as(
										Playlist.ID.as()
														.primaryKey()
														.generator(identity()),
										Playlist.NAME.as()
														.column(REQUIRED_SEARCHABLE)
														.maximumLength(120))
						.orderBy(ascending(Playlist.NAME))
						.formatter(Playlist.NAME)
						.build();
	}

	EntityDefinition playlistTrack() {
		return PlaylistTrack.TYPE.as(
										PlaylistTrack.ID.as()
														.primaryKey()
														.generator(identity()),
										PlaylistTrack.PLAYLIST_ID.as()
														.column()
														.nullable(false),
										PlaylistTrack.PLAYLIST_FK.as()
														.foreignKey(),
										PlaylistTrack.ARTIST.as()
														.denormalized()
														.from(PlaylistTrack.ALBUM)
														.using(Album.ARTIST_FK),
										PlaylistTrack.TRACK_ID.as()
														.column()
														.nullable(false),
										PlaylistTrack.TRACK_FK.as()
														.foreignKey()
														.referenceDepth(3),
										PlaylistTrack.ALBUM.as()
														.denormalized()
														.from(PlaylistTrack.TRACK_FK)
														.using(Track.ALBUM_FK))
						.formatter(EntityFormatter.builder()
										.value(PlaylistTrack.PLAYLIST_FK)
										.text(" - ")
										.value(PlaylistTrack.TRACK_FK)
										.build())
						.build();
	}

	EntityDefinition artistRevenue() {
		return ArtistRevenue.TYPE.as(
										ArtistRevenue.ARTIST_ID.as()
														.primaryKey(),
										ArtistRevenue.NAME.as()
														.column()
														.expression("artist.name"),
										ArtistRevenue.TOTAL_REVENUE.as()
														.column()
														.expression("SUM(tr.revenue)")
														.fractionDigits(2))
						.selectQuery(EntitySelectQuery.builder()
										.with("track_revenue")
										.as("""
														SELECT line.track_id, SUM(line.unitprice * line.quantity) as revenue
														FROM chinook.invoiceline line
														GROUP BY line.track_id""")
										.from("""
														track_revenue tr
														JOIN chinook.track ON tr.track_id = track.id
														JOIN chinook.album ON track.album_id = album.id
														JOIN chinook.artist ON album.artist_id = artist.id""")
										.groupBy("artist.id, artist.name")
										.orderBy("total_revenue DESC")
										.build())
						.readOnly(true)
						.build();
	}

	private static OrderBy name(Column<String> firstName, Column<String> lastName) {
		String language = Locale.getDefault().getLanguage();
		return switch (language) {
			case "en" -> ascending(lastName, firstName);
			case "is" -> ascending(firstName, lastName);
			default -> throw new IllegalArgumentException("Unsupported language: " + language);
		};
	}

	// Converts between a SQL VARCHAR ARRAY and a List<String>.
	private static final class TagsConverter implements Converter<List<String>, Array> {

		private static final int ARRAY_VALUE_INDEX = 2;

		private final ResultPacker<String> packer = resultSet -> resultSet.getString(ARRAY_VALUE_INDEX);

		@Override
		public Array toColumn(List<String> value, Statement statement) throws SQLException {
			return value.isEmpty() ? null :
							statement.getConnection().createArrayOf("VARCHAR", value.toArray(new Object[0]));
		}

		@Override
		public List<String> fromColumn(Array value) throws SQLException {
			try (ResultSet resultSet = value.getResultSet()) {
				return packer.pack(resultSet);
			}
		}
	}

	// tag::updateTotals[]
	private static final class UpdateTotals implements DatabaseProcedure<EntityConnection, Collection<Long>> {

		@Override
		public void execute(EntityConnection connection,
												Collection<Long> invoiceIds) {
			Collection<Entity> invoices =
							connection.select(where(Invoice.ID.in(invoiceIds))
											.forUpdate()
											.build());

			connection.update(invoices.stream()
							.map(UpdateTotals::updateTotal)
							.filter(Entity::modified)
							.toList());
		}

		private static Entity updateTotal(Entity invoice) {
			invoice.set(Invoice.TOTAL, invoice.optional(Invoice.CALCULATED_TOTAL).orElse(BigDecimal.ZERO));

			return invoice;
		}
	}
	// end::updateTotals[]

	// tag::randomPlaylist[]
	private static final class CreateRandomPlaylist implements DatabaseFunction<EntityConnection, RandomPlaylistParameters, Entity> {

		private final Entities entities;

		private CreateRandomPlaylist(Entities entities) {
			this.entities = entities;
		}

		@Override
		public Entity execute(EntityConnection connection,
													RandomPlaylistParameters parameters) {
			List<Long> trackIds = randomTrackIds(connection, parameters.noOfTracks(), parameters.genres());

			return insertPlaylist(connection, parameters.playlistName(), trackIds);
		}

		private Entity insertPlaylist(EntityConnection connection, String playlistName,
																	List<Long> trackIds) {
			Entity playlist = connection.insertSelect(createPlaylist(playlistName));

			connection.insert(createPlaylistTracks(playlist.primaryKey().value(), trackIds));

			return playlist;
		}

		private Entity createPlaylist(String playlistName) {
			return entities.entity(Playlist.TYPE)
							.with(Playlist.NAME, playlistName)
							.build();
		}

		private List<Entity> createPlaylistTracks(Long playlistId, List<Long> trackIds) {
			return trackIds.stream()
							.map(trackId -> createPlaylistTrack(playlistId, trackId))
							.toList();
		}

		private Entity createPlaylistTrack(Long playlistId, Long trackId) {
			return entities.entity(PlaylistTrack.TYPE)
							.with(PlaylistTrack.PLAYLIST_ID, playlistId)
							.with(PlaylistTrack.TRACK_ID, trackId)
							.build();
		}

		private static List<Long> randomTrackIds(EntityConnection connection, int noOfTracks,
																						 Collection<Entity> genres) {
			return connection.select(Track.ID,
							where(Track.GENRE_FK.in(genres))
											.orderBy(ascending(Track.RANDOM))
											.limit(noOfTracks)
											.build());
		}
	}
	// end::randomPlaylist[]

	// tag::raisePrice[]
	private static final class RaisePrice implements DatabaseFunction<EntityConnection, RaisePriceParameters, Collection<Entity>> {

		@Override
		public Collection<Entity> execute(EntityConnection entityConnection,
																			RaisePriceParameters parameters) {
			Select select = where(Track.ID.in(parameters.trackIds()))
							.forUpdate()
							.build();

			return entityConnection.updateSelect(entityConnection.select(select).stream()
							.map(track -> raisePrice(track, parameters.priceIncrease()))
							.toList());
		}

		private static Entity raisePrice(Entity track, BigDecimal priceIncrease) {
			track.set(Track.UNITPRICE, track.get(Track.UNITPRICE).add(priceIncrease));

			return track;
		}
	}
	// end::raisePrice[]
}