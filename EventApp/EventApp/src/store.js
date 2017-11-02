import Vuex from 'vuex'
import Vue from 'vue'

Vue.use(Vuex)

var store = new Vuex.Store({
	state: {
		calendarEvents: [
		{
			// TODO: remove me

			title : 'Sunny Out of Office',
			start : '2017-10-20',
			end : '2017-11-27'
		},
		],
		editingEvents: null,
		eventSearchWord: "",
	},

	mutations: {
		add_event(state,event) {
			state.push(event)
		}
	},
	//strict: true
})

export default store
