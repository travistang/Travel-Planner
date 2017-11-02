import Vue from 'vue'
import Router from 'vue-router'
import HelloWorld from '@/components/HelloWorld'
import Main from '@/components/Main'
import MuseUI from 'muse-ui'
Vue.use(Router)
Vue.use(MuseUI)

export default new Router({
  routes: [
    {
      path: '/',
			name: 'Main',
			component: Main
      //name: 'Hello',
      //component: HelloWorld
    }
  ]
})
