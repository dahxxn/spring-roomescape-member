package roomescape.reservation.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;

public interface ReservationRepository {
    List<Reservation> findAll();

    List<Reservation> findAllByNameOrderByDateAndTime(String name);

    Optional<Reservation> findById(Long id);

    Reservation save(Reservation reservation);

    boolean existsByDateAndTimeIdAndThemeId(LocalDate date, long timeId, long themeId, ReservationStatus status);

    boolean existsByDateAndTimeIdAndThemeId(LocalDate date, long timeId, long themeId, long excludeId, ReservationStatus status);

    boolean existsByNameAndDateAndTimeId(String name, LocalDate date, long timeId);

    boolean existsByTimeId(long timeId, ReservationStatus status);

    Reservation updateStatus(Reservation reservation);

    Reservation updateDateAndTime(Reservation reservation);
}
