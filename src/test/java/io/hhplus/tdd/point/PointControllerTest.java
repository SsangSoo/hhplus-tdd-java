package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;


@WebMvcTest(PointController.class)
class PointControllerTest {

    @MockBean
    private PointService pointService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("포인트 충전 테스트")
    void chargeTest() throws Exception {
        // given
        long id = 1L;
        long amount = 3000L;
        long chargeTime = System.currentTimeMillis();

        BDDMockito.given(pointService.charge(id, amount))
                .willReturn(new UserPoint(id, amount, chargeTime));

        // when
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.patch("/point/{id}/charge", id));

        // then
//        MvcResult mvcResult = resultActions

//        System.out.println("mvcResult = " + mvcResult);
    }

}