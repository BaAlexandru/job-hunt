package com.alex.job.hunt.jobhunt.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class AppUserDetails(
    private val userId: UUID,
    private val email: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>,
    private val enabled: Boolean
) : UserDetails {

    fun getUserId(): UUID = userId

    override fun getUsername(): String = email

    override fun getPassword(): String = password

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun isEnabled(): Boolean = enabled

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true
}
