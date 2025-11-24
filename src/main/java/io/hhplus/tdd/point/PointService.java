package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;


    public UserPoint charge(long id, long amount) {
        if(isNegative(amount)) {
            throw new IllegalStateException("충전 포인트를 확인해주세요. 0 이하의 포인트는 충전할 수 없습니다.");
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


    public UserPoint use(long id, long amount) {
        if(isNegative(amount)) {
            throw new IllegalStateException("사용할 포인트를 확인하세요. 0 이하의 포인트는 사용할 수 없습니다.");
        }

        long currentPoint = userPointTable.selectById(id).point();

        if(currentPoint < amount) {
            throw new IllegalStateException("잔액을 확인하세요. 사용하려는 포인트가 잔여 포인트보다 많습니다.");
        }

        long totalPoint = currentPoint - amount;

        UserPoint userPoint = userPointTable.insertOrUpdate(id, totalPoint);
        pointHistoryTable.insert(id, amount, TransactionType.USE, userPoint.updateMillis());

        return userPoint;
    }

    public UserPoint point(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> history(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
}
