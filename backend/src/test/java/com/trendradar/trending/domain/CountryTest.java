package com.trendradar.trending.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Country Entity 테스트")
class CountryTest {

    @Test
    @DisplayName("Country 생성 시 필드 정상 설정")
    void create_whenValidParams_setsAllFields() {
        // Given & When
        Country country = Country.of("KR", "한국", "South Korea", "Asia");

        // Then
        assertThat(country.getCode()).isEqualTo("KR");
        assertThat(country.getNameKo()).isEqualTo("한국");
        assertThat(country.getNameEn()).isEqualTo("South Korea");
        assertThat(country.getRegion()).isEqualTo("Asia");
    }
}
