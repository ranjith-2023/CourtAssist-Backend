package com.CourtAssist.dto.payload;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserRegistrationRequest {

    private String username;
    private String password;
    private String email;
    private String mobileNo;
    private String role;
    private String advocateName;

}
