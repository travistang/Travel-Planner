package travis.diy.tracker

import android.hardware.Sensor
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.HttpUrl
import okio.ByteString
import rx.Observable
import rx.Subscription
import rx.lang.kotlin.onError
import travis.diy.locationtracker.R
import java.lang.reflect.Array.set

class SocketMessageListActivity : AppCompatActivity() {

    private val messages = ArrayList<Tracker.Reading>()
    private lateinit var messageArrayAdapter: MessageListAdapter

    private lateinit var tracker: Tracker
    private lateinit var socket: RxWebSocket<out Tracker.Reading, out Tracker.Reading>

    private val url = HttpUrl.parse("https://echo.websocket.org")!!

    private val JSON    = jacksonObjectMapper()
    private val sensorsToSubscribe: Array<Int> = arrayOf(Sensor.TYPE_STEP_DETECTOR)
    private var messageSubscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socket_message_list)
    }

    override fun onStart() {
        super.onStart()
        tracker = Tracker(sensorsToSubscribe,this,7000)
        messageArrayAdapter = MessageListAdapter(this,R.layout.message_list_cell,messages)
        val list = findViewById<ListView>(R.id.messageList)
        list.adapter = messageArrayAdapter
        // create sensor <-> listview subscriptions
        subscribeTracker()
    }

    override fun onPause() {
        super.onPause()
        messageSubscription?.unsubscribe()
    }

    override fun onResume()
    {
        super.onResume()
        subscribeTracker()
    }

    private fun subscribeTracker()
    {
        if(messageSubscription == null)
        {
            val trackerReadingObservable = messageArrayAdapter
                    .bind(Observable.merge(tracker
                                .aggregateSensorValueObservable()
                            ,tracker.locationObservable),this)
                    .onError { e ->
                        runOnUiThread {
                            Toast.makeText(this@SocketMessageListActivity,e.toString(),Toast.LENGTH_LONG)
                        }
                    }

            // subscribe to message


            val byteToReading: ByteString.() -> Tracker.Reading = {
                val json = JSON.readValue<Tracker.Reading>(this.toByteArray())
                json
            }
            // web socket initialization
            socket = RxWebSocket(
                    url = url,
                    byteToOutFunc = { bs -> bs.byteToReading() },
                    InToByteFunc = {reading ->
                        ByteString.encodeUtf8(JSON.writeValueAsString(reading)) },
                    inputSource = trackerReadingObservable)

            messageSubscription = trackerReadingObservable.subscribe()

            socket.outputSource.subscribe { bs -> System.out.println(bs.toString()) }
        }
    }


}
