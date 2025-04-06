package app.aaps.plugins.sync.nsclient

import app.aaps.core.interfaces.receivers.ReceiverStatusStore
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.events.EventChargingState
import app.aaps.core.interfaces.rx.events.EventNetworkChange
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.StringKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.shared.tests.TestBase
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class ReceiverDelegateTest : TestBase() {

    @Mock lateinit var preferences: Preferences
    @Mock lateinit var rh: ResourceHelper
    @Mock lateinit var fabricPrivacy: FabricPrivacy

    @Mock private lateinit var receiverStatusStore: ReceiverStatusStore
    private lateinit var sut: ReceiverDelegate

    @BeforeEach
    fun prepare() {
        //receiverStatusStore = ReceiverStatusStore(context, rxBus)
        sut = ReceiverDelegate(rxBus, rh, preferences, receiverStatusStore, aapsSchedulers, fabricPrivacy)
    }

    @Test
    fun testCalculateStatusChargingState() {
        preferences.put(BooleanKey.NsClientUseOnBattery, true)
        preferences.put(BooleanKey.NsClientUseOnCharging, false)
        assertThat(sut.calculateStatus(EventChargingState(false, 0))).isTrue()
        assertThat(sut.calculateStatus(EventChargingState(true, 0))).isFalse()
        preferences.put(BooleanKey.NsClientUseOnBattery, false)
        preferences.put(BooleanKey.NsClientUseOnCharging, true)
        assertThat(sut.calculateStatus(EventChargingState(true, 0))).isTrue()
        assertThat(sut.calculateStatus(EventChargingState(false, 0))).isFalse()
    }

    @Test
    fun testCalculateStatusNetworkState() {
        preferences.put(BooleanKey.NsClientUseCellular, true)
        preferences.put(BooleanKey.NsClientUseRoaming, true)
        preferences.put(BooleanKey.NsClientUseWifi, true)
        preferences.put(StringKey.NsClientWifiSsids, "")
        assertThat(sut.calculateStatus(EventNetworkChange(mobileConnected = true, wifiConnected = false, roaming = true))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange(mobileConnected = true, wifiConnected = false, roaming = false))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange(ssid = "<unknown ssid>", mobileConnected = true, wifiConnected = true))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange(ssid = "<unknown ssid>", mobileConnected = false, wifiConnected = true))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange())).isFalse()

        preferences.put(StringKey.NsClientWifiSsids, "test 1")
        assertThat(sut.calculateStatus(EventNetworkChange(mobileConnected = true, wifiConnected = false, roaming = true))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange(mobileConnected = true, wifiConnected = false, roaming = false))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange(ssid = "<unknown ssid>", mobileConnected = true, wifiConnected = true))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange(ssid = "<unknown ssid>", mobileConnected = false, wifiConnected = true))).isFalse()
        assertThat(sut.calculateStatus(EventNetworkChange(ssid = "test 1", mobileConnected = true, wifiConnected = true))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange(ssid = "test 1", mobileConnected = false, wifiConnected = true))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange())).isFalse()

        preferences.put(BooleanKey.NsClientUseCellular, false)
        preferences.put(BooleanKey.NsClientUseWifi, true)
        preferences.put(BooleanKey.NsClientUseRoaming, true)
        preferences.put(StringKey.NsClientWifiSsids, "")
        assertThat(sut.calculateStatus(EventNetworkChange(wifiConnected = true))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange())).isFalse()
        assertThat(sut.calculateStatus(EventNetworkChange(mobileConnected = true))).isFalse()

        preferences.put(BooleanKey.NsClientUseCellular, true)
        preferences.put(BooleanKey.NsClientUseWifi, true)
        preferences.put(BooleanKey.NsClientUseRoaming, false)
        preferences.put(StringKey.NsClientWifiSsids, "")
        assertThat(sut.calculateStatus(EventNetworkChange(mobileConnected = true, roaming = false))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange(mobileConnected = true, roaming = true))).isFalse()

        preferences.put(BooleanKey.NsClientUseCellular, true)
        preferences.put(BooleanKey.NsClientUseWifi, true)
        preferences.put(BooleanKey.NsClientUseRoaming, true)
        preferences.put(StringKey.NsClientWifiSsids, "")
        assertThat(sut.calculateStatus(EventNetworkChange(mobileConnected = true, roaming = false))).isTrue()
        assertThat(sut.calculateStatus(EventNetworkChange(mobileConnected = true, roaming = true))).isTrue()
    }
}
