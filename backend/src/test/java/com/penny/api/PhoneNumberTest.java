package com.penny.api;

import com.penny.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PhoneNumberTest extends IntegrationTest {

    @Test
    void newUserStartsWithoutPhoneAndCannotRecordExpenses() throws Exception {
        long user = registerUserWithoutPhone();

        getAs(user, "/api/me").andExpect(jsonPath("$.phoneNumber").isEmpty());

        postAs(user, "/api/expenses/text", "{\"text\":\"gastei 45 no mercado\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PHONE_NUMBER_REQUIRED"));

        String image = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        postAs(user, "/api/expenses/photo", "{\"imageBase64\":\"%s\",\"mimeType\":\"image/jpeg\"}".formatted(image))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("PHONE_NUMBER_REQUIRED"));

        assertThat(classifier.lastContext()).isNull();
    }

    @Test
    void phoneNumberIsNormalizedAndUnlocksExpenses() throws Exception {
        long user = registerUserWithoutPhone();
        String digits = uniquePhoneNumber(user).substring(1);
        String formatted = digits.substring(0, 2) + " (" + digits.substring(2, 4) + ") " + digits.substring(4, 9) + "-" + digits.substring(9);

        putAs(user, "/api/me/phone", "{\"phoneNumber\":\"%s\"}".formatted(formatted))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phoneNumber").value("+" + digits));

        postAs(user, "/api/expenses/text", "{\"text\":\"gastei 10\"}").andExpect(status().isCreated());
    }

    @Test
    void telegramStylePhoneWithoutPlusIsAccepted() throws Exception {
        long user = registerUserWithoutPhone();
        String phone = uniquePhoneNumber(user);

        putAs(user, "/api/me/phone", "{\"phoneNumber\":\"%s\"}".formatted(phone.substring(1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phoneNumber").value(phone));
    }

    @Test
    void invalidPhoneNumbersAreRejected() throws Exception {
        long user = registerUserWithoutPhone();

        putAs(user, "/api/me/phone", "{\"phoneNumber\":\"123\"}")
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("INVALID_PHONE_NUMBER"));
        putAs(user, "/api/me/phone", "{\"phoneNumber\":\"0055119\"}")
            .andExpect(status().isUnprocessableEntity());
        putAs(user, "/api/me/phone", "{\"phoneNumber\":\"\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.phoneNumber").exists());
    }

    @Test
    void phoneNumberBelongsToOneAccountButOwnerCanResendOrChangeIt() throws Exception {
        long owner = registerUser();
        long other = registerUserWithoutPhone();
        String ownerPhone = uniquePhoneNumber(owner);

        putAs(other, "/api/me/phone", "{\"phoneNumber\":\"%s\"}".formatted(ownerPhone))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PHONE_NUMBER_TAKEN"));

        putAs(owner, "/api/me/phone", "{\"phoneNumber\":\"%s\"}".formatted(ownerPhone)).andExpect(status().isOk());

        String newPhone = uniquePhoneNumber(other);
        putAs(owner, "/api/me/phone", "{\"phoneNumber\":\"%s\"}".formatted(newPhone))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phoneNumber").value(newPhone));
        putAs(other, "/api/me/phone", "{\"phoneNumber\":\"%s\"}".formatted(ownerPhone)).andExpect(status().isOk());
    }

    @Test
    void phoneRouteRequiresRegisteredUser() throws Exception {
        putAs(2L, "/api/me/phone", "{\"phoneNumber\":\"+5511999990000\"}")
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_REGISTERED"));
    }
}
