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
        UserPoint userPoint = userPointTable.insertOrUpdate(id, amount);
        pointHistoryTable.insert(userPoint.id(), userPoint.point(), TransactionType.CHARGE, System.currentTimeMillis());
        return userPoint;
    }

    private boolean isNegative(long amount) {
        return amount < 0;
    }


}
