package id.hirejob.kiosk.trigger

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class HttpTrigger(private val port: Int) : TriggerSource {
    private val _isOn = MutableStateFlow(false)
    override val isOn = _isOn.asStateFlow()

    private var server: NanoHTTPD? = null

    override fun start() {
        if (server != null) return
        server = object : NanoHTTPD(port) {
            override fun serve(session: IHTTPSession?): Response {
                val uri = session?.uri ?: "/"
                val method = session?.method
                return when {
                    method == Method.GET && uri == "/health" ->
                        newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
                    method == Method.POST && uri == "/trigger" -> {
                        session.parseBody(HashMap())
                        val body = session.parms["postData"] ?: ""
                        return try {
                            val j = JSONObject(body)
                            val state = j.optString("state").lowercase()
                            when (state) {
                                "on"  -> _isOn.value = true
                                "off" -> _isOn.value = false
                            }
                            newFixedLengthResponse("{\"ok\":true}")
                        } catch (t: Throwable) {
                            Log.e("HttpTrigger", "bad body", t)
                            newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json",
                                "{\"ok\":false}")
                        }
                    }
                    else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404")
                }
            }
        }.apply {
            try { start(SOCKET_READ_TIMEOUT, false) } catch (t: Throwable) { Log.e("HttpTrigger","start",t) }
        }
    }

    override fun stop() {
        server?.stop()
        server = null
    }
}
