package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceUnitTest {

    @Mock UserPointTable userPointTable;
    @Mock PointHistoryTable pointHistoryTable;

    PointService pointService;

    @BeforeEach
    void setUp() {
        pointService = new PointService(pointHistoryTable, userPointTable);
    }

    @Test
    @DisplayName("포인트를 충전할 수 있다.")
    void chargeTest() {
        // given
        long id = 1L;
        long amount = 300L;

        long nowMillis = 123456789L;

        given(userPointTable.selectById(id))
                .willReturn(new UserPoint(id, 0L, nowMillis));

        // totalPoint = 0 + 300
        long totalPoint = 300L;

        UserPoint saved = new UserPoint(id, totalPoint, nowMillis);
        given(userPointTable.insertOrUpdate(id, totalPoint)).willReturn(saved);


        ArgumentCaptor<Long> tsCaptor = ArgumentCaptor.forClass(Long.class);
        given(pointHistoryTable.insert(eq(id), eq(amount), eq(TransactionType.CHARGE), anyLong()))
                .willReturn(new PointHistory(1L, id, amount, TransactionType.CHARGE, nowMillis));

        // when
        UserPoint userPoint = pointService.charge(id, amount);

        // then
        assertThat(userPoint.point()).isEqualTo(300L);

        // then: 호출 인자 검증
        then(userPointTable).should().selectById(id);
        then(userPointTable).should().insertOrUpdate(id, totalPoint);

        // history.insert의 timestamp를 캡처해서 insertOrUpdate의 updateMillis와 동일한지 확인
        then(pointHistoryTable).should()
                .insert(eq(id), eq(amount), eq(TransactionType.CHARGE), tsCaptor.capture());
        assertThat(tsCaptor.getValue()).isEqualTo(saved.updateMillis());

        // then: 호출 순서 검증 (선택)
        InOrder inOrder = inOrder(userPointTable, pointHistoryTable);
        inOrder.verify(userPointTable).selectById(id);
        inOrder.verify(userPointTable).insertOrUpdate(id, totalPoint);
        inOrder.verify(pointHistoryTable).insert(id, amount, TransactionType.CHARGE, saved.updateMillis());
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
    @DisplayName("포인트 충전 시 음수 포인트는 충전불가다.")
    void chargeAmountIsNegativeTest() {
        // given
        long id = 1L;
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
        long id = 1L;
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
        long id = 1L;
        long amount = 3000L;

        // when
        pointService.charge(id, amount);

        // then
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(id);
        assertThat(pointHistories).hasSize(1);
    }

    @Test
    @DisplayName("포인트 충전 후, 포인트가 충전된만큼 있어야 한다.")
    void chargePointAfterTotalPointCheckTest() {
        // given
        long id = 1L;
        long amount = 3000L;

        // when
        pointService.charge(id, amount);

        // then
        UserPoint userPoint = userPointTable.selectById(id);

        assertThat(userPoint.point()).isEqualTo(amount);
    }

    @Test
    @DisplayName("포인트를 사용한다.")
    void usePointTest() {
        // given
        long id = 1L;
        long amount = 300L;

        pointService.charge(id, amount);

        long useAmount = 100L;

        // when
        UserPoint userPoint = pointService.use(id, useAmount);

        // then
        assertThat(userPoint.point()).isEqualTo(200L);
    }

    @Test
    @DisplayName("사용하려는 포인트가 0원 이하이면 안 된다.")
    void usePointIsNegativeTest() {
        // given
        long id = 1L;
        long useAmount = 0L;

        // when // then
        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용할 포인트를 확인하세요. 0 이하의 포인트는 사용할 수 없습니다.");
    }

    @Test
    @DisplayName("사용하려는 포인트가 음수이면 안 된다.")
    void usePointIsNegativeTestTwo() {
        // given
        long id = 1L;
        long useAmount = -1L;

        // when // then
        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용할 포인트를 확인하세요. 0 이하의 포인트는 사용할 수 없습니다.");
    }

    @Test
    @DisplayName("사용하려는 포인트가 0이하이면 사용이 안 되었으므로, 현재포인트가 남아있어야 한다.")
    void usePointIsNegativeThenPointEqaulsCurrentPointTest() {
        // given
        long id = 1L;
        long amount = 300L;

        pointService.charge(id, amount);

        long useAmount = -1L;

        // when
        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용할 포인트를 확인하세요. 0 이하의 포인트는 사용할 수 없습니다.");

        // then
        assertThat(userPointTable.selectById(id).point()).isEqualTo(amount);
    }


    @Test
    @DisplayName("사용하려는 포인트가 잔여 포인트보다 많으면 안 된다.")
    void usePointIsNotBeMoreThanTheExistingPoints() {
        // given
        long id = 1L;
        long amount = 300L;

        pointService.charge(id, amount);

        long useAmount = 400L;

        // when // then
        assertThatThrownBy(() -> pointService.use(id, useAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잔액을 확인하세요. 사용하려는 포인트가 잔여 포인트보다 많습니다.");
    }

    @Test
    @DisplayName("포인트 사용 후, 사용 내역이 남아야 한다.")
    void usePointAfterHistoryCheckTest() {
        // given
        long id = 1L;
        long amount = 300L;

        pointService.charge(id, amount);


        long useAmount = 200L;

        // when
        pointService.use(id, useAmount);

        // then
        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(id);

        assertThat(pointHistories).hasSize(2)
                .extracting("amount", "type")
                .containsExactlyInAnyOrder(
                        Tuple.tuple(300L, TransactionType.CHARGE),
                        Tuple.tuple(200L, TransactionType.USE)
                );
    }

    @Test
    @DisplayName("포인트를 사용하면, 사용한 포인트만큼 차감되어야 한다.")
    void usePointAfterUserPointCheckTest() {
        // given
        long id = 1L;
        long amount = 300L;

        pointService.charge(id, amount);


        long useAmount = 200L;

        // when
        pointService.use(id, useAmount);

        // then
        UserPoint userPoint = userPointTable.selectById(id);
        Assertions.assertThat(userPoint.point()).isEqualTo(100L);

    }

    @Test
    @DisplayName("잔여 포인트를 조회할 수 있다.")
    void pointTest() {
        // given
        long id = 1L;
        long amount = 300L;

        pointService.charge(id, amount);

        // when
        UserPoint point = pointService.point(id);

        // then
        assertThat(point.point()).isEqualTo(300L);
    }

    @Test
    @DisplayName("포인트를 충전한 내역이 없으면 0원이다.")
    void noChargePointThenPointIsZeroTest() {
        // given
        long id = 1L;

        // when
        UserPoint point = pointService.point(id);

        // then
        assertThat(point.point()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트 충전내역을 확인할 수 있다.")
    void pointHistoryTest() {
        // given
        long  id = 1L;
        long amount = 300L;

        pointService.charge(id, amount);

        // when
        List<PointHistory> histories = pointService.history(id);

        // then
        assertThat(histories).hasSize(1);
    }



}
