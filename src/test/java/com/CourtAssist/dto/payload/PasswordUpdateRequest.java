package com.CourtAssist.dto.payload;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PasswordUpdateRequest {
    private String currentPassword;
    private String newPassword;

}