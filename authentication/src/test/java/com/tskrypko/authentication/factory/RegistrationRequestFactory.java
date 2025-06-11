package com.tskrypko.authentication.factory;

import com.tskrypko.authentication.dto.RegistrationRequest;

public class RegistrationRequestFactory {

    public static RegistrationRequest createRegistrationRequest(String username, String password, String confirmPassword, String email, String firstName, String lastName, String location, String bio) {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setConfirmPassword(confirmPassword);
        request.setEmail(email);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setBio(bio);
        request.setLocation(location);

        return request;
    }

    public static RegistrationRequest createRegistrationValidRequest() {
        return createRegistrationRequest(
                "testUser",
                "securePassword",
                "securePassword",
                "test@example.com",
                "Test",
                "User",
                "TestLocation",
                "Lorem ipsum dolor sit amet"
        );
    }

    public static RegistrationRequest createRegistrationRequestDifferentPasswords() {
        RegistrationRequest request = createRegistrationValidRequest();
        request.setConfirmPassword("securePassworq");
        return request;
    }
}
