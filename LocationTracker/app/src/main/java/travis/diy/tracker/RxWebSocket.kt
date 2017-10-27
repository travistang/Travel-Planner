package travis.diy.tracker

import com.squareup.okhttp.ws.WebSocket
import okhttp3.*
import rx.Observable
import rx.Subscriber
import okio.ByteString
import rx.Scheduler
import rx.Subscription
import rx.schedulers.Schedulers
import java.util.*

/**
 * Created by travistang on 24/10/2017.
 */

class RxWebSocket(val url: HttpUrl)
{
    private val client = OkHttpClient()
    private val request = Request.Builder().url(url).build()

    private var hasSubscriber = false

    val messageObservable = Observable.create(object: Observable.OnSubscribe<ByteString>
    {
        override fun call(t: Subscriber<in ByteString>?)
        {
            val listener = object: WebSocketListener()
            {
                override fun onMessage(webSocket: okhttp3.WebSocket?, bytes: ByteString?) {
                    t?.onNext(bytes)
                }

                override fun onMessage(webSocket: okhttp3.WebSocket?, text: String?) {
                    t?.onNext(ByteString.encodeUtf8(text))
                }

                override fun onClosed(webSocket: okhttp3.WebSocket?, code: Int, reason: String?) {
                    t?.onCompleted()
                    hasSubscriber = false
                }

                override fun onOpen(webSocket: okhttp3.WebSocket?, response: Response?) {
                    t?.onStart()
                    hasSubscriber = true
                }
            }
            openConnection(listener)
        }
    })

    private var socket: okhttp3.WebSocket? = null

    /*
          general methods
     */
    fun send(data: ByteString)
    {
        socket?.send(data)
    }

    fun subscribeMessage(subscriber: Subscriber<in ByteString>): Subscription?
    {
        if(hasSubscriber) return null
        return messageObservable
                .subscribeOn(Schedulers.io())
                .subscribe(subscriber)
    }

    fun <T> subscribeWithMap(subscriber: Subscriber<in T>, mapper: (ByteString) -> T): Subscription?
    {
        if(hasSubscriber) return null
        return messageObservable
                .map({m -> mapper(m)})
                .subscribeOn(Schedulers.io())
                .subscribe(subscriber)
    }

    fun closeConnection(subscription: Subscription)
    {
        socket?.close(1000,null)
        socket = null
        subscription.unsubscribe()
    }

    private fun openConnection(listener: WebSocketListener)
    {
        if(socket == null)
            socket = client.newWebSocket(request,listener)
    }

}