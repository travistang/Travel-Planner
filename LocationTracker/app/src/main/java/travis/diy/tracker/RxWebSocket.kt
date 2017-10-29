package travis.diy.tracker

import com.squareup.okhttp.ws.WebSocket
import okhttp3.*
import rx.Observable
import rx.Subscriber
import okio.ByteString
import rx.Scheduler
import rx.Subscription
import rx.schedulers.Schedulers
import travis.diy.RxComponent.RxComponent
import java.util.*

/**
 * Created by travistang on 24/10/2017.
 */

class RxWebSocket<T,U>(val url: HttpUrl
                       ,val byteToOutFunc: (ByteString) -> U
                       ,val InToByteFunc: (T) -> ByteString
                       ,override val inputSource: Observable<T>)
    :RxComponent<T,U>()
{
    private val client = OkHttpClient()
    private val request = Request.Builder().url(url).build()
    private val subscriptionList = ArrayList<Subscription>()
    private var inputSourceSubscription: Subscription? =
            inputSource
            .map{input -> InToByteFunc(input)}
            .subscribe{bs -> send(bs)}
    private lateinit var listener: WebSocketListener

    fun subscribe(s: Subscriber<U>): Subscription
    {
        val sub = this.outputSource.subscribe(s)
        subscriptionList.add(sub)
        // TODO: restart socket if not started
        if (socket == null) openConnection()
        return sub
    }

    fun unsubscribe(s: Subscription)
    {
        if (subscriptionList.find { sl -> sl === s } != null)
        {
            subscriptionList.remove(s)
            s.unsubscribe()
        }
        if (isSocketIdling()) closeConnection()
    }

    override val outputSource = Observable.create(object: Observable.OnSubscribe<U>
    {
        override fun call(t: Subscriber<in U>?)
        {
            listener = object: WebSocketListener()
            {
                override fun onMessage(webSocket: okhttp3.WebSocket?, bytes: ByteString?) {
                    if (bytes != null)
                        t?.onNext(byteToOutFunc(bytes))
                }

                override fun onMessage(webSocket: okhttp3.WebSocket?, text: String?) {
                    if (text != null)
                        t?.onNext(byteToOutFunc(ByteString.encodeUtf8(text)))
                }

                override fun onClosed(webSocket: okhttp3.WebSocket?, code: Int, reason: String?) {
                    t?.onCompleted()
                }

                override fun onOpen(webSocket: okhttp3.WebSocket?, response: Response?) {
                    t?.onStart()
                }
            }
            openConnection()
        }
    })


    private var socket: okhttp3.WebSocket? = null

    /*
          general methods
     */
    // still give this because some people who dont use RxKotlin
    fun send(bs: ByteString) = socket?.send(bs)



    fun isSocketIdling(): Boolean
    {
        return subscriptionList.isEmpty()
    }

    fun closeConnection()
    {
        socket?.close(1000,null)
        socket = null
    }

    private fun openConnection()
    {
        if(socket == null) {
            socket = client.newWebSocket(request, listener)
        }
    }
}