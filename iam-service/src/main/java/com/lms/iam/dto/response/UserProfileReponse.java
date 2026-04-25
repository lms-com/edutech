package com.lms.iam.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileReponse {
    String userId;
    String email;
    String fullName;
    @JsonFormat(pattern = "dd-MM-yyyy")
    LocalDate dob;
    Set<String> roles;
    Set<String> permissions;
    Object profile;
}
