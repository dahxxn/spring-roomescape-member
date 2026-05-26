package roomescape.reservation.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

@Repository
public class JdbcReservationRepository implements ReservationRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;
    private final RowMapper<Reservation> reservationRowMapper = (resultSet, rowNumber) -> Reservation.load(
            resultSet.getLong("reservation_id"),
            resultSet.getString("name"),
            resultSet.getDate("date").toLocalDate(),
            ReservationTime.load(
                    resultSet.getLong("time_id"),
                    resultSet.getTime("start_at").toLocalTime()
            ),
            Theme.load(
                    resultSet.getLong("theme_id"),
                    resultSet.getString("theme_name"),
                    resultSet.getString("description"),
                    resultSet.getString("thumbnail_url"),
                    resultSet.getBoolean("is_active")
            ),
            ReservationStatus.valueOf(resultSet.getString("status"))
    );

    public JdbcReservationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("reservation")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public List<Reservation> findAll() {
        String sql = """
                SELECT
                    r.id AS reservation_id,
                    r.name,
                    r.date,
                    r.status,
                    r.time_id,
                    rt.start_at,
                    t.id AS theme_id,
                    t.name AS theme_name,
                    t.description,
                    t.thumbnail_url,
                    t.is_active
                FROM reservation r
                INNER JOIN theme t ON r.theme_id = t.id
                INNER JOIN reservation_time rt ON r.time_id = rt.id
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource(), reservationRowMapper);
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        String sql = """
                SELECT
                    r.id AS reservation_id,
                    r.name,
                    r.date,
                    r.status,
                    r.time_id,
                    rt.start_at,
                    t.id AS theme_id,
                    t.name AS theme_name,
                    t.description,
                    t.thumbnail_url,
                    t.is_active
                FROM reservation r
                INNER JOIN theme t ON r.theme_id = t.id
                INNER JOIN reservation_time rt ON r.time_id = rt.id
                WHERE r.id = :id
                """;
        SqlParameterSource params = new MapSqlParameterSource("id", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, reservationRowMapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Reservation> findAllByNameOrderByDateAndTime(String name) {
        String sql = """
                SELECT
                    r.id AS reservation_id,
                    r.name,
                    r.date,
                    r.status,
                    r.time_id,
                    rt.start_at,
                    t.id AS theme_id,
                    t.name AS theme_name,
                    t.description,
                    t.thumbnail_url,
                    t.is_active
                FROM reservation r
                INNER JOIN theme t ON r.theme_id = t.id
                INNER JOIN reservation_time rt ON r.time_id = rt.id
                WHERE r.name = :name
                ORDER BY r.date ASC, rt.start_at ASC
                """;
        SqlParameterSource params = new MapSqlParameterSource("name", name);
        return jdbcTemplate.query(sql, params, reservationRowMapper);
    }

    @Override
    public Reservation save(Reservation reservation) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", reservation.name())
                .addValue("date", reservation.date())
                .addValue("time_id", reservation.time().id())
                .addValue("theme_id", reservation.theme().id())
                .addValue("status", reservation.status().name());
        Long savedId = simpleJdbcInsert.executeAndReturnKey(params).longValue();
        return Reservation.load(savedId, reservation.name(), reservation.date(), reservation.time(), reservation.theme(), reservation.status());
    }

    @Override
    public boolean existsByDateAndTimeIdAndThemeId(
            LocalDate date,
            long timeId,
            long themeId,
            ReservationStatus status
    ) {
        String sql = """
                SELECT COUNT(*) FROM reservation
                WHERE date = :date
                    AND time_id = :time_id
                    AND theme_id = :theme_id
                    AND status = :status
                """;
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("date", date)
                .addValue("time_id", timeId)
                .addValue("theme_id", themeId)
                .addValue("status", status.name());
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByDateAndTimeIdAndThemeId(
            LocalDate date,
            long timeId,
            long themeId,
            long excludeId,
            ReservationStatus status
    ) {
        String sql = """
                SELECT COUNT(*) FROM reservation
                WHERE date = :date
                    AND time_id = :time_id
                    AND theme_id = :theme_id
                    AND id != :excludeId
                    AND status = :status
                """;
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("date", date)
                .addValue("time_id", timeId)
                .addValue("theme_id", themeId)
                .addValue("excludeId", excludeId)
                .addValue("status", status.name());
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByNameAndDateAndTimeId(String name, LocalDate date, long timeId) {
        String sql = """
                SELECT COUNT(*) FROM reservation
                WHERE name = :name
                  AND date = :date
                  AND time_id = :time_id
                """;
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("date", date)
                .addValue("time_id", timeId);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByTimeId(long timeId, ReservationStatus status) {
        String sql = """
                SELECT COUNT(*) FROM reservation
                WHERE time_id = :timeId
                    AND status = :status
                """;
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("timeId", timeId)
                .addValue("status", status.name());
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Reservation updateStatus(Reservation reservation) {
        String sql = """
                UPDATE reservation
                SET status = :status
                WHERE id = :id
                """;
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", reservation.id())
                .addValue("status", reservation.status().name());
        jdbcTemplate.update(sql, params);
        return reservation;
    }

    @Override
    public Reservation updateDateAndTime(Reservation reservation) {
        String sql = """
                UPDATE reservation
                SET date = :date, time_id = :time_id
                WHERE id = :id
                """;
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", reservation.id())
                .addValue("date", reservation.date())
                .addValue("time_id", reservation.time().id());
        jdbcTemplate.update(sql, params);
        return reservation;
    }
}
