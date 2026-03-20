package com.alex.job.hunt.jobhunt.security

import com.alex.job.hunt.jobhunt.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("User not found")

        return AppUserDetails(
            userId = user.id!!,
            email = user.email,
            password = user.password,
            authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")),
            enabled = user.enabled
        )
    }
}
