<template>
	<div>
		<mu-appbar title="Event Manager">
			<mu-flat-button color="white" label="Add Event" slot="right"/>
		</mu-appbar>
		<full-calendar :events="events" locale="en"></full-calendar>
	</div>
</template>

<script>
import 'muse-ui/dist/muse-ui.css'
export default {
	name: 'Main',
	data () {
		return {
			events: this.$store.state.calendarEvents
		}
	},
	components : {
		'full-calendar': require('vue-fullcalendar')
	},
	beforeMount() {
		// invoke loading of the store
	},

}
</script>
