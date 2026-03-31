package com.trendradar.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse 공통 응답 테스트")
class ApiResponseTest {

    @Test
    @DisplayName("성공 응답 생성 시 success=true, data 포함")
    void of_whenDataProvided_returnsSuccessResponse() {
        // Given
        String data = "test data";

        // When
        ApiResponse<String> response = ApiResponse.of(data);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test data");
        assertThat(response.getMessage()).isNull();
    }

    @Test
    @DisplayName("실패 응답 생성 시 success=false, message 포함")
    void fail_whenMessageProvided_returnsFailResponse() {
        // Given
        String message = "에러 발생";

        // When
        ApiResponse<Void> response = ApiResponse.fail(message);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("에러 발생");
    }
}
