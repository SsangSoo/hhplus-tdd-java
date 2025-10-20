 package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PointServiceIntegrationTest {

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
    @DisplayName("포인트를 충전한 내역이 없는 상태에서 또 포인트를 조회하면 0원이다.")
    void noChargePointThenPointIsZeroTest2() {
        // given
        long id = userId;

        pointService.point(id);

        // when
        UserPoint point2 = pointService.point(id);

        // then
        assertThat(point2.point()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트를 충전한 내역이 없는 상태에서 또 포인트를 여러 번 조회해도 0원이다.")
    void noChargePointThenPointIsZeroTest3() {
        // given
        long id = userId;

        pointService.point(id);
        pointService.point(id);
        pointService.point(id);

        // when
        UserPoint point2 = pointService.point(id);

        // then
        assertThat(point2.point()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트를 충전하고, 사용하고, 충전하고 남은 잔액을 확인할 수 있다.")
    void useAndChargePointThenCheckPointTest() {
        // given
        long id = userId;
        long amount = 1000L;

        pointService.charge(id, amount);

        long usePoint = 500L;
        pointService.use(id, usePoint);

        long chargePoint = 1000L;
        pointService.charge(id, chargePoint);

        // when
        UserPoint point = pointService.point(id);

        // then
        assertThat(point.point()).isEqualTo(1500L);

    }

    @Test
    @DisplayName("포인트를 충전하고, 포인트를 사용하지 못 했을 때 포인트를 조회하면 그대로 잔액을 조회할 수 있어야 한다.")
    void chargePointAndFailedUsePointAfterCheckPointTest() {
        // given
        long id = userId;
        long amount = 1000L;

        pointService.charge(id, amount);

        long usePoint = 1500L;
        assertThatThrownBy(() -> pointService.use(id, usePoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잔액을 확인하세요. 사용하려는 포인트가 잔여 포인트보다 많습니다.");


        // when
        UserPoint point = pointService.point(id);

        // then
        assertThat(point.point()).isEqualTo(1000L);
    }



    @Test
    @DisplayName("포인트를 3번 충전하면 3개의 내역을 확인할 수 있다.")
    void pointHistoryTest2() {
        // given
        long  id = userId;
        long amount = 300L;

        pointService.charge(id, amount);
        pointService.charge(id, amount);
        pointService.charge(id, amount);

        // when
        List<PointHistory> histories = pointService.history(id);

        // then
        assertThat(histories).hasSize(3);

        long sumAmount = histories.stream()
                .mapToLong(p -> p.amount())
                .sum();
        UserPoint userPoint = pointService.point(id);
        assertThat(userPoint.point()).isEqualTo(sumAmount);

        Set<Long> idSet = histories.stream()
                .map(h -> h.userId())
                .collect(Collectors.toSet());
        assertThat(idSet).hasSize(1);
        assertThat(idSet).containsExactlyInAnyOrder(id);

    }

    @Test
    @DisplayName("포인트 충전과 사용 내역을 확인할 수 있다.")
    void useAndChargePointHistoryCheckTest() {
        // given
        long  id = userId;
        long amount = 10000L;

        pointService.charge(id, amount);

        long useAmount = 2000L;
        pointService.use(id, useAmount);

        useAmount = 500L;
        pointService.use(id, useAmount);

        // when
        List<PointHistory> histories = pointService.history(id);

        // then
        assertThat(histories).hasSize(3);

        long sumChargeAmount = histories.stream()
                .filter(p -> p.type().equals(TransactionType.CHARGE))
                .mapToLong(p -> p.amount())
                .sum();

        long sumUseAmount = histories.stream()
                .filter(p -> p.type().equals(TransactionType.USE))
                .mapToLong(p -> p.amount())
                .sum();

        UserPoint userPoint = pointService.point(id);
        assertThat(userPoint.point()).isEqualTo(sumChargeAmount - sumUseAmount);

        Set<Long> idSet = histories.stream()
                .map(h -> h.userId())
                .collect(Collectors.toSet());
        assertThat(idSet).hasSize(1);
        assertThat(idSet).containsExactlyInAnyOrder(id);
    }




}