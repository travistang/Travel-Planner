package travis.diy.tracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationProvider
import kotlinx.websocket.*
import com.squareup.okhttp.*
import kotlinx.websocket.gson.withGsonConsumer
import kotlinx.websocket.gson.withGsonProducer
import rx.Observable
import rx.Subscriber
import rx.internal.operators.OperatorToObservableList
import rx.lang.kotlin.*
import java.util.*
import java.util.concurrent.TimeUnit

class Tracker(val serverURL: String,val context: Context,val socketSubscriber: Subscriber<Payload>)
{
    var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var sensorManager   = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    var locationObservable = Observable.create(object: Observable.OnSubscribe<Location>
    {
        override fun call(subscriber: Subscriber<in Location>?)
        {
            if(subscriber == null) return
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            {
                subscriber.onError(RuntimeException("GPS provided not enabled"))
            }
            while (true)
            {
                try
                {
                    subscriber.onNext(locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER))
                }catch(e: SecurityException)
                {
                    subscriber.onError(e)
                }
            }
        }
    }) as Observable<Location>

    var motionSensorObservable = Sensor.TYPE_MOTION_DETECT.sensorObservable(1000000)
    var allObservables = Observable.zip(locationObservable,motionSensorObservable, {
        // zipper
        location,motion -> Payload(location,motion)
    })
    var webSocket = OkHttpClient()
            .newWebSocket(serverURL)
            .withGsonProducer(allObservables)
            .withGsonConsumer(socketSubscriber)
            .open()

    fun closeConnection()
    {
        webSocket.close(CloseReason(closeCode = 1 as CloseCode,message = "Requested by client"))
    }
    data class SensorReading(val reading: FloatArray,val timestamp: Long) {
        override fun equals(other: Any?): Boolean
        {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as SensorReading

            if (!Arrays.equals(reading, other.reading)) return false
            return true
        }
    }

    data class Payload(val location: Location, val step: SensorReading)

    fun Int.sensorObservable(samplingTime: Int) : Observable<SensorReading> =
         Observable.create(object : Observable.OnSubscribe<SensorReading>
         {
             override fun call(subscriber: Subscriber<in SensorReading>)
             {
                 val tracker = this@Tracker
                 val sensor = tracker.sensorManager.getDefaultSensor(this@sensorObservable) as Sensor
                 try
                 {
                    tracker.sensorManager.registerListener(object: SensorEventListener {
                        override fun onSensorChanged(sensorEvent: SensorEvent?)
                        {
                            if(sensorEvent != null)
                            {
                                subscriber.onNext(SensorReading(sensorEvent.values,sensorEvent.timestamp))
                            }
                        }
                        override fun onAccuracyChanged(p0: Sensor?, p1: Int){}
                    },sensor,samplingTime)
                 }catch(e: Exception)
                 {
                     subscriber.onError(e)
                 }finally
                 {
                     subscriber.onCompleted()
                 }
             }

         })
}