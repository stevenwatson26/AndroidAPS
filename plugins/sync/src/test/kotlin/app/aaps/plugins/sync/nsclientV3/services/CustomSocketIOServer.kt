package app.aaps.plugins.sync.nsclientV3.services

import app.aaps.core.interfaces.logging.AAPSLogger
import io.socket.engineio.server.Emitter
import io.socket.engineio.server.EngineIoServer
import io.socket.engineio.server.EngineIoSocket
import io.socket.engineio.server.parser.Packet
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class CustomSocketIOServer(private val port: Int, private val path: String, private val aapsLogger: AAPSLogger) {

    private var jettyServer: Server? = null
    // Engine.IO configuration
    private val engineIoServer: EngineIoServer = EngineIoServer()

    // Keep track of the connected sockets
    private val connectedSockets: MutableMap<String?, EngineIoSocket> = ConcurrentHashMap<String?, EngineIoSocket>()
    private val nextSocketId = AtomicInteger(0)

    init {
        // Handle new socket connection
        engineIoServer.on("connection", Emitter.Listener { args ->
            val socket = args[0] as EngineIoSocket
            val socketId = "socket-" + nextSocketId.incrementAndGet()
            connectedSockets.put(socketId, socket)
            aapsLogger.debug("New socket connected: $socketId")

            //Handle new message
            socket.on("message", Emitter.Listener { args1 ->
                val message = args1[0] as String
                aapsLogger.debug("Message from socket: $message")
                sendMessageToSocket(socket, "Server received: $message")
                broadcastMessage("Broadcast from server: $message")
            })

            //Handle disconnection
            socket.on("close", Emitter.Listener { args1 ->
                connectedSockets.entries.stream()
                    .filter { entry  -> entry.value == socket }
                    .map { it.key }
                    .findFirst()
                    .ifPresent(Consumer { id ->
                        connectedSockets.remove(id)
                        aapsLogger.debug("Socket disconnected: $id")
                    })
            })
        })
    }

    fun start() {
        // Create an embedded Jetty server
        jettyServer = Server(port)
        val connector = ServerConnector(jettyServer)
        jettyServer?.addConnector(connector)

        // Attach Engine.IO to the Jetty server using a Servlet
        val context = ServletContextHandler(ServletContextHandler.SESSIONS)
        context.setContextPath("/");
        //context.setContextPath(path)
        jettyServer?.setHandler(context)
        val servletHolder = ServletHolder(EngineIoServlet(engineIoServer))
        context.addServlet(servletHolder, "/*")

        // Start the server
        jettyServer?.start()
        aapsLogger.debug("Custom Socket.IO-like server started on port: $port")
    }

    fun stop() {
            jettyServer?.stop()
    }

    //Send message to a specific socket
    fun sendMessageToSocket(socket: EngineIoSocket, message: String) {
        socket.send(Packet<String>(Packet.MESSAGE, message))
    }

    //Broadcast message to all sockets
    fun broadcastMessage(message: String) {
        for (socket in connectedSockets.values) {
            socket.send(Packet<String>(Packet.MESSAGE, message))
        }
    }

    // Custom Servlet to handle Engine.IO requests
    class EngineIoServlet(private val engineIoServer: EngineIoServer) : HttpServlet() {

        @Throws(ServletException::class, IOException::class)
        override fun service(request: HttpServletRequest, response: HttpServletResponse) {
            engineIoServer.handleRequest(request, response)
        }
    }
}