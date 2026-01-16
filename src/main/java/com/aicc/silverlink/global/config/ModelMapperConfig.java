package com.aicc.silverlink.global.config;

import com.aicc.silverlink.domain.welfare.dto.WelfareApiDto;
import com.aicc.silverlink.domain.welfare.dto.WelfareDetailResponse;
import com.aicc.silverlink.domain.welfare.dto.WelfareListResponse;
import com.aicc.silverlink.domain.welfare.entity.Source;
import com.aicc.silverlink.domain.welfare.entity.Welfare;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // 1. 공통 기본 설정
        configureDefault(modelMapper);

        // 2. 도메인별 매핑 등록 (팀원들이 각자 자신의 메서드를 호출하게 함)
        registerWelfareMappings(modelMapper);
        // registerUserMappings(modelMapper); // 나중에 팀원이 추가할 구역
        // registerCounselingMappings(modelMapper);

        return modelMapper;
    }

    /**
     * ModelMapper의 전역 설정 (전략 및 컨버터)
     */
    private void configureDefault(ModelMapper modelMapper) {
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);
    }

    /**
     * 복지(Welfare) 도메인 전용 매핑 설정
     */
    private void registerWelfareMappings(ModelMapper modelMapper) {

        // [공통 컨버터] Enum -> 한글 설명
        Converter<Source, String> sourceToStringConverter = context ->
                context.getSource() == null ? null : context.getSource().getDescription();

        // [수집용] 중앙부처 DTO -> Welfare 엔티티
        TypeMap<WelfareApiDto.CentralItem, Welfare> centralTypeMap =
                modelMapper.createTypeMap(WelfareApiDto.CentralItem.class, Welfare.class);

        centralTypeMap.addMappings(mapper -> {
            mapper.map(WelfareApiDto.CentralItem::getWlfareInfoOutlCn, Welfare::setServDgst);
            mapper.map(WelfareApiDto.CentralItem::getTgtrDtlCn, Welfare::setTargetDtlCn);
        });

        // [수집용] 지자체 DTO -> Welfare 엔티티
        TypeMap<WelfareApiDto.LocalItem, Welfare> localTypeMap =
                modelMapper.createTypeMap(WelfareApiDto.LocalItem.class, Welfare.class);

        localTypeMap.addMappings(mapper -> {
            mapper.map(WelfareApiDto.LocalItem::getSprtTrgtCn, Welfare::setTargetDtlCn);
            mapper.map(WelfareApiDto.LocalItem::getInqNum, Welfare::setRprsCtadr);
        });

        // [조회용] Welfare 엔티티 -> Response DTO (Enum 변환 적용)
        modelMapper.createTypeMap(Welfare.class, WelfareDetailResponse.class)
                .addMappings(mapper -> mapper.using(sourceToStringConverter)
                        .map(Welfare::getSource, WelfareDetailResponse::setSource));

        modelMapper.createTypeMap(Welfare.class, WelfareListResponse.class)
                .addMappings(mapper -> mapper.using(sourceToStringConverter)
                        .map(Welfare::getSource, WelfareListResponse::setSource));
    }
}