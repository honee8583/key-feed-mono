package com.keyfeed.keyfeedmonolithic.domain.auth.util;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.LoginUser;

public class PrincipalUtil {

    public static Long getUserId(LoginUser loginUser) {
        if (loginUser == null) {
            return null;
        }

        return loginUser.getId();
    }

}
