package com.CourtAssist.service.user;

import com.CourtAssist.model.Users;
import com.CourtAssist.repository.UsersRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsersRepository usersRepository;

    public UserDetailsServiceImpl(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        System.out.println("User detail service method called with identifier: " + identifier);

        // Try different lookup strategies in order
        Users user = usersRepository.findByUsername(identifier)
                .orElseGet(() -> usersRepository.findByEmail(identifier)
                        .orElseGet(() -> usersRepository.findByMobileNo(identifier)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier))));

        System.out.println("User found: " + user.getUsername());
        return new UserPrincipal(user);
    }
}