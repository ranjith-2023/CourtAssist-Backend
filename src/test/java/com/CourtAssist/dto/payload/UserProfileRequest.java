package com.CourtAssist.dto.payload;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserProfileRequest {

    private String username;
    private String email;
    private String mobileNo;
    private String advocateName;
    private String role;

}