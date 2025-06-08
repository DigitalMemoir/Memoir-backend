package com.univ.memoir.api.dto.req;

import com.univ.memoir.core.domain.InterestType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.Set;

@Getter
public class UserInterestRequest {

    @NotEmpty
    @Size(max = 5, message = "관심사는 최대 5개까지 선택할 수 있습니다.")
    private Set<InterestType> interests;
}
