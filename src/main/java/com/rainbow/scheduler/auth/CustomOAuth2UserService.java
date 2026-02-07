package com.rainbow.scheduler.auth;

import com.rainbow.scheduler.model.User;
import com.rainbow.scheduler.model.UserRole;
import com.rainbow.scheduler.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        if (email == null && "github".equals(registrationId)) {
            // GitHub might not provide email in default attributes
            email = attributes.get("login") + "@github.com";
        }

        String finalEmail = email;
        Optional<User> userOptional = userRepository.findByEmail(finalEmail);

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setName((String) attributes.get("name"));
            user.setAvatar((String) attributes.get("picture"));
        } else {
            user = User.builder()
                    .email(finalEmail)
                    .name((String) attributes.get("name"))
                    .avatar((String) attributes.get("picture"))
                    .provider(registrationId)
                    .providerId(oAuth2User.getName())
                    .role(userRepository.count() == 0 ? UserRole.ROLE_MANAGER : UserRole.ROLE_OPERATOR)
                    .build();
        }

        userRepository.save(user);
        return oAuth2User;
    }
}
