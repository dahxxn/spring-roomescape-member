package roomescape.reservation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationStatus;
import roomescape.theme.domain.Theme;
import roomescape.theme.repository.JdbcThemeRepository;
import roomescape.time.domain.ReservationTime;
import roomescape.time.repository.JdbcReservationTimeRepository;

@JdbcTest
class ReservationRepositoryTest {
    private final String name = "한다";
    private final LocalDate date1 = LocalDate.of(2099, 1, 1);
    private final LocalDate date2 = LocalDate.of(2099, 9, 1);
    private ReservationTime reservationTime1;
    private ReservationTime reservationTime2;
    private Theme theme;

    private JdbcReservationRepository jdbcReservationRepository;
    private JdbcReservationTimeRepository jdbcReservationTimeRepository;
    private JdbcThemeRepository jdbcThemeRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        jdbcReservationRepository = new JdbcReservationRepository(jdbcTemplate);
        jdbcReservationTimeRepository = new JdbcReservationTimeRepository(jdbcTemplate);
        jdbcThemeRepository = new JdbcThemeRepository(jdbcTemplate);

        reservationTime1= jdbcReservationTimeRepository.save(ReservationTime.create(LocalTime.of(12, 00)));
        reservationTime2= jdbcReservationTimeRepository.save(ReservationTime.create(LocalTime.of(20, 00)));

        theme = jdbcThemeRepository.save(Theme.create("테마", "설명", "썸네일"));
    }

    @Test
    @DisplayName("나의 예약들을 조회하면 날짜/시간 오름차순으로 정렬해 모두 조회한다.")
    void findAllByName() {
        // given
        List<Reservation> reservations = saveAll(
                List.of(Reservation.create(name, date1, reservationTime1, theme),
                        Reservation.create(name, date1, reservationTime2, theme),
                        Reservation.create(name, date2, reservationTime1, theme),
                        Reservation.create(name, date2, reservationTime2, theme))
        );
        reservations.sort(Comparator.comparing(Reservation::date).thenComparing(r -> r.time().startAt()));

        // when
        List<Reservation> actual = jdbcReservationRepository.findAllByNameOrderByDateAndTime(name);

        // then
        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(reservations);
    }

    @Test
    @DisplayName("자기 자신을 제외하고 날짜/시간/테마 중복 여부를 확인한다.")
    void existsByDateAndTimeAndThemeId_excludeSelf() {
        // given
        Reservation saved = jdbcReservationRepository.save(
                Reservation.create(name, date1, reservationTime1, theme));

        // when & then
        assertThat(jdbcReservationRepository.existsByDateAndTimeIdAndThemeId(
                date1, reservationTime1.id(), theme.id(), saved.id(), ReservationStatus.RESERVED))
                .isFalse();
    }

    @Test
    @DisplayName("자기 자신 외 다른 예약이 있으면 true를 반환한다.")
    void existsByDateAndTimeAndThemeId_excludeSelf_otherExists() {
        // given
        Reservation saved = jdbcReservationRepository.save(
                Reservation.create(name, date1, reservationTime1, theme));
        jdbcReservationRepository.save(
                Reservation.create("브라운", date1, reservationTime2, theme));

        // when & then
        assertThat(jdbcReservationRepository.existsByDateAndTimeIdAndThemeId(
                date1, reservationTime2.id(), theme.id(), saved.id(), ReservationStatus.RESERVED))
                .isTrue();
    }

    private List<Reservation> saveAll(List<Reservation> reservations) {
        List<Reservation> savedReservations = new ArrayList<>();
        for (Reservation reservation : reservations) {
            Reservation saved =  jdbcReservationRepository.save(reservation);
            savedReservations.add(saved);
        }
        return savedReservations;
    }


    @Test
    @DisplayName("같은 날짜/시간/테마에 예약중인 예약은 하나만 저장할 수 있다.")
    void save_duplicate_reserved_reservation() {
        // given
        jdbcReservationRepository.save(Reservation.create(name, date1, reservationTime1, theme));

        // when & then
        assertThatThrownBy(() -> jdbcReservationRepository.save(
                Reservation.create("브라운", date1, reservationTime1, theme)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("취소된 예약이 있는 날짜/시간/테마에는 다시 예약을 저장할 수 있다.")
    void save_reserved_reservation_after_canceled_reservation() {
        // given
        Reservation saved = jdbcReservationRepository.save(Reservation.create(name, date1, reservationTime1, theme));
        jdbcReservationRepository.updateStatus(saved.cancel());

        // when & then
        assertThatCode(() -> jdbcReservationRepository.save(
                Reservation.create("브라운", date1, reservationTime1, theme)))
                .doesNotThrowAnyException();
    }

}
