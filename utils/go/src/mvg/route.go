package mvg

import (
	"time"
)

type Route struct {
	From,To Station
	Departure,Arrival int64
	Product,Label string
	DeparturePlatform, ArrivalPlatform string
	// TODO: add me back later
	//Notifications Notifications,
	ConnectionPartList []Route
	ConnectionPartType string
}

func (route *Route) IsPartial() bool {
	return len(route.ConnectionPartList) == 0
}

func (route *Route) GetDepartureTime() time.Time {
	return time.Unix(route.Departure / 1000,0)
}

func (route Route) GetArrivalTime() time.Time {
	return time.Unix(route.Arrival / 1000,0)
}

func (route *Route) GetDuration() time.Duration {
	return route.GetArrivalTime().Sub(route.GetDepartureTime())
}

func (route *Route) TotalWalkingTime() time.Duration {
	if route.IsPartial() {
		if route.ConnectionPartType == "FOOT" {
			return route.GetDuration()
		}
		return  time.Now().Sub(time.Now()) // return 0 duration...
	} else {
		// this is the total route...
		now,future := time.Now(),time.Now()
		// accumulate all walking durations and add the time to "future"
		for _,partial := range route.ConnectionPartList {
			future = future.Add(partial.TotalWalkingTime())
		}
		// then get the difference between 
		return future.Sub(now)
	}
}

func (route *Route) NumChange() int {
	if route.IsPartial() { return 0 }
	return len(route.ConnectionPartList) - 1
}

func (route *Route) TotalWaitingTime() time.Duration {
	if route.IsPartial() {return 0}
	start,end := time.Now(),time.Now()
	for _,subRoute := range route.ConnectionPartList {
		end = end.Add(subRoute.GetDuration())
	}
	return end.Sub(start)
}
