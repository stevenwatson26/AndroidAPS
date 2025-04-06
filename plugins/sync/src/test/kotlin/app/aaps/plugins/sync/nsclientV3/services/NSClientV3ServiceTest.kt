package app.aaps.plugins.sync.nsclientV3.services

import app.aaps.core.interfaces.nsclient.StoreDataForDb
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.StringKey
import app.aaps.plugins.sync.nsShared.NsIncomingDataProcessor
import app.aaps.plugins.sync.nsclient.data.NSDeviceStatusHandler
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import app.aaps.shared.tests.TestBaseWithProfile
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NSClientV3ServiceTest : TestBaseWithProfile() {

    @Mock lateinit var nsIncomingDataProcessor: NsIncomingDataProcessor
    @Mock lateinit var storeDataForDb: StoreDataForDb
    @Mock lateinit var uiInteraction: UiInteraction
    @Mock lateinit var nsDeviceStatusHandler: NSDeviceStatusHandler
    @Mock lateinit var nsClientV3Plugin: NSClientV3Plugin

    private lateinit var sut: NSClientV3Service
    private val port = 45454
    private lateinit var socketIOServer: CustomSocketIOServer

    @BeforeEach
    fun init() {
        sut = NSClientV3Service().also {
            it.injector = injector
            it.aapsLogger = aapsLogger
            it.rxBus = rxBus
            it.rh = rh
            it.preferences = preferences
            it.fabricPrivacy = fabricPrivacy
            it.nsClientV3Plugin = nsClientV3Plugin
            it.config = config
            it.nsIncomingDataProcessor = nsIncomingDataProcessor
            it.storeDataForDb = storeDataForDb
            it.uiInteraction = uiInteraction
            it.nsDeviceStatusHandler = nsDeviceStatusHandler
        }
        socketIOServer = CustomSocketIOServer(port, "/storage", aapsLogger)
        socketIOServer.start()
    }

    @AfterEach
    fun tearDown() {
        socketIOServer.stop()
    }

    @Test
    fun initializeWebSocketsTest() {
        // No url specified
        `when`(preferences.get(StringKey.NsClientUrl)).thenReturn("")
        sut.initializeWebSockets("Test")
        sut.shutdownWebsockets()
        assertThat(sut.storageSocket).isNull()
        // Socket should be created
        `when`(preferences.get(StringKey.NsClientUrl)).thenReturn("http://something")
        `when`(preferences.get(BooleanKey.NsClientNotificationsFromAnnouncements)).thenReturn(true)
        `when`(nsClientV3Plugin.isAllowed).thenReturn(true)
        sut.initializeWebSockets("Test")
        assertThat(sut.storageSocket).isNotNull()
        assertThat(sut.alarmSocket).isNotNull()
        sut.shutdownWebsockets()
    }

    @Test
    fun `test receiving messages from server`() {
        val latch = CountDownLatch(1) // To wait for the message
        var receivedMessage: String? = null

        // Setup listener on the server
        // socketIoServer.addConnectListener { client: SocketIOClient ->
        //     client.addEventListener("message", String::class.java, object : DataListener<String> {
        //         override fun onData(client: SocketIOClient?, data: String?, ackSender: com.corundumstudio.socketio.AckRequest?) {
        //             receivedMessage = data
        //             latch.countDown()
        //         }
        //     })
        // }

        // connect to the server
        `when`(preferences.get(StringKey.NsClientUrl)).thenReturn("http://localhost:$port")
        `when`(preferences.get(BooleanKey.NsClientNotificationsFromAnnouncements)).thenReturn(true)
        `when`(nsClientV3Plugin.isAllowed).thenReturn(true)
        sut.initializeWebSockets("Test")
        val storageSocket = sut.storageSocket!!
        // Emit message to be received by the server
        storageSocket.emit("message", "Hello From Client")

        // Wait for the message to be received, with a timeout
        latch.await(10, TimeUnit.SECONDS)

        assertThat(receivedMessage).isEqualTo("Hello From Client")
        sut.shutdownWebsockets()
    }
}