package travis.diy.RxComponent

import rx.Observable
import rx.Subscriber
import rx.Subscription

/**
 * Created by travistang on 29/10/2017.
 */
/**
 * Components that consumes input source with type In
 * And emits
 */
abstract class RxComponent<In,Out> {
    abstract val outputSource: Observable<Out>
    abstract val inputSource: Observable<In>
    
}