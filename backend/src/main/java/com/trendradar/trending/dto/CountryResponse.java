package com.trendradar.trending.dto;

import com.trendradar.trending.domain.Country;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CountryResponse {

    private final String code;
    private final String nameKo;
    private final String nameEn;
    private final String region;

    public static CountryResponse from(Country country) {
        return CountryResponse.builder()
                .code(country.getCode())
                .nameKo(country.getNameKo())
                .nameEn(country.getNameEn())
                .region(country.getRegion())
                .build();
    }
}
