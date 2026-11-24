package br.edu.ifsp.ifrota.poc_admin_entregas.core

import org.junit.Assert.assertEquals
import org.junit.Test

class UserRoleTest {

    @Test
    fun `somente o valor admin concede o papel de administrador`() {
        assertEquals(UserRole.ADMIN, UserRole.fromFirestore("admin"))
    }

    @Test
    fun `ausente ou desconhecido e sempre motorista`() {
        assertEquals(UserRole.DRIVER, UserRole.fromFirestore(null))
        assertEquals(UserRole.DRIVER, UserRole.fromFirestore("driver"))
        assertEquals(UserRole.DRIVER, UserRole.fromFirestore("ADMIN"))
        assertEquals(UserRole.DRIVER, UserRole.fromFirestore(true))
        assertEquals(UserRole.DRIVER, UserRole.fromFirestore(""))
    }
}
