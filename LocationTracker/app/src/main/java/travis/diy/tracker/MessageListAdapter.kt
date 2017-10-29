package travis.diy.tracker

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okio.ByteString
import rx.Observable
import rx.Subscription
import rx.lang.kotlin.onError
import rx.lang.kotlin.onErrorReturnNull
import rx.schedulers.Schedulers
import travis.diy.locationtracker.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by travistang on 25/10/2017.
 */
open class MessageListAdapter(private val listView: Context,
                              val layout: Int,
                              private val array: ArrayList<Tracker.Reading>):
        ArrayAdapter<Tracker.Reading>(listView,layout,array)
{
    private val JSON = jacksonObjectMapper()

    inline private fun <reified T:Any> String.canBeDeserializedAs(cls: Class<T>): Boolean
    {
        try {
            JSON.readValue<T>(this)
            return true
        }catch (e: Throwable)
        {
            System.out.println(e)
            return false
        }
    }
    // get an intermediate observables but do not do subscription
    fun bind(source: Observable<out Tracker.Reading>,activity: Activity): Observable<out Tracker.Reading>
    {
        return source.doOnNext{
            reading ->
                activity.runOnUiThread {
                    this@MessageListAdapter.array.add(reading)
                    this.notifyDataSetChanged()
                }
        }.onError{e -> System.out.println(e)}
    }
    // get a subscription instead of observables
    fun consume(source: Observable<out Tracker.Reading>,activity: Activity): Subscription?
    {
        return source.subscribe({ reading ->
            run{
            this@MessageListAdapter.array.add(reading)
            activity.runOnUiThread { this.notifyDataSetChanged() }
            }},{e -> System.out.println(e)})
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View
    {
        val payload = this.array[position]
        val convertToString = fun(timestamp: Long): String = SimpleDateFormat("HH:mm:ss").format(Date(timestamp))

        if (null == convertView)
        {
            val inflater = LayoutInflater.from(this.listView)
            var view     = inflater.inflate(R.layout.message_list_cell,parent,false)
            var timestamp: Long
            when (payload)
            {
                is Tracker.GPSReading -> timestamp = payload.time
                is Tracker.SensorReading -> timestamp = payload.time
                else -> return view
            }
            view.findViewById<TextView>(R.id.timestamp_view).text = convertToString(timestamp)
            when (payload)
            {
                    is Tracker.GPSReading ->
                    {
                        val location = payload.location
                        view.findViewById<TextView>(R.id.location_view).text  = String.format("(%f,%f)",location.latitude,location.longitude)
                        view.findViewById<TextView>(R.id.message_view).text = "GPS"
                    }

                    is Tracker.SensorReading ->
                    {
                        view.findViewById<TextView>(R.id.location_view).text  = payload.reading.joinToString(",")
                        view.findViewById<TextView>(R.id.message_view).text = "STEP"
                    }
            }
            return view
        } else
        {
            return convertView!!
        }
    }
}