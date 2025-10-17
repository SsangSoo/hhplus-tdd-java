package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PointServiceTest {

    static long userId = 1L;

    @Autowired
    PointService pointService;

    @Autowired
    PointHistoryTable pointHistoryTable;

    @Autowired
    UserPointTable userPointTable;

    @AfterEach
    void tearDown() {
        userId++;
    }


    @Test
    @DisplayName("포인트를 충전할 수 있다.")
    void chargeTest() {
        // given
        long id = userId;
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
        long id = userId;
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
        long id = userId;
        long amount = -300L;

        // when // then
        Assertions.assertThatThrownBy(() -> pointService.charge(id, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("충전 포인트를 확인해주세요. 0 이하의 포인트는 충전할 수 없습니다.");
    }

    @Test
    @DisplayName("포인트 충전 시 0 포인트는 충전불가다.")
    void chargeAmountIsZeroTest() {
        // given
        long id = userId;
        long amount = 0L;

        // when // then
        Assertions.assertThatThrownBy(() -> pointService.charge(id, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("충전 포인트를 확인해주세요. 0 이하의 포인트는 충전할 수 없습니다.");
    }

    @Test
    @DisplayName("포인트를 충전하면 충전 내역이 남아야 한다. 1번 충전했으므로 1번 남아야한다.")
    void chargePointAfterHistoryCheckTest() {
        //
        long id = userId;
        long amount = 3000L;

        // when
        pointService.charge(id, amount);

        // then
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(id);
        assertThat(pointHistories).hasSize(1);
    }

    @Test
    @DisplayName("포인트를 충전하면 충전 내역이 남아야 한다. 2번 충전했으므로 2개의 내역이 남아야한다.")
    void twiceChargePointAfterHistoryCheckTest() {
        // given
        long id = userId;
        long firstAmount = 3000L;

        pointService.charge(id, firstAmount);

        long secondAmount = 2000L;

        // when
        pointService.charge(id, secondAmount);


        // then
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(id);
        assertThat(pointHistories).hasSize(2);
    }

    @Test
    @DisplayName("포인트 충전 후, 포인트가 충전된만큼 있어야 한다.")
    void chargePointAfterTotalPointCheckTest() {
        // given
        long id = userId;
        long amount = 3000L;

        // when
        pointService.charge(id, amount);

        // then
        UserPoint userPoint = userPointTable.selectById(id);

        assertThat(userPoint.point()).isEqualTo(amount);
    }

    @Test
    @DisplayName("포인트를 3번 충전했다면, 합산된 포인트만큼 충전되어 있어야 한다.")
    void threeTimesChargePointAfterTotalPointCheckTest() {
        // given
        long id = userId;
        long firstAmount = 2000L;
        long secondAmount = 3000L;
        long thirdAmount = 4000L;

        // when
        pointService.charge(id, firstAmount);
        pointService.charge(id, secondAmount);
        pointService.charge(id, thirdAmount);

        // then
        UserPoint userPoint = userPointTable.selectById(id);

        assertThat(userPoint.point()).isEqualTo(firstAmount + secondAmount + thirdAmount);
    }

    @Test
    @DisplayName("첫 포인트 충전 실패시 포인트 총액은 당연히 0원이다.")
    void whenChargePointFailedTotalPointIsZeroTest() {
        // given
        long id = userId;
        long amount = 0L;

        // when
        assertThatThrownBy(() -> pointService.charge(id, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("충전 포인트를 확인해주세요. 0 이하의 포인트는 충전할 수 없습니다.");

        // then
        UserPoint userPoint = userPointTable.selectById(id);

        assertThat(userPoint.point()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트 충전 실패시 포인트 내역은 남지 않아야 된다.")
    void whenChargePointFailedTotalPointHistoryIsZeroTest() {
        // given
        long id = userId;
        long amount = 0L;

        // when
        assertThatThrownBy(() -> pointService.charge(id, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("충전 포인트를 확인해주세요. 0 이하의 포인트는 충전할 수 없습니다.");

        // then
        assertThat(pointHistoryTable.selectAllByUserId(userId)).hasSize(0);
    }

    @Test
    @DisplayName("처음 포인트를 충전하고, 그 다음 포인트 충전 실패시 포인트 충전 내역은 1개, 포인트는 첫 포인트 충전금액이다.")
    void firstChargePointAfterWhenSecondPointChargeFailedTotalPointIsFirstChargePointAndHistoryIsOneTest() {
        // given
        long id = userId;
        long firstAmount = 3000L;

        pointService.charge(id, firstAmount);

        long secondAmount = 0L;

        // when
        assertThatThrownBy(() -> pointService.charge(id, secondAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("충전 포인트를 확인해주세요. 0 이하의 포인트는 충전할 수 없습니다.");

        // then
        assertThat(pointHistoryTable.selectAllByUserId(userId)).hasSize(1);
        assertThat(userPointTable.selectById(id).point()).isEqualTo(firstAmount);
    }

    @Test
    @DisplayName("포인트를 사용한다.")
    void usePointTest() {
        // given
        long id = userId;
        long amount = 300L;

        pointService.charge(id, amount);

        long useAmount = 100L;

        long totalAmount = amount - useAmount;

        // when
        UserPoint userPoint = pointService.use(id, useAmount);

        // then
        assertThat(userPoint.point()).isEqualTo(totalAmount);
    }

    @Test
    @DisplayName("사용하려는 포인트가 0원 이하이면 안 된다.")
    void usePointIsNegativeTest() {
        // given
        long id = userId;
        long useAmount = 0L;

        // when // then
        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용할 포인트를 확인해보세요. 0 이하의 포인트는 사용할 수 없습니다.");
    }

    @Test
    @DisplayName("사용하려는 포인트가 음수이면 안 된다.")
    void usePointIsNegativeTestTwo() {
        // given
        long id = userId;
        long useAmount = -1L;

        // when // then
        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용할 포인트를 확인해보세요. 0 이하의 포인트는 사용할 수 없습니다.");
    }

    @Test
    @DisplayName("사용하려는 포인트가 0이하이면 사용이 안 되었으므로, 현재포인트가 남아있어야 한다.")
    void usePointIsNegativeThenPointEqaulsCurrentPointTest() {
        // given
        long id = userId;
        long amount = 300L;

        pointService.charge(id, amount);

        long useAmount = -1L;

        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용할 포인트를 확인해보세요. 0 이하의 포인트는 사용할 수 없습니다.");

        // then
        assertThat(userPointTable.selectById(id).point()).isEqualTo(amount);
    }

    @Test
    @DisplayName("사용하려는 포인트가 기존의 포인트보다 많으면 안 된다.")
    void usePointIsNotBeMoreThanTheExistingPoints() {
        // given

        // when

        // then

    }


}