package  com.bih.sosapp.domain

import com.bih.sosapp.domain.PhoneNumberValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberValidatorTest {
    @Test
    fun acceptsTypicalEmergencyNumbers() {
        assertTrue(PhoneNumberValidator.isValid("112"))
        assertTrue(PhoneNumberValidator.isValid("+1 (555) 010-9090"))
        assertTrue(PhoneNumberValidator.isValid("112, +1 (555) 010-9090"))
    }

    @Test
    fun rejectsBlankOrLetters() {
        assertFalse(PhoneNumberValidator.isValid(""))
        assertFalse(PhoneNumberValidator.isValid("help-me"))
        assertFalse(PhoneNumberValidator.isValid("112, help-me"))
    }
}
