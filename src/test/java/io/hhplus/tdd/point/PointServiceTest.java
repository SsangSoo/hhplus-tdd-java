package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest
class PointServiceTest {

    @Autowired
    PointService pointService;

    @Autowired
    PointHistoryTable pointHistoryTable;

    @Autowired
    UserPointTable userPointTable;


    @Test
    @DisplayName("포인트를 충전할 수 있다.")
    void chargeTest() {
        // given
        long id = 1L;
        long amount = 300L;

        // when
        UserPoint userPoint = pointService.charge(id, amount);

        // then
        assertThat(userPoint.point()).isEqualTo(300L);
    }

    @Test
    @DisplayName("포인트 충전은 무조건 0 이상이어야 한다.")
    void chargeAmountIsPositiveTest() {
        // given
        long id = 1L;
        long amount = 1L;

        // when
        UserPoint userPoint = pointService.charge(id, amount);

        // then
        assertThat(userPoint.point()).isEqualTo(1L);
    }

    @Test
    @DisplayName("포인트 충전 시 0 이하의 포인트는 충전불가다.")
    void chargeAmountIsNegativeTest() {
        // given
        long id = 1L;
        long amount = -300L;

        // when // then
        Assertions.assertThatThrownBy(() -> pointService.charge(id, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("충전 포인트를 확인해주세요. 0 이하의 값은 허용할 수 없습니다.");
    }

    @Test
    @DisplayName("포인트 충전 시 0 포인트는 충전불가다.")
    void chargeAmountIsZeroTest() {
        // given
        long id = 1L;
        long amount = 0L;

        // when // then
        Assertions.assertThatThrownBy(() -> pointService.charge(id, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("충전 포인트를 확인해주세요. 0 이하의 값은 허용할 수 없습니다.");
    }






}