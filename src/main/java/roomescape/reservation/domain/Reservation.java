package roomescape.reservation.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import roomescape.common.exception.ConflictException;
import roomescape.common.exception.DomainValidationException;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

public class Reservation {
    private final Long id;
    private final String name;
    private final LocalDate date;
    private final ReservationTime time;
    private final Theme theme;
    private final ReservationStatus status;

    private Reservation(Long id, String name, LocalDate date, ReservationTime time, Theme theme, ReservationStatus status) {
        validate(name, date, time, theme);
        this.id = id;
        this.name = name;
        this.date = date;
        this.time = time;
        this.theme = theme;
        this.status = status;
    }

    public static Reservation create(String name, LocalDate date, ReservationTime reservationTime, Theme theme) {
        validatePast(date, reservationTime);
        return new Reservation(null, name, date, reservationTime, theme, ReservationStatus.RESERVED);
    }

    public static Reservation load(Long id, String name, LocalDate date, ReservationTime reservationTime, Theme theme, ReservationStatus status) {
        return new Reservation(id, name, date, reservationTime, theme, status);
    }

    private static void validate(String name, LocalDate date, ReservationTime reservationTime, Theme theme) {
        validateName(name);
        validateDate(date);
        validateReservationTime(reservationTime);
        validateTheme(theme);
    }

    private static void validatePast(LocalDate date, ReservationTime reservationTime) {
        if (LocalDateTime.of(date, reservationTime.startAt()).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("과거 날짜/시간으로는 예약할 수 없습니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("예약자 이름은 필수입니다.");
        }
    }

    private static void validateDate(LocalDate date) {
        if (date == null) {
            throw new DomainValidationException("예약 날짜는 필수입니다.");
        }
    }

    private static void validateReservationTime(ReservationTime reservationTime) {
        if (reservationTime == null) {
            throw new DomainValidationException("예약 시간은 필수입니다.");
        }
    }

    private static void validateTheme(Theme theme) {
        if (theme == null) {
            throw new DomainValidationException("테마는 필수입니다.");
        }
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public LocalDate date() {
        return date;
    }

    public ReservationTime time() {
        return time;
    }

    public Theme theme() {
        return theme;
    }

    public ReservationStatus status() {
        return status;
    }

    public Reservation cancel() {
        return new Reservation(id, name, date, time, theme, ReservationStatus.CANCELED);
    }

    public Reservation rescheduled(LocalDate date, ReservationTime time) {
        validateChangeable();
        validatePast(date, time);
        return new Reservation(id, name, date, time, theme, status);
    }

    private void validateChangeable() {
        if (status == ReservationStatus.CANCELED) {
            throw new ConflictException("이미 취소된 예약은 수정할 수 없습니다.");
        }
    }
}
