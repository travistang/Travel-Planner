package travis.diy.tracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import rx.Observable
import rx.Subscriber
import java.util.*
import java.util.concurrent.TimeUnit


class Tracker(val sensors: Array<Int>, val context: Context,val samplingTime: Int)
{

    private var locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var sensorManager   = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    var locationObservable = Observable.create(object: Observable.OnSubscribe<GPSReading>
    {
        override fun call(t: Subscriber<in GPSReading>?)
        {
            val listener = object: LocationListener
            {
                override fun onLocationChanged(p0: Location?) {
                    if (p0 != null)
                        t?.onNext(GPSReading(p0,System.currentTimeMillis()))
                }

                override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

                }

                override fun onProviderEnabled(p0: String?) {
                    t?.onStart()
                }

                override fun onProviderDisabled(p0: String?) {
                    t?.onCompleted()
                }
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0f,listener)
        }
    })


    // data classes
    abstract class Reading(val timestamp: Long)
    data class SensorReading(val reading: FloatArray, val time: Long) : Reading(time) {
        override fun equals(other: Any?): Boolean
        {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as SensorReading

            if (!Arrays.equals(reading, other.reading) || time != other.time) return false
            return true
        }
    }
    data class GPSReading(val location: Location, val time:Long) : Reading(time)

    // util functions
    fun Int.sensorObservable(samplingTime: Int) : Observable<SensorReading> =
         Observable.create(object : Observable.OnSubscribe<SensorReading>
         {
             override fun call(subscriber: Subscriber<in SensorReading>)
             {
                 val tracker = this@Tracker
                 val sensor = tracker.sensorManager.getDefaultSensor(this@sensorObservable) as Sensor
                 if (sensor == null)
                 {
                     subscriber.onError(RuntimeException("Cannot get sensor"))
                     return
                 }
                 try
                 {
                    tracker.sensorManager.registerListener(object: SensorEventListener {
                        override fun onSensorChanged(sensorEvent: SensorEvent?)
                        {
                            if(sensorEvent != null)
                            {
                                subscriber.onNext(SensorReading(sensorEvent.values,sensorEvent.timestamp))
                            }else
                            {
                                System.out.println("Sensor is giving null")
                            }
                        }
                        override fun onAccuracyChanged(p0: Sensor?, p1: Int){}
                    },sensor,10000000)
                 }catch(e: Exception)
                 {
                     subscriber.onError(e)
                 }finally
                 {
//                     subscriber.onCompleted()
                 }
             }

         })
                 .throttleFirst(samplingTime.toLong(),TimeUnit.MICROSECONDS)

    // main function
    fun aggregateSensorValueObservable() : Observable<out Reading>
    {
//        return sensors[0].sensorObservable(this@Tracker.samplingTime)
        // TODO: merge all sensors back again
        return Observable
                .merge(sensors
                    .filter({sensor -> sensorManager    // make sure all the sensor flags are valid
                        .getSensorList(Sensor.TYPE_ALL)
                        .contains(sensorManager.getDefaultSensor(sensor))})
                    .map({sensor -> sensor.sensorObservable(this@Tracker.samplingTime)})
                )
    }

}