package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;


    public UserPoint charge(long id, long amount) {
        if(isNegative(amount)) {
            throw new IllegalStateException("충전 포인트를 확인해주세요. 0 이하의 값은 허용할 수 없습니다.");
        }

        long currentPoint = userPointTable.selectById(id).point();
        long totalPoint = currentPoint + amount;

        UserPoint userPoint = userPointTable.insertOrUpdate(id, totalPoint);
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, userPoint.updateMillis());

        return userPoint;
    }

    private boolean isNegative(long amount) {
        return amount <= 0;
    }


}
