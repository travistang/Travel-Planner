package api

import (
	"encoding/json"
	"net/http"
	"github.com/gorilla/mux"
	"log"
	"fmt"

	"mvg"
)

type Endpoint struct {
	Endpoint,Method string
	Handler func(http.ResponseWriter,*http.Request)
}
type Response struct {
	ok int
	response interface{}
}
// handlers
func departure_handler(w http.ResponseWriter,request *http.Request) {
	params := mux.Vars(request)
	if station,ok := params["station"]; ok {
		stationObj,err := mvg.FindStationByName(station)
		if err != nil || len(stationObj) == 0 {
			errorResponse := Response {ok: 0,response: fmt.Sprintf("No such station: %s",station)}
			json.NewEncoder(w).Encode(errorResponse)
			return
		}
		// obtain departures
		departures,err := mvg.DepartureFrom(stationObj[0])
		if err != nil {
			errorResponse := Response {ok: 0,response: fmt.Sprintf("%+v",err)}
			json.NewEncoder(w).Encode(errorResponse)
			return
		}
		// return departures
		json.NewEncoder(w).Encode(departures)
	}
	errorResponse := Response {
		ok: 0,
		response: "Missing parameter `station`"}
	json.NewEncoder(w).Encode(errorResponse)
}
func TestAPI() {
	endpoints := []Endpoint{
		Endpoint{
			// This is a test...
			Endpoint : "/mvg/departure/{station}",
			Method : "GET",
			Handler: departure_handler}}
	RunMVGApi(":12333",endpoints)
}

func RunMVGApi(port string,endpoints []Endpoint) {
	router := mux.NewRouter()
	for _,endpoint := range endpoints {
		router.HandleFunc(endpoint.Endpoint,endpoint.Handler).Methods(endpoint.Method)
	}

	log.Fatal(http.ListenAndServe(port, router))
}
