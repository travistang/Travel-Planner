package travis.diy.tracker

import android.hardware.Sensor
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.HttpUrl
import rx.Observable
import rx.Subscription
import travis.diy.locationtracker.R
import java.lang.reflect.Array.set

class SocketMessageListActivity : AppCompatActivity() {

    private val messages = ArrayList<Tracker.Reading>()
    private lateinit var messageArrayAdapter: MessageListAdapter

    private lateinit var tracker: Tracker
    private val socket  = RxWebSocket(HttpUrl.parse("https://echo.websocket.org")!!)
    private val JSON    = jacksonObjectMapper()
    private val sensorsToSubscribe: Array<Int> = arrayOf(Sensor.TYPE_STEP_DETECTOR,Sensor.TYPE_STEP_COUNTER,Sensor.TYPE_ACCELEROMETER)
    private var messageSubscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socket_message_list)
    }

    override fun onStart() {
        super.onStart()
        tracker = Tracker(sensorsToSubscribe,this,1000)
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
            messageSubscription = messageArrayAdapter
                    .consume(Observable.merge(tracker
                                .aggregateSensorValueObservable()
                            ,tracker.locationObservable),this)
        }
    }



}
