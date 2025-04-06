package app.aaps.implementation.protection

import app.aaps.core.keys.BooleanKey
import app.aaps.shared.tests.TestBaseWithProfile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`

class ExportPasswordDataStoreImplTest : TestBaseWithProfile() {

    private val somePassword = "somePassword"

    val sut: ExportPasswordDataStoreImpl by lazy {
        ExportPasswordDataStoreImpl(aapsLogger, preferences, config)
    }

    @Test
    fun exportPasswordStoreEnabled() {
        // When disabled
        preferences.put(BooleanKey.MaintenanceEnableExportSettingsAutomation, false)
        assertFalse(sut.exportPasswordStoreEnabled())

        // When enabled
        preferences.put(BooleanKey.MaintenanceEnableExportSettingsAutomation, true)
        assertTrue(sut.exportPasswordStoreEnabled())
        assertTrue(sut.clearPasswordDataStore(context).isEmpty())

        // These will fail to run (can not instantiate secure encrypt?)
        // assertTrue(sut.putPasswordToDataStore(context, somePassword) == somePassword)
        // assertTrue(sut.getPasswordFromDataStore(context) == Triple ("", true, true))
    }

    @Test
    fun clearPasswordDataStore() {
        preferences.put(BooleanKey.MaintenanceEnableExportSettingsAutomation, false)
        assertTrue(sut.clearPasswordDataStore(context).isEmpty())
    }

    @Test
    fun putPasswordToDataStore() {
        preferences.put(BooleanKey.MaintenanceEnableExportSettingsAutomation, false)
        assertTrue(sut.putPasswordToDataStore(context, somePassword) == somePassword)
    }

    @Test
    fun getPasswordFromDataStore() {
        preferences.put(BooleanKey.MaintenanceEnableExportSettingsAutomation, false)
        assertTrue(sut.getPasswordFromDataStore(context) == Triple("", true, true))
    }
}