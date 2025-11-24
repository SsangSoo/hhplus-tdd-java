package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class PointControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private PointController pointController;

    @Mock
    private PointService pointService;


    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(pointController)
                .build();
    }

    @Test
    @DisplayName("포인트 충전 테스트")
    void chargeTest() throws Exception {
        // given
        long id = 1L;
        long amount = 3000L;
        long chargeTime = System.currentTimeMillis();

        given(pointService.charge(id, amount))
                .willReturn(new UserPoint(id, amount, chargeTime));

        // when
        ResultActions resultActions = mockMvc.perform(patch("/point/{id}/charge", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(amount))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.updateMillis").value(chargeTime));
    }


    @Test
    @DisplayName("포인트 사용 테스트")
    void useTest() throws Exception {
        // given
        long id = 1L;
        long chargePoint = 3000L;
        long usePoint = 1000L;
        long totalPoint = 2000L;

        long chargeTime = System.currentTimeMillis();
        long useTime = chargeTime + 1000L;


        given(pointService.charge(id, chargePoint))
                .willReturn(new UserPoint(id, chargePoint, chargeTime));

        given(pointService.use(id, usePoint))
                .willReturn(new UserPoint(id, totalPoint, useTime));

        mockMvc.perform(patch("/point/{id}/charge", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargePoint)));

        // when
        ResultActions resultActions = mockMvc.perform(patch("/point/{id}/use", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(String.valueOf(usePoint)));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(totalPoint))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.updateMillis").value(useTime));
    }

    @Test
    @DisplayName("포인트 확인 테스트")
    void pointTest() throws Exception {

        // given
        long id = 1L;
        long chargePoint = 3000L;
        long chargeTime = System.currentTimeMillis();
        long useTime = chargeTime + 1000L;

        long usePoint = 1000L;
        long totalPoint = chargePoint - usePoint;

        given(pointService.charge(id, chargePoint))
                .willReturn(new UserPoint(id, chargePoint, chargeTime));

        given(pointService.use(id, usePoint))
                .willReturn(new UserPoint(id, totalPoint, useTime));

        given(pointService.point(id))
                .willReturn(new UserPoint(id, totalPoint, useTime));

        mockMvc.perform(patch("/point/{id}/charge", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargePoint)));

        mockMvc.perform(patch("/point/{id}/use", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(String.valueOf(usePoint)));

        // when
        ResultActions resultActions = mockMvc.perform(get("/point/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.point").value(totalPoint))
                .andExpect(jsonPath("$.updateMillis").value(useTime));
    }


    @Test
    @DisplayName("포인트 내역 확인 테스트")
    void historyTest() throws Exception {
        // given
        long id = 1L;
        long chargePoint = 3000L;
        long chargeTime = System.currentTimeMillis();
        long useTime = chargeTime + 1000L;

        long usePoint = 1000L;
        long totalPoint = chargePoint - usePoint;

        given(pointService.charge(id, chargePoint))
                .willReturn(new UserPoint(id, chargePoint, chargeTime));

        given(pointService.use(id, usePoint))
                .willReturn(new UserPoint(id, totalPoint, useTime));

        given(pointService.history(id))
                .willReturn(
                        List.of(
                                new PointHistory(1L, id, chargePoint, TransactionType.CHARGE, chargeTime),
                                new PointHistory(2L, id, usePoint, TransactionType.USE, useTime)
                        )
                );

        mockMvc.perform(patch("/point/{id}/charge", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(String.valueOf(chargePoint)));

        mockMvc.perform(patch("/point/{id}/use", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(String.valueOf(usePoint)));

        // when
        ResultActions resultActions = mockMvc.perform(get("/point/{id}/histories", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].userId").value(1L))
                .andExpect(jsonPath("$[0].amount").value(3000L))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].updateMillis").value(chargeTime))

                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].userId").value(1L))
                .andExpect(jsonPath("$[1].amount").value(1000L))
                .andExpect(jsonPath("$[1].type").value("USE"))
                .andExpect(jsonPath("$[1].updateMillis").value(useTime));
    }

}