package com.company.I_SPICE.services;

import com.company.I_SPICE.model.User;
import com.company.I_SPICE.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the
            // OAuth2AuthenticationFailureHandler
            ex.printStackTrace();
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Update existing user logic if necessary
        } else {
            user = new User();
            user.setEmail(email);

            // Generate a random password for OAuth2 users since they won't use it
            user.setPassword(UUID.randomUUID().toString());

            // Extract names from Google profile
            String name = oAuth2User.getAttribute("name");
            String givenName = oAuth2User.getAttribute("given_name");
            String familyName = oAuth2User.getAttribute("family_name");

            user.setFirstName(givenName != null ? givenName : "GoogleUser");
            user.setLastName(familyName != null ? familyName : "");
            user.setFullName(name != null ? name : user.getFirstName() + " " + user.getLastName());

            // Set required fields
            String baseUsername = name != null ? name.trim() : email.split("@")[0];

            String finalUsername = baseUsername;
            int suffix = 1;
            while (userRepository.findByUsername(finalUsername).isPresent()) {
                finalUsername = baseUsername + suffix;
                suffix++;
            }
            user.setUsername(finalUsername);
            user.setEmailVerified(true); // OAuth2 providers verify emails
            user.setRole("USER");
            user.setLoyaltyPoints(50);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.save(user);
        }

        // Map authorities from database
        Set<GrantedAuthority> authorities = new HashSet<>(oAuth2User.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        String userNameAttributeName = oAuth2UserRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), userNameAttributeName);
    }
}
