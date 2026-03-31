package com.trendradar.trending.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "countries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Country {

    @Id
    @Column(length = 5)
    private String code;

    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;

    @Column(name = "name_en", nullable = false, length = 50)
    private String nameEn;

    @Column(length = 50)
    private String region;

    public static Country of(String code, String nameKo, String nameEn, String region) {
        Country country = new Country();
        country.code = code;
        country.nameKo = nameKo;
        country.nameEn = nameEn;
        country.region = region;
        return country;
    }
}
