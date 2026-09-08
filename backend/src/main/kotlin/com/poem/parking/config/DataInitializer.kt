package com.poem.parking.config

import com.poem.parking.domain.Apartment
import com.poem.parking.repository.ApartmentRepository
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** 개발용 시드 데이터 */
@Configuration
@Profile("!prod")
class DataInitializer {
    @Bean
    fun seed(apartmentRepository: ApartmentRepository) = ApplicationRunner {
        if (apartmentRepository.findByCode("APT-0001") == null) {
            apartmentRepository.save(Apartment(code = "APT-0001", name = "행복마을 아파트", address = "경기도 고양시"))
        }
        if (apartmentRepository.findByCode("APT-0002") == null) {
            apartmentRepository.save(Apartment(code = "APT-0002", name = "푸른숲 아파트", freeMinutes = 60, unitFee = 300))
        }
    }
}
