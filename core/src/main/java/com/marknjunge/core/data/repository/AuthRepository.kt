package com.marknjunge.core.data.repository

import com.marknjunge.core.data.local.PreferencesRepository
import com.marknjunge.core.data.model.*
import com.marknjunge.core.data.network.service.AuthService
import com.marknjunge.core.data.network.GoogleSignInService
import com.marknjunge.core.utils.call

interface AuthRepository {
    suspend fun signInWithGoogle(idToken: String): Resource<User>

    suspend fun signUp(firstName: String, lastName: String, email: String, password: String): Resource<User>

    suspend fun signIn(email: String, password: String): Resource<User>

    suspend fun signOut(): Resource<Unit>

    suspend fun signOutLocally(): Resource<Unit>

    suspend fun requestPasswordReset(email: String): Resource<ApiResponse>

    suspend fun resetPassword(token: String, newPassword: String): Resource<ApiResponse>
}

internal class ApiAuthRepository(
    private val authService: AuthService,
    private val preferencesRepository: PreferencesRepository,
    private val googleSignInClient: GoogleSignInService
) : AuthRepository {
    override suspend fun signInWithGoogle(idToken: String): Resource<User> {
        return call {
            val response = authService.signInWithGoogle(SignInGoogleDto(idToken))

            preferencesRepository.user = response.user
            preferencesRepository.sessionId = response.session.sessionId

            response.user
        }
    }

    override suspend fun signUp(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Resource<User> {
        return call {
            val signUpDto = SignUpDto(firstName, lastName, password, email)
            val response = authService.signUp(signUpDto)

            preferencesRepository.user = response.user
            preferencesRepository.sessionId = response.session.sessionId

            response.user
        }
    }

    override suspend fun signIn(email: String, password: String): Resource<User> {
        return call {
            val response = authService.signIn(SignInDto(email, password))

            preferencesRepository.user = response.user
            preferencesRepository.sessionId = response.session.sessionId

            response.user
        }
    }

    override suspend fun signOut(): Resource<Unit> {
        return call {
            authService.signOut(preferencesRepository.sessionId)
            if (preferencesRepository.user!!.signInMethod == "GOOGLE") {
                googleSignInClient.signOut()
            }
            preferencesRepository.user = null
            preferencesRepository.sessionId = ""
        }
    }

    override suspend fun signOutLocally(): Resource<Unit> {
        return call {
            if (preferencesRepository.user!!.signInMethod == "GOOGLE") {
                googleSignInClient.signOut()
            }
            preferencesRepository.user = null
            preferencesRepository.sessionId = ""
        }
    }

    override suspend fun requestPasswordReset(email: String): Resource<ApiResponse> {
        return call {
            authService.requestPasswordReset(RequestPasswordResetDto(email))
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String): Resource<ApiResponse> {
        return call {
            authService.resetPassword(ResetPasswordDto(token, newPassword))
        }
    }
}